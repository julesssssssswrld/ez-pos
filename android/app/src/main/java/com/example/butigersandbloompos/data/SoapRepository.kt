package com.example.butigersandbloompos.data

import com.example.butigersandbloompos.data.dao.InventoryLogDao
import com.example.butigersandbloompos.data.dao.ProductDao
import com.example.butigersandbloompos.data.dao.SaleDao
import com.example.butigersandbloompos.data.dao.DailySalesSummary
import com.example.butigersandbloompos.data.dao.TopProduct
import com.example.butigersandbloompos.data.entity.InventoryLog
import com.example.butigersandbloompos.data.entity.Product
import com.example.butigersandbloompos.data.entity.Sale
import com.example.butigersandbloompos.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SoapRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val inventoryLogDao: InventoryLogDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun now(): String = dateFormat.format(Date())

    // ── Products ──────────────────────────────────────────────
    fun getAllProducts(): Flow<List<Product>> = productDao.getAll()

    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.search("%$query%")

    suspend fun getProductById(id: Long): Product? = productDao.getById(id)

    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getByBarcode(barcode)

    suspend fun addProduct(product: Product): Long {
        val timestamp = now()
        val sortOrder = productDao.getNextSortOrder()
        val id = productDao.insert(product.copy(createdAt = timestamp, sortOrder = sortOrder))
        inventoryLogDao.insert(
            InventoryLog(
                productId = id,
                productName = "${product.name} ${product.variant}".trim(),
                action = "ADDED",
                quantityChange = product.stock,
                stockBefore = 0.0,
                stockAfter = product.stock,
                note = "New product added",
                createdAt = timestamp
            )
        )
        return id
    }

    suspend fun updateProduct(oldProduct: Product?, product: Product) {
        productDao.update(product)
        val oldStock = oldProduct?.stock ?: 0.0
        if (oldStock != product.stock) {
            inventoryLogDao.insert(
                InventoryLog(
                    productId = product.id,
                    productName = "${product.name} ${product.variant}".trim(),
                    action = if (product.stock > oldStock) "RESTOCKED" else "EDITED",
                    quantityChange = product.stock - oldStock,
                    stockBefore = oldStock,
                    stockAfter = product.stock,
                    note = "Stock updated via edit",
                    createdAt = now()
                )
            )
        } else {
            inventoryLogDao.insert(
                InventoryLog(
                    productId = product.id,
                    productName = "${product.name} ${product.variant}".trim(),
                    action = "EDITED",
                    quantityChange = 0.0,
                    stockBefore = oldStock,
                    stockAfter = product.stock,
                    note = "Product details updated",
                    createdAt = now()
                )
            )
        }
    }

    suspend fun deleteProduct(id: Long) {
        val product = productDao.getById(id)
        productDao.deleteById(id)
        if (product != null) {
            inventoryLogDao.insert(
                InventoryLog(
                    productId = null,
                    productName = "${product.name} ${product.variant}".trim(),
                    action = "DELETED",
                    quantityChange = -product.stock,
                    stockBefore = product.stock,
                    stockAfter = 0.0,
                    note = "Product removed from inventory",
                    createdAt = now()
                )
            )
        }
    }

    suspend fun reorderProducts(products: List<Product>) {
        products.forEachIndexed { index, product ->
            productDao.updateSortOrder(product.id, index)
        }
    }

    // ── Sales ─────────────────────────────────────────────────
    fun getAllSales(): Flow<List<Sale>> = saleDao.getAllSales()

    fun getSalesByDateRange(startDate: String, endDate: String): Flow<List<Sale>> =
        saleDao.getSalesByDateRange(startDate, endDate)

    fun searchSalesById(query: String): Flow<List<Sale>> =
        saleDao.searchSalesById("%$query%")

    suspend fun getSaleById(id: Long): Sale? = saleDao.getSaleById(id)

    suspend fun getSaleItems(saleId: Long): List<SaleItem> = saleDao.getSaleItems(saleId)

    /**
     * Process a complete sale: insert sale record + items + deduct stock + log inventory.
     * Returns the new sale ID.
     */
    suspend fun processSale(
        items: List<Pair<Product, Double>>,
        cashTendered: Double
    ): Long {
        val timestamp = now()
        var totalAmount = 0.0
        var totalItemCount = 0

        val saleItems = items.map { (product, qty) ->
            val subtotal = product.price * qty
            totalAmount += subtotal
            totalItemCount += qty.toInt()
            SaleItem(
                saleId = 0,
                productId = product.id,
                productName = product.name,
                productVariant = product.variant,
                quantity = qty,
                unitPrice = product.price,
                subtotal = subtotal
            )
        }

        val changeAmount = cashTendered - totalAmount
        val sale = Sale(
            totalAmount = totalAmount,
            cashTendered = cashTendered,
            changeAmount = changeAmount,
            itemCount = totalItemCount,
            createdAt = timestamp
        )

        val saleId = saleDao.insertSaleWithItems(sale, saleItems)

        // Deduct stock and log each item
        for ((product, qty) in items) {
            val stockBefore = product.stock
            productDao.deductStock(product.id, qty)
            inventoryLogDao.insert(
                InventoryLog(
                    productId = product.id,
                    productName = "${product.name} ${product.variant}".trim(),
                    action = "SOLD",
                    quantityChange = -qty,
                    stockBefore = stockBefore,
                    stockAfter = stockBefore - qty,
                    note = "Sale #$saleId",
                    createdAt = timestamp
                )
            )
        }

        return saleId
    }

    // ── Analytics ─────────────────────────────────────────────
    suspend fun getTotalRevenue(): Double = saleDao.getTotalRevenue()
    suspend fun getTotalSalesCount(): Int = saleDao.getTotalSalesCount()
    suspend fun getAverageTransactionValue(): Double = saleDao.getAverageTransactionValue()
    suspend fun getTodayRevenue(): Double {
        val todayStart = now().substring(0, 10) + " 00:00:00"
        return saleDao.getTodayRevenue(todayStart)
    }
    suspend fun getTodaySalesCount(): Int {
        val todayStart = now().substring(0, 10) + " 00:00:00"
        return saleDao.getTodaySalesCount(todayStart)
    }
    suspend fun getTopProducts(): List<TopProduct> = saleDao.getTopProducts()
    suspend fun getDailySalesSummary(): List<DailySalesSummary> = saleDao.getDailySalesSummary()
    suspend fun getProductCount(): Int = productDao.getProductCount()
    suspend fun getOutOfStockCount(): Int = productDao.getOutOfStockCount()
    suspend fun getLowStockCount(): Int = productDao.getLowStockCount()

    // ── Inventory Log ─────────────────────────────────────────
    fun getAllInventoryLogs(): Flow<List<InventoryLog>> = inventoryLogDao.getAll()
    fun getInventoryLogsByAction(action: String): Flow<List<InventoryLog>> = inventoryLogDao.getByAction(action)
}
