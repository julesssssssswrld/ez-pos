package com.example.butigersandbloompos.ui.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.butigersandbloompos.data.entity.InventoryLog
import com.example.butigersandbloompos.theme.Warning

@Composable
fun InventoryLogScreen(
    logs: List<InventoryLog>,
    filter: InventoryLogFilter,
    onFilterChange: (InventoryLogFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                InventoryLogFilter.ALL to "All",
                InventoryLogFilter.ADDED to "Added",
                InventoryLogFilter.EDITED to "Edited",
                InventoryLogFilter.SOLD to "Sold",
                InventoryLogFilter.RESTOCKED to "Restocked",
                InventoryLogFilter.DELETED to "Deleted"
            )
            items(filters) { (f, label) ->
                FilterChip(
                    selected = filter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(label, fontWeight = if (filter == f) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventNote, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))
                    Text("No inventory activity", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Stock changes will appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogCard(log)
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: InventoryLog) {
    val actionColor: Color = when (log.action) {
        "ADDED" -> MaterialTheme.colorScheme.primary
        "SOLD" -> MaterialTheme.colorScheme.tertiary
        "RESTOCKED" -> MaterialTheme.colorScheme.secondary
        "DELETED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val actionIcon = when (log.action) {
        "ADDED" -> Icons.Default.AddCircle
        "SOLD" -> Icons.Default.ShoppingCart
        "RESTOCKED" -> Icons.Default.Refresh
        "DELETED" -> Icons.Default.Delete
        else -> Icons.Default.Edit
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = actionColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    actionIcon, contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                    tint = actionColor
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(log.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Surface(color = actionColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Text(log.action, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = actionColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                if (log.note.isNotBlank()) {
                    Text(log.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val qtyStr = if (log.quantityChange >= 0) "+${log.quantityChange.toBigDecimal().stripTrailingZeros().toPlainString()}"
                        else log.quantityChange.toBigDecimal().stripTrailingZeros().toPlainString()
                    val qtyColor = if (log.quantityChange >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text("Qty: $qtyStr", style = MaterialTheme.typography.labelSmall, color = qtyColor, fontWeight = FontWeight.Bold)
                    Text("${log.stockBefore.toBigDecimal().stripTrailingZeros().toPlainString()} → ${log.stockAfter.toBigDecimal().stripTrailingZeros().toPlainString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(log.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}
