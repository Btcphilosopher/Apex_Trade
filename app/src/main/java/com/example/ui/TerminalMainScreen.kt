package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Candle
import com.example.data.OrderBookEntry
import com.example.data.WalletAsset
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TerminalMainScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val isUnlocked by viewModel.isTerminalUnlocked.collectAsStateWithLifecycle()
    val hudNotification by viewModel.hudNotification.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        if (!isUnlocked) {
            // Secure Lock Screen Gate
            BiometricLockScreen(viewModel)
        } else {
            // Main Dashboard Terminal UI
            TerminalDashboardContent(viewModel)

            // Animated HUD Ticker alerts
            hudNotification?.let { alertMessage ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 48.dp)
                        .align(Alignment.TopCenter)
                ) {
                    HudNotificationBanner(
                        message = alertMessage,
                        onDismiss = { viewModel.dismissHudNotification() }
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(viewModel: TerminalViewModel) {
    val isScanning by viewModel.isBiometricScanning.collectAsStateWithLifecycle()
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    
    val scannerOffset by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanning_line"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Info/Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.QueryStats,
                contentDescription = null,
                tint = TerminalPrimary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "APEX",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "TRADE",
                color = TerminalPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Institutional Trading Channel • Secure Custody v1.0",
            color = TerminalTertiary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Scanner Shell
        Box(
            modifier = Modifier
                .size(240.dp)
                .border(2.dp, if (isScanning) TerminalPrimary else TerminalBorder, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(TerminalSurface),
            contentAlignment = Alignment.Center
        ) {
            // Simulated scanned identity details
            Icon(
                imageVector = if (isScanning) Icons.Default.Face else Icons.Default.Fingerprint,
                contentDescription = "Scan",
                tint = if (isScanning) TerminalPrimary else TradingGrey,
                modifier = Modifier.size(110.dp)
            )

            // Scanning laser line overlay
            if (isScanning) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerY = size.height / 2 + scannerOffset
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                TerminalPrimary.copy(alpha = 0.4f),
                                TerminalPrimary
                            )
                        ),
                        topLeft = Offset(0f, centerY - 24f),
                        size = Size(size.width, 24f)
                    )
                    drawLine(
                        color = TerminalPrimary,
                        start = Offset(0f, centerY),
                        end = Offset(size.width, centerY),
                        strokeWidth = 3f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(56.dp))

        Text(
            text = if (isScanning) "SCANNING SECURE BIOMETRICS..." else "AUTHENTICATION REQUIRED",
            color = if (isScanning) TerminalPrimary else Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isScanning) "Decrypting keys from Android Keystore enclave" else "Authorize login approval with biometrics or PIN to access orders book",
            color = TerminalTertiary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { viewModel.triggerBiometricUnlock {} },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) TerminalBorder else TerminalPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isScanning
        ) {
            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SECURE LOG IN",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun HudNotificationBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TerminalPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = DarkAmber,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "dismiss",
                    tint = TradingGrey,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TerminalDashboardContent(viewModel: TerminalViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: MARKET, 1: TRADE, 2: WALLET, 3: ALERTS

    val systemUiController = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TerminalBackground,
        bottomBar = {
            NavigationBar(
                containerColor = TerminalSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                val tabs = listOf(
                    Triple("Market", Icons.Outlined.TrendingUp, Icons.Filled.TrendingUp),
                    Triple("Trade", Icons.Outlined.CandlestickChart, Icons.Filled.CandlestickChart),
                    Triple("Wallet", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
                    Triple("Alerts", Icons.Outlined.CircleNotifications, Icons.Filled.CircleNotifications)
                )

                tabs.forEachIndexed { index, (label, outlinedIcon, filledIcon) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = label
                            )
                        },
                        label = { 
                            Text(
                                text = label, 
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TerminalPrimary,
                            selectedTextColor = Color.White,
                            indicatorColor = TerminalPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = TradingGrey,
                            unselectedTextColor = TradingGrey
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> MarketScreen(viewModel)
                1 -> TradeScreen(viewModel)
                2 -> WalletScreen(viewModel)
                3 -> AlertsAndHistoryScreen(viewModel)
            }
        }
    }
}

// ==========================================
// COLUMN/GRID COMPONENT CODES FOR SCREENS
// ==========================================

@Composable
fun MarketScreen(viewModel: TerminalViewModel) {
    val livePrices by viewModel.livePrices.collectAsStateWithLifecycle()
    val activePair by viewModel.activePair.collectAsStateWithLifecycle()
    
    // Track previous prices to determine direction
    var previousPrices by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    LaunchedEffect(livePrices) {
        // Keep a copy of previous pricing models to compare with fresh ticker values
        if (previousPrices.isNotEmpty()) {
            // delay comparison
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Hero Header
        Text(
            text = "Markets Tickers",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "WebSocket Real-Time Institutional Feed",
            color = TerminalTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                MarketSectorHeader(title = "Core Cryptocurrencies Pairs")
            }

            items(livePrices.keys.toList()) { pair ->
                val price = livePrices[pair] ?: 0.0
                val prevPrice = previousPrices[pair] ?: price
                val isUp = price >= prevPrice
                
                // Track dynamic changes
                val directionAnimColor by animateColorAsState(
                    targetValue = if (price > prevPrice) TradingGreen.copy(alpha = 0.25f)
                    else if (price < prevPrice) TradingRed.copy(alpha = 0.25f)
                    else Color.Transparent,
                    animationSpec = tween(300),
                    finishedListener = { previousPrices = livePrices },
                    label = "directionColor"
                )

                // High fidelity ticker item card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectPair(pair) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activePair == pair) TerminalSurface else TerminalSurface.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (activePair == pair) TerminalPrimary else TerminalBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(color = directionAnimColor)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coin Icon Simulation
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TerminalBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.take(3),
                                color = TerminalPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pair,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Vol: 24h: " + String.format("%.2f", price * 0.02) + "M",
                                color = TradingGrey,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$" + formatPrice(price, pair),
                                color = if (isUp) TradingGreen else TradingRed,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isUp) "+1.92%" else "-2.48%",
                                color = if (isUp) TradingGreen else TradingRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                QuickSimulatorConsole(viewModel)
            }
        }
    }
}

@Composable
fun MarketSectorHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = TerminalPrimary,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun QuickSimulatorConsole(viewModel: TerminalViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TerminalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BLOCKCHAIN SEED & TESTING CONSOLE",
                fontSize = 12.sp,
                color = TerminalSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Simulate active blockchain deposits or test system triggers dynamically.",
                fontSize = 11.sp,
                color = TradingGrey,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.simulateDeposit("BTC", 0.1) },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+0.1 BTC", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.simulateDeposit("ETH", 1.5) },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1.5 ETH", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.simulateDeposit("USDT", 1000.0) },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1k USDT", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// ACTIVE TRADING ENGINE SCREEN UI
// ==========================================

@Composable
fun TradeScreen(viewModel: TerminalViewModel) {
    val activePair by viewModel.activePair.collectAsStateWithLifecycle()
    val activeTimeframe by viewModel.activeTimeframe.collectAsStateWithLifecycle()
    val livePrices by viewModel.livePrices.collectAsStateWithLifecycle()
    val activeCandles by viewModel.activeCandles.collectAsStateWithLifecycle()
    val activeOrderBook by viewModel.activeOrderBook.collectAsStateWithLifecycle()

    val currentPrice = livePrices[activePair] ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // TOP TICKER HEADER PANEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Pair Selector Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activePair,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "select",
                        tint = TerminalPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Vol: $${String.format("%.2f", currentPrice * 1842)} USD",
                    color = TradingGrey,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$" + formatPrice(currentPrice, activePair),
                    color = TradingGreen,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Index Spot • WebSocket Online",
                    color = TerminalPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // TIMEFRAME SELECTOR CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val intervals = listOf("1m", "5m", "15m", "1h", "1d")
            intervals.forEach { tf ->
                val isSelected = activeTimeframe == tf
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TerminalPrimary else Color.Transparent)
                        .clickable { viewModel.selectTimeframe(tf) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf,
                        color = if (isSelected) TerminalBackground else TradingGrey,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // INTERACTIVE CANDLESTICK CHART ENGINE
        InteractiveCandlestickChart(candles = activeCandles)

        Spacer(modifier = Modifier.height(12.dp))

        // SUB-ROUND: ORDERBOOK & PLACING PANEL SIDE-BY-SIDE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Order Book (45% Width)
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .background(TerminalSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, TerminalBorder, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "ORDER BOOK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Asks Red Stacks
                OrderBookStack(entries = activeOrderBook.asks.take(6).reversed(), isBid = false, basePair = activePair)

                // Mid Spread Indicator
                HorizontalDivider(color = TerminalBorder, modifier = Modifier.padding(vertical = 6.dp))
                Text(
                    text = "$" + formatPrice(currentPrice, activePair),
                    color = TradingGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = TerminalBorder, modifier = Modifier.padding(vertical = 6.dp))

                // Bids Green Stacks
                OrderBookStack(entries = activeOrderBook.bids.take(6), isBid = true, basePair = activePair)
            }

            // Quick Trade Panel (55% Width)
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .background(TerminalSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, TerminalBorder, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                TradingOrderPlacementForm(viewModel)
            }
        }
    }
}

@Composable
fun OrderBookStack(
    entries: List<OrderBookEntry>,
    isBid: Boolean,
    basePair: String
) {
    val barColor = if (isBid) TradingGreen.copy(alpha = 0.12f) else TradingRed.copy(alpha = 0.12f)
    val fontColor = if (isBid) TradingGreen else TradingRed

    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Price", color = TradingGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Qty", color = TradingGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        entries.forEach { entry ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .drawBehind {
                        // Drawing a full terminal horizontal depth-weighted percentage bar
                        val fillPct = (entry.total / (entries.maxOfOrNull { it.total } ?: 1.0)).toFloat()
                        drawRect(
                            color = barColor,
                            topLeft = Offset(if (isBid) size.width * (1f - fillPct) else 0f, 0f),
                            size = Size(size.width * fillPct, size.height)
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPrice(entry.price, basePair),
                        color = fontColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format("%.3f", entry.amount),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveCandlestickChart(candles: List<Candle>) {
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // Calculate visible touched candle
                        val sizeWidth = size.width.toFloat()
                        val slice = sizeWidth / 60
                        val rawIndex = (offset.x / slice).toInt()
                        if (rawIndex in 0 until candles.size) {
                            highlightedIndex = rawIndex
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { highlightedIndex = null },
                    onDragCancel = { highlightedIndex = null },
                    onDrag = { change, dragAmount ->
                        dragOffset += dragAmount.x
                        // Track touched index with crosshair
                        val relativeX = change.position.x
                        val slice = size.width.toFloat() / 60
                        val rawIndex = (relativeX / slice).toInt()
                        if (rawIndex in 0 until candles.size) {
                            highlightedIndex = rawIndex
                        }
                    }
                )
            }
    ) {
        if (candles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TerminalPrimary)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 20.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val rawCandles = candles.takeLast(60)
                if (rawCandles.isEmpty()) return@Canvas

                val maxPrice = rawCandles.maxOf { it.high }
                val minPrice = rawCandles.minOf { it.low }
                val priceRange = maxPrice - minPrice

                // Plot helper
                fun transformY(price: Double): Float {
                    return if (priceRange == 0.0) canvasHeight / 2f
                    else (canvasHeight - ((price - minPrice) / priceRange * canvasHeight)).toFloat()
                }

                // Draw Grid lines
                val gridStroke = Stroke(width = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                for (j in 1..4) {
                    val y = canvasHeight * j / 5f
                    drawLine(color = TerminalBorder, start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1f)
                }

                val candleCount = rawCandles.size
                val itemWidth = canvasWidth / candleCount
                val barGap = itemWidth * 0.15f

                // Draw candles
                rawCandles.forEachIndexed { index, candle ->
                    val x = index * itemWidth + barGap
                    val wickX = x + (itemWidth - barGap * 2) / 2f
                    val color = if (candle.close >= candle.open) TradingGreen else TradingRed

                    val openY = transformY(candle.open)
                    val closeY = transformY(candle.close)
                    val highY = transformY(candle.high)
                    val lowY = transformY(candle.low)

                    // Draw Wick line
                    drawLine(
                        color = color,
                        start = Offset(wickX, highY),
                        end = Offset(wickX, lowY),
                        strokeWidth = 2f
                    )

                    // Draw body rect
                    val bodyHeight = maxOf(2f, Math.abs(closeY - openY))
                    drawRect(
                        color = color,
                        topLeft = Offset(x, minOf(openY, closeY)),
                        size = Size(itemWidth - barGap * 2, bodyHeight)
                    )
                }

                // Crosshair touch tracking overlays
                highlightedIndex?.let { index ->
                    if (index in rawCandles.indices) {
                        val touchedCandle = rawCandles[index]
                        val cx = index * itemWidth + itemWidth / 2f
                        val cy = transformY(touchedCandle.close)

                        // Vertical guide path
                        drawLine(
                            color = TerminalSecondary.copy(alpha = 0.5f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, canvasHeight),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )

                        // Horizontal guide path
                        drawLine(
                            color = TerminalSecondary.copy(alpha = 0.5f),
                            start = Offset(0f, cy),
                            end = Offset(canvasWidth, cy),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )

                        // Draw bullet node
                        drawCircle(
                            color = TerminalSecondary,
                            radius = 6f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            // HUD overlay info displays
            val activeInspectCandle = highlightedIndex?.let { idx ->
                candles.takeLast(60).getOrNull(idx)
            } ?: candles.lastOrNull()

            activeInspectCandle?.let { candle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .background(TerminalBackground.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("O:${String.format("%.2f", candle.open)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("H:${String.format("%.2f", candle.high)}", color = TradingGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("L:${String.format("%.2f", candle.low)}", color = TradingRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("C:${String.format("%.2f", candle.close)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("V:${String.format("%.1f", candle.volume)}", color = TerminalSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun TradingOrderPlacementForm(viewModel: TerminalViewModel) {
    val orderType by viewModel.orderType.collectAsStateWithLifecycle()
    val orderSide by viewModel.orderSide.collectAsStateWithLifecycle()
    val priceInput by viewModel.uiLimitPriceInput.collectAsStateWithLifecycle()
    val amountInput by viewModel.uiAmountInput.collectAsStateWithLifecycle()
    val statusMsg by viewModel.tradeStatusMessage.collectAsStateWithLifecycle()
    val walletAssets by viewModel.walletAssets.collectAsStateWithLifecycle()
    val activePair by viewModel.activePair.collectAsStateWithLifecycle()

    val quoteSymbol = activePair.split("/").getOrNull(1) ?: "USDT"
    val baseSymbol = activePair.split("/").getOrNull(0) ?: "BTC"

    // Fetch corresponding balances
    val quoteWallet = walletAssets.find { it.symbol == quoteSymbol }
    val baseWallet = walletAssets.find { it.symbol == baseSymbol }

    var animatedConfirmTrigger by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Toggle Buttons (BUY vs SELL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(if (orderSide == "BUY") TradingGreen else TerminalBorder)
                    .clickable { viewModel.setOrderSide("BUY") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("BUY", fontWeight = FontWeight.Bold, color = if (orderSide == "BUY") TerminalBackground else Color.White, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(if (orderSide == "SELL") TradingRed else TerminalBorder)
                    .clickable { viewModel.setOrderSide("SELL") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SELL", fontWeight = FontWeight.Bold, color = if (orderSide == "SELL") Color.White else Color.White, fontSize = 12.sp)
            }
        }

        // Limit, Market, Stop Loss Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val forms = listOf("LIMIT", "MARKET", "STOP_LOSS")
            forms.forEach { f ->
                val active = orderType == f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) TerminalBorder else Color.Transparent)
                        .clickable { viewModel.setOrderType(f) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = f.replace("_", " "),
                        fontSize = 9.sp,
                        color = if (active) TerminalPrimary else TradingGrey,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Limit Price Input Form (Skip if MARKET)
        if (orderType != "MARKET") {
            OutlinedTextField(
                value = priceInput,
                onValueChange = { viewModel.updatePriceInput(it) },
                label = { Text("Price ($quoteSymbol)", fontSize = 11.sp) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerminalPrimary,
                    unfocusedBorderColor = TerminalBorder,
                    focusedLabelColor = TerminalPrimary,
                    unfocusedLabelColor = TradingGrey
                )
            )
        }

        // Quantity Amount Input Form
        OutlinedTextField(
            value = amountInput,
            onValueChange = { viewModel.updateAmountInput(it) },
            label = { Text("Amount ($baseSymbol)", fontSize = 11.sp) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TerminalPrimary,
                unfocusedBorderColor = TerminalBorder,
                focusedLabelColor = TerminalPrimary,
                unfocusedLabelColor = TradingGrey
            )
        )

        // SLIDER SET PERCENT VALUES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(25, 50, 75, 100).forEach { pct ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            val activeVal = if (orderSide == "BUY") quoteWallet?.balance ?: 0.0 else baseWallet?.balance ?: 0.0
                            val targetAmt = if (orderSide == "BUY") {
                                val limitP = priceInput.toDoubleOrNull() ?: 1.0
                                (activeVal * (pct.toDouble() / 100)) / (if (limitP > 0) limitP else 1.0)
                            } else {
                                activeVal * (pct.toDouble() / 100)
                            }
                            viewModel.updateAmountInput(String.format("%.4f", targetAmt))
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${pct}%", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Available Balance Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Avbl:", color = TradingGrey, fontSize = 11.sp)
            Text(
                text = if (orderSide == "BUY") {
                    "${String.format("%.2f", quoteWallet?.balance ?: 0.0)} $quoteSymbol"
                } else {
                    "${String.format("%.4f", baseWallet?.balance ?: 0.0)} $baseSymbol"
                },
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Trigger Submit execution button
        Button(
            onClick = {
                viewModel.submitTradeOrder {
                    animatedConfirmTrigger = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (orderSide == "BUY") TradingGreen else TradingRed,
                contentColor = if (orderSide == "BUY") Color.Black else Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "${orderSide} ${baseSymbol}".uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }

        // Execution Alert Messages
        statusMsg?.let { msg ->
            Text(
                text = msg,
                fontSize = 10.sp,
                color = if (msg.contains("SUCCESS") || msg.contains("Order")) TradingGreen else TradingRed,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // Confirmed visual anim confirmation tick feedback
        LaunchedEffect(animatedConfirmTrigger) {
            if (animatedConfirmTrigger) {
                delay(1200)
                animatedConfirmTrigger = false
            }
        }
    }
}

// ==========================================
// SECURE PORTFOLIO WALLET CUSTODY
// ==========================================

@Composable
fun WalletScreen(viewModel: TerminalViewModel) {
    val walletAssets by viewModel.walletAssets.collectAsStateWithLifecycle()
    val livePrices by viewModel.livePrices.collectAsStateWithLifecycle()
    val withdrawStatus by viewModel.withdrawStatus.collectAsStateWithLifecycle()

    // Aggregate portfolio value based on active prices in real-time
    val netWorthInUsdt = walletAssets.sumOf { asset ->
        val assetPrice = if (asset.symbol == "USDT") 1.0 else {
            // lookup ticker
            livePrices["${asset.symbol}/USDT"] ?: 0.0
        }
        (asset.balance + asset.locked) * assetPrice
    }

    var selectedWithdrawAsset by remember { mutableStateOf("BTC") }
    var withdrawAddressInput by remember { mutableStateOf("") }
    var withdrawAmountInput by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Portfolio Net Worth Card
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TerminalPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTIMATED TOTAL BALANCE",
                        color = TerminalTertiary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Secured",
                        tint = TerminalPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$" + String.format("%,.2f", netWorthInUsdt) + " USDT",
                    color = TerminalPrimary,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "≈ " + String.format("%.6f", netWorthInUsdt / (livePrices["BTC/USDT"] ?: 68000.0)) + " BTC",
                    color = TradingGrey,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // WALLET ASSETS GRID LIST
        Text(
            text = "DIGITAL ASSETS BALANCE",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                walletAssets.forEachIndexed { index, asset ->
                    val tokenPrice = if (asset.symbol == "USDT") 1.0 else {
                        livePrices["${asset.symbol}/USDT"] ?: 0.0
                    }
                    val assetUsdValue = (asset.balance + asset.locked) * tokenPrice

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerminalBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = asset.symbol.take(2),
                                color = TerminalPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = asset.symbol,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (asset.locked > 0) {
                                Text(
                                    text = "Locked in Orders: ${String.format("%.4f", asset.locked)}",
                                    color = DarkAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("%.4f", asset.balance),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$" + String.format("%.2f", assetUsdValue),
                                color = TradingGrey,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (index < walletAssets.size - 1) {
                        HorizontalDivider(color = TerminalBorder, thickness = 0.5.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CUSTODY DEPOSIT & SECURE WITHDRAW CAPABILITIES
        Text(
            text = "BLOCKCHAIN TRANSFERS GATEWAY",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Coin:", color = TradingGrey, fontSize = 12.sp)
                    val choices = listOf("BTC", "ETH", "USDT")
                    choices.forEach { symbol ->
                        val isSel = selectedWithdrawAsset == symbol
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) TerminalPrimary else TerminalBorder)
                                .clickable { selectedWithdrawAsset = symbol }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                color = if (isSel) TerminalBackground else Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Get Deposit Address
                val depositAddr = viewModel.getDepositAddress(selectedWithdrawAsset)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalBackground, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("SECURE DEPOSIT ADDRESS ($selectedWithdrawAsset)", fontSize = 10.sp, color = TerminalPrimary, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = depositAddr,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(depositAddr)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "copy", tint = TerminalPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fast Outbound Withdraw Form
                Text("INITIATE LEDGER WITHDRAWAL", fontSize = 10.sp, color = TerminalPrimary, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = withdrawAddressInput,
                    onValueChange = { withdrawAddressInput = it },
                    label = { Text("Destination Blockchain Address", fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalPrimary,
                        unfocusedBorderColor = TerminalBorder
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = withdrawAmountInput,
                    onValueChange = { withdrawAmountInput = it },
                    label = { Text("Amount to Send", fontSize = 11.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalPrimary,
                        unfocusedBorderColor = TerminalBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val amt = withdrawAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0 && withdrawAddressInput.isNotEmpty()) {
                            viewModel.simulateWithdrawal(selectedWithdrawAsset, amt, withdrawAddressInput)
                            withdrawAmountInput = ""
                            withdrawAddressInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXECUTE WITHDRAWAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                withdrawStatus?.let { status ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = TerminalPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = status, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PRICE ALERT SETS & TRANSACTION HISTORIES SCREEN
// ==========================================

@Composable
fun AlertsAndHistoryScreen(viewModel: TerminalViewModel) {
    val activePair by viewModel.activePair.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionHistory.collectAsStateWithLifecycle()
    val livePrices by viewModel.livePrices.collectAsStateWithLifecycle()

    val currentPrice = livePrices[activePair] ?: 0.0

    var alertTargetPriceInput by remember { mutableStateOf("") }
    var alertIsAbove by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ALERT SYSTEM CRADLE
        Text(
            text = "PRICE ALERT SYSTEM",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configure instant push signals when $activePair breaches targets.",
                    fontSize = 11.sp,
                    color = TradingGrey,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = alertTargetPriceInput,
                        onValueChange = { alertTargetPriceInput = it },
                        label = { Text("Target Price ($)", fontSize = 11.sp) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalPrimary,
                            unfocusedBorderColor = TerminalBorder
                        )
                    )

                    // Above vs Below Toggle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (alertIsAbove) "RISE ABOVE" else "FALL BELOW", fontSize = 9.sp, color = TerminalPrimary, fontFamily = FontFamily.Monospace)
                        Switch(
                            checked = alertIsAbove,
                            onCheckedChange = { alertIsAbove = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TradingGreen,
                                checkedTrackColor = TerminalBorder,
                                uncheckedThumbColor = TradingRed,
                                uncheckedTrackColor = TerminalBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val p = alertTargetPriceInput.toDoubleOrNull() ?: 0.0
                        if (p > 0.0) {
                            viewModel.createPriceAlert(p, alertIsAbove)
                            alertTargetPriceInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("ACTIVATE PRICE WATCH ALERT", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                }

                // Active alerts listings
                if (activeAlerts.isNotEmpty()) {
                    HorizontalDivider(color = TerminalBorder, modifier = Modifier.padding(vertical = 14.dp))
                    Text("ACTIVE MONITOR CHANNELS", fontSize = 10.sp, color = TerminalSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    activeAlerts.forEach { alert ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(TerminalBackground, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(alert.pair, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val direction = if (alert.isAbove) "Triggers above" else "Triggers below"
                                Text(
                                    text = "$direction $${String.format("%.2f", alert.targetPrice)}",
                                    color = if (alert.isActive) TerminalPrimary else TradingGrey,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removePriceAlert(alert.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "delete", tint = TradingRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // LEDGER TRANSACTION HISTORIES list
        Text(
            text = "MUTABLE BLOCKCHAIN LEDGER",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (transactions.isEmpty()) {
                    Text(
                        text = "No history records generated yet. Put through orders to construct entries.",
                        fontSize = 11.sp,
                        color = TradingGrey,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    )
                } else {
                    transactions.forEachIndexed { i, tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (tx.type) {
                                                "BUY" -> TradingGreen.copy(alpha = 0.15f)
                                                "SELL" -> TradingRed.copy(alpha = 0.15f)
                                                "DEPOSIT" -> DarkAccentBlue.copy(alpha = 0.15f)
                                                else -> DarkAmber.copy(alpha = 0.15f)
                                            }
                                        ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (tx.type) {
                                        "BUY" -> Icons.Default.Add
                                        "SELL" -> Icons.Default.Remove
                                        "DEPOSIT" -> Icons.Default.ArrowDownward
                                        else -> Icons.Default.ArrowUpward
                                    },
                                    contentDescription = null,
                                    tint = when (tx.type) {
                                        "BUY" -> TradingGreen
                                        "SELL" -> TradingRed
                                        "DEPOSIT" -> DarkAccentBlue
                                        else -> DarkAmber
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tx.type} ${tx.asset}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Hash: " + tx.txHash.take(12) + "...",
                                    color = TradingGrey,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%.4f", tx.amount) + " " + tx.asset,
                                    color = when (tx.type) {
                                        "BUY" -> TradingGreen
                                        "SELL" -> TradingRed
                                        else -> Color.White
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tx.status,
                                    color = if (tx.status == "SUCCESS") TradingGreen else TradingGrey,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (i < transactions.size - 1) {
                            HorizontalDivider(color = TerminalBorder, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UTILITY FUNCTIONS HELPER
// ==========================================

fun formatPrice(value: Double, symbol: String): String {
    return when {
        symbol.contains("XRP") -> String.format("%.4f", value)
        symbol.contains("SOL") -> String.format("%.2f", value)
        else -> String.format("%.2f", value)
    }
}
