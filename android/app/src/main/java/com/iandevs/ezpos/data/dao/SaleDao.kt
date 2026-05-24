package com.iandevs.ezpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.iandevs.ezpos.data.entity.Sale
import com.iandevs.ezpos.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow

data class DailySalesSummary(
    val date: String,
    val revenue: Double,
    val count: Int
)

data class TopProduct(
    val productName: String,
    val totalQuantity: Double,
    val totalRevenue: Double
)

@Dao
interface SaleDao {

    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("SELECT * FROM sales ORDER BY created_at DESC LIMIT 500")
    fun getAllSales(): Flow<List<Sale>>

    @Query("""
        SELECT * FROM sales 
        WHERE created_at >= :startDate AND created_at <= :endDate 
        ORDER BY created_at DESC
    """)
    fun getSalesByDateRange(startDate: String, endDate: String): Flow<List<Sale>>

    @Query("""
        SELECT * FROM sales 
        WHERE CAST(id AS TEXT) LIKE :query
        ORDER BY created_at DESC
    """)
    fun searchSalesById(query: String): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItem>

    // ── Analytics ─────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM sales")
    suspend fun getTotalRevenue(): Double

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getTotalSalesCount(): Int

    @Query("SELECT COALESCE(AVG(total_amount), 0) FROM sales")
    suspend fun getAverageTransactionValue(): Double

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE created_at >= :todayStart")
    suspend fun getTodayRevenue(todayStart: String): Double

    @Query("SELECT COUNT(*) FROM sales WHERE created_at >= :todayStart")
    suspend fun getTodaySalesCount(todayStart: String): Int

    @Query("""
        SELECT product_name AS productName, 
               SUM(quantity) AS totalQuantity,
               SUM(subtotal) AS totalRevenue
        FROM sale_items 
        GROUP BY product_name 
        ORDER BY totalRevenue DESC 
        LIMIT 10
    """)
    suspend fun getTopProducts(): List<TopProduct>

    @Query("""
        SELECT SUBSTR(created_at, 1, 10) AS date,
               SUM(total_amount) AS revenue,
               COUNT(*) AS count
        FROM sales 
        GROUP BY SUBSTR(created_at, 1, 10) 
        ORDER BY date DESC 
        LIMIT 30
    """)
    suspend fun getDailySalesSummary(): List<DailySalesSummary>

    @Transaction
    suspend fun insertSaleWithItems(sale: Sale, items: List<SaleItem>): Long {
        val saleId = insertSale(sale)
        val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
        insertSaleItems(itemsWithSaleId)
        return saleId
    }
}
