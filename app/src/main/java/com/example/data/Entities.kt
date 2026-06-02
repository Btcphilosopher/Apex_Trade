package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_assets")
data class WalletAsset(
    @PrimaryKey val symbol: String,
    val balance: Double,
    val locked: Double
)

@Entity(tableName = "trade_orders")
data class TradeOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pair: String,
    val type: String, // LIMIT, MARKET, STOP_LOSS
    val side: String, // BUY, SELL
    val price: Double,
    val amount: Double,
    val filledAmount: Double,
    val status: String, // PENDING, FILLED, CANCELLED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pair: String,
    val targetPrice: Double,
    val isAbove: Boolean,
    var isActive: Boolean = true
)

@Entity(tableName = "transaction_history")
data class TransactionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String, // DEPOSIT, WITHDRAW, TRANSFER
    val asset: String,
    val amount: Double,
    val address: String,
    val txHash: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // SUCCESS, PENDING, FAILED
)
