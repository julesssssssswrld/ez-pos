package com.example.butigersandbloompos.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.butigersandbloompos.data.entity.Product
import com.example.butigersandbloompos.theme.Warning
import java.text.NumberFormat
import java.util.Locale
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}

@Composable
fun InventoryScreen(
    products: List<Product>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onReorderProducts: (List<Product>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local snapshot list for immediate drag feedback
    val localProducts = remember(products) { products.toMutableStateList() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery, onValueChange = onSearchChange,
                modifier = Modifier.weight(1f).height(48.dp), placeholder = { Text("Search inventory...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(16.dp)) } },
                singleLine = true, shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onAddProduct, shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add", style = MaterialTheme.typography.bodySmall) }
        }

        if (localProducts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))
                    Text("No products found", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val lazyListState = rememberLazyListState()
            val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                localProducts.apply { add(to.index, removeAt(from.index)) }
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(localProducts, key = { it.id }) { product ->
                    ReorderableItem(reorderState, key = product.id) { isDragging ->
                        val elevation = if (isDragging) 6.dp else 1.dp
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (product.variant.isNotBlank()) {
                                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                                Text(product.variant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        val isOutOfStock = product.stock <= 0
                                        val isLowStock = !isOutOfStock && product.stock > 0 && product.stock <= product.minWarning
                                        Text(pesoFormat.format(product.price), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        val stockColor = when { isOutOfStock -> MaterialTheme.colorScheme.error; isLowStock -> Warning; else -> MaterialTheme.colorScheme.onSurfaceVariant }
                                        val stockLabel = when { isOutOfStock -> "Out of stock"; else -> "Stock: ${product.stock.toBigDecimal().stripTrailingZeros().toPlainString()}" }
                                        Text(stockLabel, style = MaterialTheme.typography.bodySmall, color = stockColor, fontWeight = if (isOutOfStock || isLowStock) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                    if (product.barcode.isNotBlank()) {
                                        Text("Barcode: ${product.barcode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }

                                // Stock status badge
                                val isOutOfStock = product.stock <= 0
                                val isLowStock = !isOutOfStock && product.stock > 0 && product.stock <= product.minWarning
                                if (isOutOfStock) {
                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text("OUT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                } else if (isLowStock) {
                                    Surface(color = Warning.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                                        Text("LOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Warning, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }

                                FilledTonalIconButton(onClick = { onEditProduct(product) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                }
                                FilledTonalIconButton(
                                    onClick = { onDeleteProduct(product) }, modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }

                                // Drag handle on the right with DragIndicator icon
                                Icon(
                                    Icons.Default.DragIndicator,
                                    contentDescription = "Drag to reorder",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .draggableHandle(
                                            onDragStopped = { onReorderProducts(localProducts.toList()) }
                                        ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
