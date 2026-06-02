package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_assets")
    fun getAllAssets(): Flow<List<WalletAsset>>

    @Query("SELECT * FROM wallet_assets WHERE symbol = :symbol LIMIT 1")
    suspend fun getAsset(symbol: String): WalletAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: WalletAsset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<WalletAsset>)

    @Update
    suspend fun updateAsset(asset: WalletAsset)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM trade_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<TradeOrder>>

    @Query("SELECT * FROM trade_orders WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingOrders(): Flow<List<TradeOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: TradeOrder): Long

    @Update
    suspend fun updateOrder(order: TradeOrder)

    @Query("UPDATE trade_orders SET status = 'CANCELLED' WHERE id = :orderId")
    suspend fun cancelOrder(orderId: Long)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1")
    suspend fun getActiveAlertsOnce(): List<PriceAlert>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1")
    fun getActiveAlertsFlow(): Flow<List<PriceAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert)

    @Update
    suspend fun updateAlert(alert: PriceAlert)

    @Query("DELETE FROM price_alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transaction_history ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionHistory)
}
