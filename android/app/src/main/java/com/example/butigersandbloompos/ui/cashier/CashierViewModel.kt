package com.example.butigersandbloompos.ui.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.butigersandbloompos.data.SoapRepository
import com.example.butigersandbloompos.data.entity.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val quantity: Double
)

data class SaleCompletedInfo(
    val saleId: Long,
    val totalAmount: Double,
    val cashTendered: Double,
    val changeAmount: Double
)

data class CashierUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val cart: List<CartItem> = emptyList(),
    val showPaymentDialog: Boolean = false,
    val cashInput: String = "",
    val toastMessage: String? = null,
    val toastIsError: Boolean = false,
    val saleCompleted: SaleCompletedInfo? = null,
    val editingQuantityProductId: Long? = null
)

class CashierViewModel(private val repository: SoapRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CashierUiState())
    val uiState: StateFlow<CashierUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
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
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun addToCart(product: Product) {
        _uiState.update { state ->
            val existing = state.cart.find { it.product.id == product.id }
            val newCart = if (existing != null) {
                if (existing.quantity < product.stock) {
                    state.cart.map {
                        if (it.product.id == product.id) it.copy(quantity = it.quantity + 1)
                        else it
                    }
                } else {
                    return@update state.copy(
                        toastMessage = "Maximum stock reached for ${product.name}",
                        toastIsError = true
                    )
                }
            } else {
                if (product.stock <= 0) {
                    return@update state.copy(
                        toastMessage = "${product.name} is out of stock",
                        toastIsError = true
                    )
                }
                state.cart + CartItem(product, 1.0)
            }
            state.copy(
                cart = newCart,
                toastMessage = "${product.name} added",
                toastIsError = false
            )
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        _uiState.update { state ->
            val item = state.cart.find { it.product.id == productId } ?: return@update state
            val newQty = item.quantity + delta
            val newCart = when {
                newQty <= 0 -> state.cart.filter { it.product.id != productId }
                newQty > item.product.stock -> return@update state.copy(
                    toastMessage = "Maximum stock reached",
                    toastIsError = true
                )
                else -> state.cart.map {
                    if (it.product.id == productId) it.copy(quantity = newQty)
                    else it
                }
            }
            state.copy(cart = newCart)
        }
    }

    fun setCartItemQuantity(productId: Long, quantity: Double) {
        _uiState.update { state ->
            val item = state.cart.find { it.product.id == productId } ?: return@update state
            val clampedQty = quantity.coerceIn(0.0, item.product.stock)
            val newCart = if (clampedQty <= 0) {
                state.cart.filter { it.product.id != productId }
            } else {
                state.cart.map {
                    if (it.product.id == productId) it.copy(quantity = clampedQty)
                    else it
                }
            }
            state.copy(cart = newCart, editingQuantityProductId = null)
        }
    }

    fun startEditQuantity(productId: Long) {
        _uiState.update { it.copy(editingQuantityProductId = productId) }
    }

    fun cancelEditQuantity() {
        _uiState.update { it.copy(editingQuantityProductId = null) }
    }

    fun clearCart() {
        _uiState.update { it.copy(cart = emptyList(), toastMessage = "Cart cleared", toastIsError = false) }
    }

    fun getCartTotal(): Double {
        return _uiState.value.cart.sumOf { it.product.price * it.quantity }
    }

    fun getCartItemCount(): Int {
        return _uiState.value.cart.sumOf { it.quantity.toInt() }
    }

    // ── Payment ───────────────────────────────────────────────
    fun openPaymentDialog() {
        _uiState.update { it.copy(showPaymentDialog = true, cashInput = "", saleCompleted = null) }
    }

    fun closePaymentDialog() {
        _uiState.update { it.copy(showPaymentDialog = false, cashInput = "") }
    }

    fun onCashInputKey(key: String) {
        _uiState.update { state ->
            val currentInput = state.cashInput
            val newInput = when (key) {
                in "0".."9" -> if (currentInput.length < 10) currentInput + key else currentInput
                "." -> if (!currentInput.contains(".")) {
                    if (currentInput.isEmpty()) "0." else "$currentInput."
                } else currentInput
                "backspace" -> currentInput.dropLast(1)
                "clear" -> ""
                "exact" -> getCartTotal().toBigDecimal().stripTrailingZeros().toPlainString()
                else -> currentInput
            }
            state.copy(cashInput = newInput)
        }
    }

    fun onQuickAmount(amount: Int) {
        _uiState.update { state ->
            val current = state.cashInput.toDoubleOrNull() ?: 0.0
            val newAmount = (current + amount)
            state.copy(cashInput = newAmount.toBigDecimal().stripTrailingZeros().toPlainString())
        }
    }

    fun getCashValue(): Double {
        return _uiState.value.cashInput.toDoubleOrNull() ?: 0.0
    }

    fun getChange(): Double {
        return maxOf(0.0, getCashValue() - getCartTotal())
    }

    fun canConfirmPayment(): Boolean {
        return getCashValue() >= getCartTotal() && _uiState.value.cart.isNotEmpty()
    }

    fun processSale() {
        if (!canConfirmPayment()) return
        viewModelScope.launch {
            try {
                val items = _uiState.value.cart.map { it.product to it.quantity }
                val totalAmount = getCartTotal()
                val cashTendered = getCashValue()
                val changeAmount = getChange()
                val saleId = repository.processSale(items, cashTendered)
                _uiState.update {
                    it.copy(
                        showPaymentDialog = false,
                        cashInput = "",
                        saleCompleted = SaleCompletedInfo(
                            saleId = saleId,
                            totalAmount = totalAmount,
                            cashTendered = cashTendered,
                            changeAmount = changeAmount
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        toastMessage = e.message ?: "Sale failed",
                        toastIsError = true
                    )
                }
            }
        }
    }

    fun dismissSaleCompleted() {
        _uiState.update {
            it.copy(
                cart = emptyList(),
                saleCompleted = null,
                toastMessage = "Sale completed",
                toastIsError = false
            )
        }
        // Refresh products to get updated stock
        _searchQuery.value = _uiState.value.searchQuery
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                addToCart(product)
            } else {
                _uiState.update {
                    it.copy(
                        toastMessage = "No product found for barcode: $barcode",
                        toastIsError = true
                    )
                }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
