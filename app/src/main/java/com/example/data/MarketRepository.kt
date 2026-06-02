package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.math.sin
import kotlin.random.Random

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class OrderBookEntry(
    val price: Double,
    val amount: Double,
    val total: Double
)

data class OrderBook(
    val bids: List<OrderBookEntry>, // Green (buy depth)
    val asks: List<OrderBookEntry>  // Red (sell depth)
)

class MarketRepository(
    context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    val walletDao = db.walletDao()
    val orderDao = db.orderDao()
    val alertDao = db.alertDao()
    val transactionDao = db.transactionDao()

    // Listed supported pairs
    val supportedPairs = listOf("BTC/USDT", "ETH/USDT", "SOL/USDT", "XRP/USDT")
    
    // Initial Base Prices
    private val basePrices = mutableMapOf(
        "BTC/USDT" to 68420.0,
        "ETH/USDT" to 3452.5,
        "SOL/USDT" to 145.2,
        "XRP/USDT" to 0.5842
    )

    // Live Prices State Flow
    private val _livePrices = MutableStateFlow<Map<String, Double>>(basePrices.toMap())
    val livePrices: StateFlow<Map<String, Double>> = _livePrices.asStateFlow()

    // Live Candlestick histories store by pair and timeframe
    private val candleHistories = mutableMapOf<String, MutableMap<String, MutableList<Candle>>>()

    private val _activePair = MutableStateFlow("BTC/USDT")
    val activePair: StateFlow<String> = _activePair.asStateFlow()

    private val _activeTimeframe = MutableStateFlow("15m")
    val activeTimeframe: StateFlow<String> = _activeTimeframe.asStateFlow()

    // Triggered alerts stream for displaying HUD screen notifications
    private val _alertNotificationFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val alertNotificationFlow: SharedFlow<String> = _alertNotificationFlow.asSharedFlow()

    init {
        // Initialize default wallet seeding
        seedWalletIfNull()
        
        // Generate historical candles
        initializeCandleHistory()

        // Start Price Ticker & Matching Engine background loop (WebSocket emulation)
        startMarketSimulation()
    }

    fun selectPair(pair: String) {
        if (supportedPairs.contains(pair)) {
            _activePair.value = pair
        }
    }

    fun selectTimeframe(tf: String) {
        _activeTimeframe.value = tf
    }

    private fun seedWalletIfNull() {
        scope.launch {
            val count = walletDao.getAllAssets().firstOrNull()?.size ?: 0
            if (count == 0) {
                walletDao.insertAssets(
                    listOf(
                        WalletAsset("BTC", 0.8421, 0.0),
                        WalletAsset("ETH", 12.55, 0.0),
                        WalletAsset("USDT", 5000.0, 0.0),
                        WalletAsset("SOL", 45.0, 0.0),
                        WalletAsset("XRP", 1200.0, 0.0)
                    )
                )
                // Seed some trade history
                transactionDao.insertTransaction(
                    TransactionHistory(type = "DEPOSIT", asset = "USDT", amount = 5000.0, address = "0xBankTransfer", txHash = "0xSeededTxHash_USDT", status = "SUCCESS")
                )
                transactionDao.insertTransaction(
                    TransactionHistory(type = "DEPOSIT", asset = "BTC", amount = 0.8421, address = "bc1qSeededTxHash_BTC", txHash = "0xSeededTxHash_BTC", status = "SUCCESS")
                )
                transactionDao.insertTransaction(
                    TransactionHistory(type = "DEPOSIT", asset = "ETH", amount = 12.55, address = "0xSeededTxHash_ETH", txHash = "0xSeededTxHash_ETH", status = "SUCCESS")
                )
            }
        }
    }

    private fun initializeCandleHistory() {
        val now = System.currentTimeMillis()
        supportedPairs.forEach { pair ->
            val pairMap = candleHistories.getOrPut(pair) { mutableMapOf() }
            listOf("1m", "5m", "15m", "1h", "1d").forEach { tf ->
                val list = mutableListOf<Candle>()
                val timeframeMs = getTimeframeMs(tf)
                val basePr = basePrices[pair] ?: 100.0
                
                // Seed 50 historical candles with a beautiful sine wave + random noise to reflect exchange trends
                for (i in 49 downTo 0) {
                    val time = now - (i * timeframeMs)
                    val angle = i * 0.15
                    val drift = sin(angle) * (basePr * 0.015)
                    val noise = (Random.nextDouble() - 0.5) * (basePr * 0.008)
                    val cOpen = basePr + drift + noise
                    val cClose = cOpen + (Random.nextDouble() - 0.48) * (basePr * 0.005)
                    val cHigh = maxOf(cOpen, cClose) + Random.nextDouble() * (basePr * 0.004)
                    val cLow = minOf(cOpen, cClose) - Random.nextDouble() * (basePr * 0.004)
                    val volume = Random.nextDouble() * 250.0 + 50.0

                    list.add(Candle(time, cOpen, cHigh, cLow, cClose, volume))
                }
                pairMap[tf] = list
            }
        }
    }

    private fun getTimeframeMs(tf: String): Long {
        return when (tf) {
            "1m" -> 60_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "1h" -> 3_600_000L
            "1d" -> 86_400_000L
            else -> 900_000L
        }
    }

    // Expose flows for current active candle list
    val activeCandlesFlow: Flow<List<Candle>> = combine(_activePair, _activeTimeframe, _livePrices) { pair, tf, _ ->
        candleHistories[pair]?.get(tf) ?: emptyList()
    }.flowOn(Dispatchers.Default)

    // High fidelity order book stream
    val activeOrderBookFlow: Flow<OrderBook> = combine(_activePair, _livePrices) { pair, prices ->
        val midPrice = prices[pair] ?: 100.0
        val tickSize = getTickSize(pair)
        
        // Generate Sell Orders (Asks) - above current price
        val asks = mutableListOf<OrderBookEntry>()
        var askCumulative = 0.0
        for (i in 1..8) {
            val p = midPrice + (i * tickSize) + (Random.nextDouble() * 0.1 * tickSize)
            val amt = (Random.nextDouble() * 2.5 + 0.1) * getMultiplier(pair)
            askCumulative += amt
            asks.add(OrderBookEntry(p, amt, askCumulative))
        }

        // Generate Buy Orders (Bids) - below current price
        val bids = mutableListOf<OrderBookEntry>()
        var bidCumulative = 0.0
        for (i in 1..8) {
            val p = midPrice - (i * tickSize) - (Random.nextDouble() * 0.1 * tickSize)
            val amt = (Random.nextDouble() * 2.5 + 0.1) * getMultiplier(pair)
            bidCumulative += amt
            bids.add(OrderBookEntry(p, amt, bidCumulative))
        }

        // Return bids and sorted asks (Asks from high to low or reverse for depth, standard asks are listed low price to high price)
        OrderBook(bids = bids, asks = asks.sortedBy { it.price })
    }.flowOn(Dispatchers.Default)

    private fun getTickSize(pair: String): Double {
        return when (pair) {
            "BTC/USDT" -> 5.0
            "ETH/USDT" -> 0.5
            "SOL/USDT" -> 0.05
            "XRP/USDT" -> 0.0001
            else -> 1.0
        }
    }

    private fun getMultiplier(pair: String): Double {
        return when (pair) {
            "BTC/USDT" -> 0.15
            "ETH/USDT" -> 1.2
            "SOL/USDT" -> 10.0
            "XRP/USDT" -> 500.0
            else -> 1.0
        }
    }

    private fun startMarketSimulation() {
        scope.launch {
            while (isActive) {
                delay(250) // High frequency ticks <250ms feel entirely real
                val currMap = _livePrices.value.toMutableMap()
                for (pair in supportedPairs) {
                    val oldPrice = currMap[pair] ?: 100.0
                    val volatility = getVolatilityFactor(pair)
                    // High-frequency random walk drift
                    val changePercent = (Random.nextDouble() - 0.495) * volatility 
                    val newPrice = oldPrice * (1 + changePercent)
                    currMap[pair] = newPrice

                    // Update live candlestick values
                    updateCandleWithTick(pair, newPrice)
                }
                _livePrices.value = currMap

                // Match order book to execute limit and stop-loss orders in real time
                checkAndMatchOrders(currMap)

                // Check and trigger custom alerts set by users
                checkPriceAlerts(currMap)
            }
        }
    }

    private fun getVolatilityFactor(pair: String): Double {
        return when (pair) {
            "BTC/USDT" -> 0.0004
            "ETH/USDT" -> 0.0006
            "SOL/USDT" -> 0.0012
            "XRP/USDT" -> 0.0015
            else -> 0.0005
        }
    }

    private fun updateCandleWithTick(pair: String, price: Double) {
        val pairMap = candleHistories[pair] ?: return
        pairMap.forEach { (tf, candles) ->
            if (candles.isNotEmpty()) {
                val lastCandle = candles.last()
                val now = System.currentTimeMillis()
                val timeframeMs = getTimeframeMs(tf)
                
                if (now - lastCandle.timestamp < timeframeMs) {
                    // Update current candle
                    val updated = lastCandle.copy(
                        high = maxOf(lastCandle.high, price),
                        low = minOf(lastCandle.low, price),
                        close = price,
                        volume = lastCandle.volume + Random.nextDouble() * 0.5
                    )
                    candles[candles.size - 1] = updated
                } else {
                    // Start new candle
                    val newCandle = Candle(
                        timestamp = lastCandle.timestamp + timeframeMs,
                        open = lastCandle.close,
                        high = maxOf(lastCandle.close, price),
                        low = minOf(lastCandle.close, price),
                        close = price,
                        volume = Random.nextDouble() * 2.0
                    )
                    candles.add(newCandle)
                    // limit to 60 candles
                    if (candles.size > 60) {
                        candles.removeAt(0)
                    }
                }
            }
        }
    }

    private suspend fun checkAndMatchOrders(prices: Map<String, Double>) {
        val pendingList = orderDao.getPendingOrders().firstOrNull() ?: return
        for (order in pendingList) {
            val currPrice = prices[order.pair] ?: continue
            val isBuy = order.side == "BUY"
            
            var shouldFill = false
            when (order.type) {
                "LIMIT" -> {
                    if (isBuy && currPrice <= order.price) {
                        shouldFill = true
                    } else if (!isBuy && currPrice >= order.price) {
                        shouldFill = true
                    }
                }
                "STOP_LOSS" -> {
                    // Stop loss order triggers when price crashes beyond a limit (sell stop or buy limit/stop etc.)
                    if (isBuy && currPrice >= order.price) {
                        shouldFill = true
                    } else if (!isBuy && currPrice <= order.price) {
                        shouldFill = true
                    }
                }
                "MARKET" -> {
                    shouldFill = true
                }
            }

            if (shouldFill) {
                // Execute trade settlement inside DB
                try {
                    val pairTokens = order.pair.split("/")
                    val targetAsset = pairTokens[0] // e.g. BTC
                    val baseAsset = pairTokens[1]   // e.g. USDT

                    val pricePaid = if (order.type == "LIMIT") order.price else currPrice
                    val totalQuoteValue = order.amount * pricePaid

                    if (isBuy) {
                        // User is buying BTC with USDT
                        // Verify and debit quote asset (USDT) from locked/active
                        val quoteRecord = walletDao.getAsset(baseAsset) ?: WalletAsset(baseAsset, 0.0, 0.0)
                        val targetRecord = walletDao.getAsset(targetAsset) ?: WalletAsset(targetAsset, 0.0, 0.0)

                        if (order.type == "LIMIT") {
                            // Locked fund exists in LOCKED column
                            val newLocked = maxOf(0.0, quoteRecord.locked - totalQuoteValue)
                            walletDao.updateAsset(quoteRecord.copy(locked = newLocked))
                        } else {
                            // Market order -> directly deduct balances
                            val newBalance = maxOf(0.0, quoteRecord.balance - totalQuoteValue)
                            walletDao.updateAsset(quoteRecord.copy(balance = newBalance))
                        }

                        // Add target asset to wallet balance
                        walletDao.updateAsset(targetRecord.copy(balance = targetRecord.balance + order.amount))

                    } else {
                        // User is selling BTC for USDT
                        val quoteRecord = walletDao.getAsset(baseAsset) ?: WalletAsset(baseAsset, 0.0, 0.0)
                        val targetRecord = walletDao.getAsset(targetAsset) ?: WalletAsset(targetAsset, 0.0, 0.0)

                        if (order.type == "LIMIT") {
                            val newLocked = maxOf(0.0, targetRecord.locked - order.amount)
                            walletDao.updateAsset(targetRecord.copy(locked = newLocked))
                        } else {
                            val newBalance = maxOf(0.0, targetRecord.balance - order.amount)
                            walletDao.updateAsset(targetRecord.copy(balance = newBalance))
                        }

                        // Add USDT to wallet balance
                        walletDao.updateAsset(quoteRecord.copy(balance = quoteRecord.balance + totalQuoteValue))
                    }

                    // Log Order Complete State
                    val filledOrder = order.copy(
                        status = "FILLED",
                        filledAmount = order.amount,
                        price = pricePaid,
                        timestamp = System.currentTimeMillis()
                    )
                    orderDao.updateOrder(filledOrder)

                    // Write Transaction Record
                    transactionDao.insertTransaction(
                        TransactionHistory(
                            type = if (isBuy) "BUY" else "SELL",
                            asset = targetAsset,
                            amount = order.amount,
                            address = "ApexMatchEngine",
                            txHash = "0x" + UUID.randomUUID().toString().replace("-", "").take(16),
                            status = "SUCCESS"
                        )
                    )

                    // Emit immediate UI feedback HUD message
                    _alertNotificationFlow.emit("Order Executed! Filled ${order.side} ${order.amount} $targetAsset @ $${String.format("%.2f", pricePaid)}")

                } catch (e: Exception) {
                    Log.e("MarketRepository", "Order Execution failure: ", e)
                }
            }
        }
    }

    private suspend fun checkPriceAlerts(prices: Map<String, Double>) {
        val activeAlerts = alertDao.getActiveAlertsOnce()
        for (alert in activeAlerts) {
            val price = prices[alert.pair] ?: continue
            val isTriggered = if (alert.isAbove) {
                price >= alert.targetPrice
            } else {
                price <= alert.targetPrice
            }

            if (isTriggered) {
                alert.isActive = false
                alertDao.updateAlert(alert)
                
                // Fire instant trigger alert notification UI flow
                val direction = if (alert.isAbove) "ascended above" else "crashed below"
                val text = "🚨 ALERT TRIGGERED: ${alert.pair} $direction $${String.format("%.2f", alert.targetPrice)}! Current price: $${String.format("%.2f", price)}"
                _alertNotificationFlow.emit(text)
            }
        }
    }

    // Direct interface for placing orders instantly
    suspend fun placeOrder(
        pair: String,
        type: String, // LIMIT, MARKET, STOP_LOSS
        side: String, // BUY, SELL
        price: Double,
        amount: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val pairTokens = pair.split("/")
        val targetAsset = pairTokens[0]
        val baseAsset = pairTokens[1]

        val currPrice = _livePrices.value[pair] ?: price
        val executionPrice = if (type == "MARKET") currPrice else price
        val quoteTotal = amount * executionPrice

        if (side == "BUY") {
            // Lock portfolio assets for BUY order before placing
            val baseWallet = walletDao.getAsset(baseAsset) ?: WalletAsset(baseAsset, 0.0, 0.0)
            if (baseWallet.balance < quoteTotal) {
                return@withContext false // INS_FUNDS
            }

            if (type == "LIMIT" || type == "STOP_LOSS") {
                // Lock quote funds (USDT)
                walletDao.updateAsset(
                    baseWallet.copy(
                        balance = baseWallet.balance - quoteTotal,
                        locked = baseWallet.locked + quoteTotal
                    )
                )
            }
        } else {
            // Lock base crypto (e.g. BTC)
            val baseWallet = walletDao.getAsset(targetAsset) ?: WalletAsset(targetAsset, 0.0, 0.0)
            if (baseWallet.balance < amount) {
                return@withContext false // INS_FUNDS
            }

            if (type == "LIMIT" || type == "STOP_LOSS") {
                // Lock base assets (BTC)
                walletDao.updateAsset(
                    baseWallet.copy(
                        balance = baseWallet.balance - amount,
                        locked = baseWallet.locked + amount
                    )
                )
            }
        }

        // Insert database record
        val order = TradeOrder(
            pair = pair,
            type = type,
            side = side,
            price = executionPrice,
            amount = amount,
            filledAmount = 0.0,
            status = if (type == "MARKET") "FILLED" else "PENDING"
        )
        val orderId = orderDao.insertOrder(order)

        if (type == "MARKET") {
            // Immediate fill market orders
            val matchedOrder = order.copy(id = orderId)
            checkAndMatchOrders(mapOf(pair to currPrice))
        } else {
            _alertNotificationFlow.emit("Submitted ${side} Limit Order: ${amount} ${targetAsset} at $${String.format("%.2f", price)}")
        }

        return@withContext true
    }

    // Safe Deposit addresses
    fun getDepositAddress(asset: String): String {
        return when (asset) {
            "BTC" -> "bc1qlpwyv0g27npx49shsl7n7a6dfp2yuxsw4c9x86"
            "ETH" -> "0xdAC17F958D2ee523a2206206994597C13D831ec7"
            "SOL" -> "8x8BfBf9v7A37d8e2Fe9Lsc2v1hC2WqN1Z7fghJK"
            "XRP" -> "rE1puyZyxuAxe4cshsl7n7Z7a6dfp2yuxsw4c9"
            "USDT" -> "0xdAC17F958D2ee523a2206206994597C13D831ec7"
            else -> "0xAddressUnavailable"
        }
    }

    // Simulated Blockchain withdraw requests with real pending confirmation flows
    fun executeWithdraw(
        asset: String,
        amount: Double,
        destinationAddress: String,
        onStatusChange: (String) -> Unit
    ) {
        scope.launch {
            val walletRecord = walletDao.getAsset(asset)
            if (walletRecord == null || walletRecord.balance < amount) {
                onStatusChange("FAILED: Insufficient portfolio balances")
                return@launch
            }

            // Deduct from local balance
            walletDao.updateAsset(walletRecord.copy(balance = walletRecord.balance - amount))
            
            // Generate Transaction Record in DB as Pending
            val txHash = "0x" + UUID.randomUUID().toString().replace("-", "").take(32)
            val tx = TransactionHistory(
                type = "WITHDRAW",
                asset = asset,
                amount = amount,
                address = destinationAddress,
                txHash = txHash,
                status = "PENDING"
            )
            transactionDao.insertTransaction(tx)

            // Simulate block confirmation intervals
            onStatusChange("Withdraw Submitting... (Fee estimated)")
            delay(1500)
            onStatusChange("Blockchain Broadcasted: Hash short $txHash")
            delay(2000)
            onStatusChange("Confirmations status: 1/3")
            delay(2000)
            onStatusChange("Confirmations status: 2/3")
            delay(2000)
            onStatusChange("Confirmed on Ledger. Transaction Completed!")

            // Update Transaction to success
            val finishedTx = tx.copy(status = "SUCCESS")
            transactionDao.insertTransaction(finishedTx)
        }
    }

    // Simulated Blockchain deposits
    fun triggerSimulatedDeposit(asset: String, amount: Double) {
        scope.launch {
            val walletRecord = walletDao.getAsset(asset) ?: WalletAsset(asset, 0.0, 0.0)
            // Add balance
            walletDao.updateAsset(walletRecord.copy(balance = walletRecord.balance + amount))

            // Add history
            transactionDao.insertTransaction(
                TransactionHistory(
                    type = "DEPOSIT",
                    asset = asset,
                    amount = amount,
                    address = "BinanceWalletTransfer",
                    txHash = "0x" + UUID.randomUUID().toString().replace("-", "").take(32),
                    status = "SUCCESS"
                )
            )
            _alertNotificationFlow.emit("Blockchain Deposit Confirmed: Received $amount $asset!")
        }
    }
}
