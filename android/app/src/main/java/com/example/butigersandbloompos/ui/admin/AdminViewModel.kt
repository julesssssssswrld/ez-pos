package com.example.butigersandbloompos.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.butigersandbloompos.data.SoapRepository
import com.example.butigersandbloompos.data.dao.DailySalesSummary
import com.example.butigersandbloompos.data.dao.TopProduct
import com.example.butigersandbloompos.data.entity.InventoryLog
import com.example.butigersandbloompos.data.entity.Product
import com.example.butigersandbloompos.data.entity.Sale
import com.example.butigersandbloompos.data.entity.SaleItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProductFormState(
    val id: Long? = null,
    val name: String = "",
    val variant: String = "",
    val price: String = "",
    val stock: String = "",
    val minWarning: String = "",
    val barcode: String = ""
)

data class AnalyticsData(
    val totalRevenue: Double = 0.0,
    val totalSalesCount: Int = 0,
    val averageTransaction: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val todaySalesCount: Int = 0,
    val productCount: Int = 0,
    val outOfStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val topProducts: List<TopProduct> = emptyList(),
    val dailySummary: List<DailySalesSummary> = emptyList()
)

enum class HistoryFilter { ALL, TODAY, THIS_WEEK, THIS_MONTH }
enum class InventoryLogFilter { ALL, ADDED, EDITED, SOLD, RESTOCKED, DELETED }

data class AdminUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val sales: List<Sale> = emptyList(),
    val expandedSaleId: Long? = null,
    val expandedSaleItems: List<SaleItem> = emptyList(),
    val expandedSale: Sale? = null,
    val showProductForm: Boolean = false,
    val productForm: ProductFormState = ProductFormState(),
    val showDeleteConfirm: Boolean = false,
    val deleteProductId: Long? = null,
    val deleteProductName: String = "",
    val toastMessage: String? = null,
    val toastIsError: Boolean = false,
    val activeTab: AdminTab = AdminTab.INVENTORY,
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val historySaleIdSearch: String = "",
    val analytics: AnalyticsData = AnalyticsData(),
    val inventoryLogs: List<InventoryLog> = emptyList(),
    val inventoryLogFilter: InventoryLogFilter = InventoryLogFilter.ALL
)

enum class AdminTab { INVENTORY, HISTORY, ANALYTICS, STOCK_LOG }

class AdminViewModel(private val repository: SoapRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    init {
        // Observe products
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _searchQuery.debounce(200).collectLatest { query ->
                val flow = if (query.isBlank()) repository.getAllProducts()
                else repository.searchProducts(query)
                flow.collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            }
        }
        // Observe sales (all initially)
        loadSales()
        // Observe inventory logs
        loadInventoryLogs()
    }

    private fun loadSales() {
        viewModelScope.launch {
            val filter = _uiState.value.historyFilter
            val searchId = _uiState.value.historySaleIdSearch

            val flow = when {
                searchId.isNotBlank() -> repository.searchSalesById(searchId)
                filter == HistoryFilter.ALL -> repository.getAllSales()
                else -> {
                    val (start, end) = getDateRange(filter)
                    repository.getSalesByDateRange(start, end)
                }
            }
            flow.collect { sales ->
                _uiState.update { it.copy(sales = sales) }
            }
        }
    }

    private fun loadInventoryLogs() {
        viewModelScope.launch {
            val filter = _uiState.value.inventoryLogFilter
            val flow = if (filter == InventoryLogFilter.ALL) {
                repository.getAllInventoryLogs()
            } else {
                repository.getInventoryLogsByAction(filter.name)
            }
            flow.collect { logs ->
                _uiState.update { it.copy(inventoryLogs = logs) }
            }
        }
    }

    private fun getDateRange(filter: HistoryFilter): Pair<String, String> {
        val cal = Calendar.getInstance()
        val end = dateFormat.format(cal.time)
        when (filter) {
            HistoryFilter.TODAY -> cal.set(Calendar.HOUR_OF_DAY, 0).also { cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0) }
            HistoryFilter.THIS_WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
            HistoryFilter.THIS_MONTH -> cal.add(Calendar.MONTH, -1)
            else -> {}
        }
        return dateFormat.format(cal.time) to end
    }

    fun setActiveTab(tab: AdminTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == AdminTab.ANALYTICS) loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                val data = AnalyticsData(
                    totalRevenue = repository.getTotalRevenue(),
                    totalSalesCount = repository.getTotalSalesCount(),
                    averageTransaction = repository.getAverageTransactionValue(),
                    todayRevenue = repository.getTodayRevenue(),
                    todaySalesCount = repository.getTodaySalesCount(),
                    productCount = repository.getProductCount(),
                    outOfStockCount = repository.getOutOfStockCount(),
                    lowStockCount = repository.getLowStockCount(),
                    topProducts = repository.getTopProducts(),
                    dailySummary = repository.getDailySalesSummary()
                )
                _uiState.update { it.copy(analytics = data) }
            } catch (_: Exception) {}
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    // ── History Filters ───────────────────────────────────────
    fun setHistoryFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter, historySaleIdSearch = "") }
        loadSales()
    }

    fun setHistorySaleIdSearch(query: String) {
        _uiState.update { it.copy(historySaleIdSearch = query) }
        loadSales()
    }

    fun setInventoryLogFilter(filter: InventoryLogFilter) {
        _uiState.update { it.copy(inventoryLogFilter = filter) }
        loadInventoryLogs()
    }

    // ── Product Form ──────────────────────────────────────────
    fun openAddProductForm() {
        _uiState.update { it.copy(showProductForm = true, productForm = ProductFormState()) }
    }

    fun openEditProductForm(product: Product) {
        _uiState.update {
            it.copy(
                showProductForm = true,
                productForm = ProductFormState(
                    id = product.id,
                    name = product.name,
                    variant = product.variant,
                    price = if (product.price > 0) product.price.toBigDecimal().stripTrailingZeros().toPlainString() else "",
                    stock = if (product.stock > 0) product.stock.toBigDecimal().stripTrailingZeros().toPlainString() else "",
                    minWarning = if (product.minWarning > 0) product.minWarning.toBigDecimal().stripTrailingZeros().toPlainString() else "",
                    barcode = product.barcode
                )
            )
        }
    }

    fun closeProductForm() {
        _uiState.update { it.copy(showProductForm = false) }
    }

    fun updateProductForm(form: ProductFormState) {
        _uiState.update { it.copy(productForm = form) }
    }

    // made with <3 by @julesssssssswrld & @ItsYoyong
    fun saveProduct() {
        val form = _uiState.value.productForm
        val errors = buildList {
            if (form.name.isBlank()) add("Product name is required")
            if (form.variant.isBlank()) add("Variant is required")
            if (form.price.isBlank() || form.price.toDoubleOrNull() == null || form.price.toDoubleOrNull()!! <= 0) add("Price must be greater than 0")
            if (form.stock.isBlank() || form.stock.toDoubleOrNull() == null || form.stock.toDoubleOrNull()!! < 0) add("Stock must be 0 or more")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(toastMessage = errors.first(), toastIsError = true) }
            return
        }

        viewModelScope.launch {
            try {
                val product = Product(
                    id = form.id ?: 0,
                    name = form.name.trim(),
                    variant = form.variant.trim(),
                    price = form.price.toDoubleOrNull() ?: 0.0,
                    stock = form.stock.toDoubleOrNull() ?: 0.0,
                    minWarning = form.minWarning.toDoubleOrNull() ?: 0.0,
                    barcode = form.barcode.trim()
                )

                if (form.id != null) {
                    val oldProduct = repository.getProductById(form.id)
                    repository.updateProduct(oldProduct, product)
                    _uiState.update { it.copy(showProductForm = false, toastMessage = null) }
                } else {
                    repository.addProduct(product)
                    _uiState.update { it.copy(showProductForm = false, toastMessage = null) }
                }
                _searchQuery.value = _uiState.value.searchQuery
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = e.message ?: "Save failed", toastIsError = true) }
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────
    fun requestDeleteProduct(product: Product) {
        _uiState.update {
            it.copy(showDeleteConfirm = true, deleteProductId = product.id, deleteProductName = "${product.name} ${product.variant}".trim())
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false, deleteProductId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteProductId ?: return
        viewModelScope.launch {
            try {
                repository.deleteProduct(id)
                _uiState.update { it.copy(showDeleteConfirm = false, deleteProductId = null, toastMessage = null) }
                _searchQuery.value = _uiState.value.searchQuery
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = e.message ?: "Delete failed", toastIsError = true) }
            }
        }
    }

    // ── Reorder ───────────────────────────────────────────────
    fun reorderProducts(products: List<com.example.butigersandbloompos.data.entity.Product>) {
        viewModelScope.launch {
            repository.reorderProducts(products)
        }
    }

    // ── History ───────────────────────────────────────────────
    fun toggleSaleExpansion(saleId: Long) {
        viewModelScope.launch {
            if (_uiState.value.expandedSaleId == saleId) {
                _uiState.update { it.copy(expandedSaleId = null, expandedSaleItems = emptyList(), expandedSale = null) }
            } else {
                try {
                    val items = repository.getSaleItems(saleId)
                    val sale = repository.getSaleById(saleId)
                    _uiState.update { it.copy(expandedSaleId = saleId, expandedSaleItems = items, expandedSale = sale) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(toastMessage = "Failed to load sale details", toastIsError = true) }
                }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
