package com.iandevs.ezpos.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iandevs.ezpos.data.entity.Product

@Composable
fun AdminScreen(
    uiState: AdminUiState,
    onTabChange: (AdminTab) -> Unit,
    onSearchChange: (String) -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onReorderProducts: (List<Product>) -> Unit,
    onToggleSale: (Long) -> Unit,
    onHistoryFilterChange: (HistoryFilter) -> Unit,
    onHistorySaleIdSearch: (String) -> Unit,
    onInventoryLogFilterChange: (InventoryLogFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = AdminTab.entries.indexOf(uiState.activeTab),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            AdminTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.activeTab == tab,
                    onClick = { onTabChange(tab) },
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when (tab) {
                                    AdminTab.INVENTORY -> Icons.Default.Inventory2
                                    AdminTab.HISTORY -> Icons.Default.History
                                    AdminTab.ANALYTICS -> Icons.Default.Analytics
                                    AdminTab.STOCK_LOG -> Icons.Default.EventNote
                                },
                                contentDescription = null, modifier = Modifier.size(16.dp)
                            )
                            Text(
                                when (tab) {
                                    AdminTab.INVENTORY -> "Inventory"
                                    AdminTab.HISTORY -> "History"
                                    AdminTab.ANALYTICS -> "Analytics"
                                    AdminTab.STOCK_LOG -> "Stock Log"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
            }
        }

        when (uiState.activeTab) {
            AdminTab.INVENTORY -> InventoryScreen(
                products = uiState.products, searchQuery = uiState.searchQuery,
                onSearchChange = onSearchChange, onAddProduct = onAddProduct,
                onEditProduct = onEditProduct, onDeleteProduct = onDeleteProduct,
                onReorderProducts = onReorderProducts
            )
            AdminTab.HISTORY -> HistoryScreen(
                sales = uiState.sales, expandedSaleId = uiState.expandedSaleId,
                expandedSaleItems = uiState.expandedSaleItems, expandedSale = uiState.expandedSale,
                onToggleSale = onToggleSale, historyFilter = uiState.historyFilter,
                onFilterChange = onHistoryFilterChange, saleIdSearch = uiState.historySaleIdSearch,
                onSaleIdSearchChange = onHistorySaleIdSearch
            )
            AdminTab.ANALYTICS -> AnalyticsScreen(analytics = uiState.analytics)
            AdminTab.STOCK_LOG -> InventoryLogScreen(
                logs = uiState.inventoryLogs, filter = uiState.inventoryLogFilter,
                onFilterChange = onInventoryLogFilterChange
            )
        }
    }
}
