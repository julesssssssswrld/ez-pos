package com.iandevs.ezpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iandevs.ezpos.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY sort_order ASC, name, variant")
    fun getAll(): Flow<List<Product>>

    @Query("""
        SELECT * FROM products 
        WHERE name LIKE :query OR variant LIKE :query OR barcode LIKE :query 
        ORDER BY sort_order ASC, name, variant
    """)
    fun search(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): Product?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM products")
    suspend fun getNextSortOrder(): Int

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE products SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :id")
    suspend fun deductStock(id: Long, quantity: Double)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("SELECT COUNT(*) FROM products WHERE stock <= 0")
    suspend fun getOutOfStockCount(): Int

    @Query("SELECT COUNT(*) FROM products WHERE stock > 0 AND stock <= min_warning")
    suspend fun getLowStockCount(): Int
}
