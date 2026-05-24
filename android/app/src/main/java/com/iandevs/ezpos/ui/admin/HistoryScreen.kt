package com.iandevs.ezpos.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iandevs.ezpos.data.entity.Sale
import com.iandevs.ezpos.data.entity.SaleItem
import com.iandevs.ezpos.util.formatPeso

@Composable
fun HistoryScreen(
    sales: List<Sale>,
    expandedSaleId: Long?,
    expandedSaleItems: List<SaleItem>,
    expandedSale: Sale?,
    onToggleSale: (Long) -> Unit,
    historyFilter: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
    saleIdSearch: String,
    onSaleIdSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filter bar
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = saleIdSearch, onValueChange = onSaleIdSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by Sale ID...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (saleIdSearch.isNotEmpty()) IconButton(onClick = { onSaleIdSearchChange("") }) { Icon(Icons.Default.Close, "Clear") } },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    HistoryFilter.ALL to "All Time",
                    HistoryFilter.TODAY to "Today",
                    HistoryFilter.THIS_WEEK to "This Week",
                    HistoryFilter.THIS_MONTH to "This Month"
                )
                items(filters) { (filter, label) ->
                    FilterChip(
                        selected = historyFilter == filter && saleIdSearch.isBlank(),
                        onClick = { onFilterChange(filter) },
                        label = { Text(label, fontWeight = if (historyFilter == filter) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        enabled = saleIdSearch.isBlank()
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        if (sales.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))
                    Text("No transactions found", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sales will appear here as they are processed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 64.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sales, key = { it.id }) { sale ->
                    SaleCard(sale, isExpanded = expandedSaleId == sale.id, items = if (expandedSaleId == sale.id) expandedSaleItems else emptyList(), detailSale = if (expandedSaleId == sale.id) expandedSale else null, onToggle = { onToggleSale(sale.id) })
                }
            }
        }
    }
}

@Composable
private fun SaleCard(sale: Sale, isExpanded: Boolean, items: List<SaleItem>, detailSale: Sale?, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text("Sale #${sale.id}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Text("${sale.itemCount} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(sale.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Text(sale.totalAmount.formatPeso(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} ${item.productVariant}".trim() + " ×${item.quantity.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(item.subtotal.formatPeso(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (detailSale != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cash Tendered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(detailSale.cashTendered.formatPeso(), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Change", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(detailSale.changeAmount.formatPeso(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
