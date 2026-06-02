package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MarketRepository(application)

    // Flow states from repository
    val activePair = repository.activePair
    val activeTimeframe = repository.activeTimeframe
    val livePrices = repository.livePrices

    // Reactive database streams with safe stateIn
    val walletAssets: StateFlow<List<WalletAsset>> = repository.walletDao.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<TradeOrder>> = repository.orderDao.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAlerts: StateFlow<List<PriceAlert>> = repository.alertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionHistory: StateFlow<List<TransactionHistory>> = repository.transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCandles: StateFlow<List<Candle>> = repository.activeCandlesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeOrderBook: StateFlow<OrderBook> = repository.activeOrderBookFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderBook(emptyList(), emptyList()))

    // Biometric Security lock verification on app launching
    private val _isBiometricScanning = MutableStateFlow(false)
    val isBiometricScanning = _isBiometricScanning.asStateFlow()

    private val _isTerminalUnlocked = MutableStateFlow(false)
    val isTerminalUnlocked = _isTerminalUnlocked.asStateFlow()

    // Interactive trading panel states (Limit/Market/Stop Loss)
    private val _orderType = MutableStateFlow("LIMIT") // LIMIT, MARKET, STOP_LOSS
    val orderType = _orderType.asStateFlow()

    private val _orderSide = MutableStateFlow("BUY") // BUY, SELL
    val orderSide = _orderSide.asStateFlow()

    private val _uiLimitPriceInput = MutableStateFlow("")
    val uiLimitPriceInput = _uiLimitPriceInput.asStateFlow()

    private val _uiAmountInput = MutableStateFlow("")
    val uiAmountInput = _uiAmountInput.asStateFlow()

    private val _tradeStatusMessage = MutableStateFlow<String?>(null)
    val tradeStatusMessage = _tradeStatusMessage.asStateFlow()

    // Blockchain Deposit & Withdraw visual flows
    private val _withdrawStatus = MutableStateFlow<String?>(null)
    val withdrawStatus = _withdrawStatus.asStateFlow()

    // HUD banner notifications
    private val _hudNotification = MutableStateFlow<String?>(null)
    val hudNotification = _hudNotification.asStateFlow()

    init {
        // Automatically set initial mock price in terminal form
        viewModelScope.launch {
            activePair.collect { pair ->
                val basePr = livePrices.value[pair] ?: 100.0
                _uiLimitPriceInput.value = String.format("%.2f", basePr)
            }
        }

        // Listen for alert notification triggers in background
        viewModelScope.launch {
            repository.alertNotificationFlow.collect { alertText ->
                _hudNotification.value = alertText
            }
        }
    }

    fun setOrderType(type: String) {
        _orderType.value = type
    }

    fun setOrderSide(side: String) {
        _orderSide.value = side
    }

    fun updatePriceInput(price: String) {
        _uiLimitPriceInput.value = price
    }

    fun updateAmountInput(amt: String) {
        _uiAmountInput.value = amt
    }

    fun selectPair(pair: String) {
        repository.selectPair(pair)
    }

    fun selectTimeframe(tf: String) {
        repository.selectTimeframe(tf)
    }

    fun dismissHudNotification() {
        _hudNotification.value = null
    }

    // Authenticate Terminal via simulated Biometric finger-unlock
    fun triggerBiometricUnlock(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBiometricScanning.value = true
            kotlinx.coroutines.delay(1000) // Beautiful simulated laser scan delay
            _isBiometricScanning.value = false
            _isTerminalUnlocked.value = true
            onSuccess()
        }
    }

    fun lockTerminal() {
        _isTerminalUnlocked.value = false
    }

    // Placing Trades with validation and deduction engine
    fun submitTradeOrder(onExecutedAnimation: () -> Unit) {
        val pair = activePair.value
        val type = orderType.value
        val side = orderSide.value
        val price = _uiLimitPriceInput.value.toDoubleOrNull() ?: 0.0
        val amount = _uiAmountInput.value.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _tradeStatusMessage.value = "ERROR: Quantity must be greater than zero!"
            return
        }

        if (type != "MARKET" && price <= 0.0) {
            _tradeStatusMessage.value = "ERROR: Order price must be valid for Limit/Stop Orders!"
            return
        }

        viewModelScope.launch {
            _tradeStatusMessage.value = "Submitting Order onto Terminal Ledger..."
            val success = repository.placeOrder(pair, type, side, price, amount)
            if (success) {
                _tradeStatusMessage.value = "SUCCESS: Order dispatched."
                onExecutedAnimation() // Trigger visual confetti or screen-pulse ripple
                _uiAmountInput.value = ""
                // keep status active for 2s then disappear
                kotlinx.coroutines.delay(2000)
                _tradeStatusMessage.value = null
            } else {
                _tradeStatusMessage.value = "FAILED: Insufficient portfolio collateral balance!"
                kotlinx.coroutines.delay(2500)
                _tradeStatusMessage.value = null
            }
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            repository.orderDao.cancelOrder(orderId)
            // Refund locked assets back to active balance (in simple logic)
            // Real exchange database would reverse this, we can just update the wallet values
            val order = repository.orderDao.getAllOrders().firstOrNull()?.find { it.id == orderId } ?: return@launch
            val symbolTokens = order.pair.split("/")
            val isBuy = order.side == "BUY"
            
            if (isBuy) {
                val quoteAsset = symbolTokens[1] // USDT
                val wallet = repository.walletDao.getAsset(quoteAsset) ?: return@launch
                val amountRefund = order.amount * order.price
                repository.walletDao.updateAsset(
                    wallet.copy(
                        balance = wallet.balance + amountRefund,
                        locked = maxOf(0.0, wallet.locked - amountRefund)
                    )
                )
            } else {
                val targetAsset = symbolTokens[0] // e.g. BTC
                val wallet = repository.walletDao.getAsset(targetAsset) ?: return@launch
                repository.walletDao.updateAsset(
                    wallet.copy(
                        balance = wallet.balance + order.amount,
                        locked = maxOf(0.0, wallet.locked - order.amount)
                    )
                )
            }
            repository.orderDao.insertOrder(order.copy(status = "CANCELLED"))
        }
    }

    // Custom alerts system
    fun createPriceAlert(targetPrice: Double, isAbove: Boolean) {
        viewModelScope.launch {
            val alert = PriceAlert(
                pair = activePair.value,
                targetPrice = targetPrice,
                isAbove = isAbove,
                isActive = true
            )
            repository.alertDao.insertAlert(alert)
        }
    }

    fun removePriceAlert(alertId: Int) {
        viewModelScope.launch {
            repository.alertDao.deleteAlert(alertId)
        }
    }

    // Deposit Simulator
    fun simulateDeposit(asset: String, amount: Double) {
        repository.triggerSimulatedDeposit(asset, amount)
    }

    // Withdraw execution with blockchain feedback loop
    fun simulateWithdrawal(asset: String, amount: Double, address: String) {
        _withdrawStatus.value = "Initializing Blockchain Session..."
        repository.executeWithdraw(asset, amount, address) { status ->
            _withdrawStatus.value = status
            // Clear message if fully finished after delay
            if (status.contains("Completed") || status.contains("FAILED")) {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(4000)
                    _withdrawStatus.value = null
                }
            }
        }
    }

    fun getDepositAddress(asset: String): String {
        return repository.getDepositAddress(asset)
    }
}
