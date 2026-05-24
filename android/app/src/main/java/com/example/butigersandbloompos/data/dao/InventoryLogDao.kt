package com.example.butigersandbloompos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.butigersandbloompos.data.entity.InventoryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryLogDao {

    @Insert
    suspend fun insert(log: InventoryLog)

    @Query("SELECT * FROM inventory_log ORDER BY created_at DESC LIMIT 500")
    fun getAll(): Flow<List<InventoryLog>>

    @Query("SELECT * FROM inventory_log WHERE product_id = :productId ORDER BY created_at DESC")
    fun getByProductId(productId: Long): Flow<List<InventoryLog>>

    @Query("""
        SELECT * FROM inventory_log 
        WHERE action = :action 
        ORDER BY created_at DESC LIMIT 500
    """)
    fun getByAction(action: String): Flow<List<InventoryLog>>

    @Query("""
        SELECT * FROM inventory_log 
        WHERE created_at >= :startDate AND created_at <= :endDate 
        ORDER BY created_at DESC
    """)
    fun getByDateRange(startDate: String, endDate: String): Flow<List<InventoryLog>>
}
