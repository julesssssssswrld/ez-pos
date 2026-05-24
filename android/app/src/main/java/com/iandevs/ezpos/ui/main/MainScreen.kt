package com.iandevs.ezpos.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iandevs.ezpos.SoapApp
import com.iandevs.ezpos.ui.admin.*
import com.iandevs.ezpos.ui.cashier.*

enum class AppMode { CASHIER, ADMIN }

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as SoapApp
    val repository = app.repository

    val cashierViewModel: CashierViewModel = viewModel { CashierViewModel(repository) }
    val adminViewModel: AdminViewModel = viewModel { AdminViewModel(repository) }

    val cashierState by cashierViewModel.uiState.collectAsStateWithLifecycle()
    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()

    var currentMode by remember { mutableStateOf(AppMode.CASHIER) }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Main content ───────────────────────────────────────
        AnimatedContent(
            targetState = currentMode,
            transitionSpec = {
                fadeIn() + slideInHorizontally { if (targetState == AppMode.ADMIN) it / 6 else -it / 6 } togetherWith
                fadeOut() + slideOutHorizontally { if (targetState == AppMode.ADMIN) -it / 6 else it / 6 }
            },
            label = "mode_switch",
            modifier = Modifier.fillMaxSize()
        ) { mode ->
            when (mode) {
                AppMode.CASHIER -> CashierScreen(
                    uiState = cashierState,
                    onSearchChange = cashierViewModel::onSearchQueryChange,
                    onProductClick = cashierViewModel::addToCart,
                    onCartQuantityChange = cashierViewModel::updateCartQuantity,
                    onSetCartItemQuantity = cashierViewModel::setCartItemQuantity,
                    onStartEditQuantity = cashierViewModel::startEditQuantity,
                    onCancelEditQuantity = cashierViewModel::cancelEditQuantity,
                    onClearCart = cashierViewModel::clearCart,
                    onFinalizeSale = cashierViewModel::openPaymentDialog,
                    onBarcodeScanRequest = {},
                    onBarcodeScanned = cashierViewModel::onBarcodeScanned
                )
                AppMode.ADMIN -> AdminScreen(
                    uiState = adminState,
                    onTabChange = adminViewModel::setActiveTab,
                    onSearchChange = adminViewModel::onSearchQueryChange,
                    onAddProduct = adminViewModel::openAddProductForm,
                    onEditProduct = adminViewModel::openEditProductForm,
                    onDeleteProduct = adminViewModel::requestDeleteProduct,
                    onReorderProducts = adminViewModel::reorderProducts,
                    onToggleSale = adminViewModel::toggleSaleExpansion,
                    onHistoryFilterChange = adminViewModel::setHistoryFilter,
                    onHistorySaleIdSearch = adminViewModel::setHistorySaleIdSearch,
                    onInventoryLogFilterChange = adminViewModel::setInventoryLogFilter
                )
            }
        }

        // ── Floating nav pill (lower-left) ────────────────────
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 6.dp
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    PillTab(
                        label = "Cashier",
                        icon = if (currentMode == AppMode.CASHIER) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                        selected = currentMode == AppMode.CASHIER,
                        onClick = { currentMode = AppMode.CASHIER }
                    )
                    PillTab(
                        label = "Admin",
                        icon = if (currentMode == AppMode.ADMIN) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                        selected = currentMode == AppMode.ADMIN,
                        onClick = { currentMode = AppMode.ADMIN }
                    )
                }
            }
        }
    }

    // ── Dialogs (outside box so they overlay everything) ───────
    if (cashierState.showPaymentDialog) {
        PaymentDialog(
            cartTotal = cashierViewModel.getCartTotal(),
            cartItemCount = cashierViewModel.getCartItemCount(),
            cashInput = cashierState.cashInput,
            canConfirm = cashierViewModel.canConfirmPayment(),
            onKeyPress = cashierViewModel::onCashInputKey,
            onQuickAmount = cashierViewModel::onQuickAmount,
            onConfirm = cashierViewModel::processSale,
            onDismiss = cashierViewModel::closePaymentDialog
        )
    }

    cashierState.saleCompleted?.let { info ->
        SaleCompletedDialog(info = info, onComplete = cashierViewModel::dismissSaleCompleted)
    }

    if (adminState.showProductForm) {
        ProductFormDialog(
            formState = adminState.productForm,
            onFormChange = adminViewModel::updateProductForm,
            onSave = adminViewModel::saveProduct,
            onDismiss = adminViewModel::closeProductForm
        )
    }

    if (adminState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = adminViewModel::cancelDelete,
            title = { Text("Delete Product") },
            text = { Text("Remove \"${adminState.deleteProductName}\" from inventory? This cannot be undone.") },
            confirmButton = {
                Button(onClick = adminViewModel::confirmDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = adminViewModel::cancelDelete) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PillTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(14.dp))
            AnimatedVisibility(visible = selected) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
