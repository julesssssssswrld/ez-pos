package com.example.butigersandbloompos.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.butigersandbloompos.theme.Warning
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}

@Composable
fun AnalyticsScreen(
    analytics: AnalyticsData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Cards
        item {
            Text("Sales Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Today's Revenue",
                    value = pesoFormat.format(analytics.todayRevenue),
                    subtitle = "${analytics.todaySalesCount} transactions",
                    icon = Icons.Default.Today,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Revenue",
                    value = pesoFormat.format(analytics.totalRevenue),
                    subtitle = "${analytics.totalSalesCount} all-time",
                    icon = Icons.Default.AttachMoney,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Avg. Transaction",
                    value = pesoFormat.format(analytics.averageTransaction),
                    subtitle = "per sale",
                    icon = Icons.Default.TrendingUp,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Products",
                    value = "${analytics.productCount}",
                    subtitle = "${analytics.outOfStockCount} out, ${analytics.lowStockCount} low",
                    icon = Icons.Default.Inventory2,
                    iconColor = Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Products
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Text("Top Selling Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (analytics.topProducts.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No sales data yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(analytics.topProducts) { product ->
                TopProductRow(product)
            }
        }

        // Daily Summary
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Text("Daily Sales (Last 30 Days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (analytics.dailySummary.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No sales data yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(analytics.dailySummary) { day ->
                DaySummaryRow(day)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = iconColor.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.dp), tint = iconColor)
                }
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TopProductRow(product: com.example.butigersandbloompos.data.dao.TopProduct) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${product.totalQuantity.toInt()} units sold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(pesoFormat.format(product.totalRevenue), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DaySummaryRow(day: com.example.butigersandbloompos.data.dao.DailySalesSummary) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(day.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("${day.count} transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(pesoFormat.format(day.revenue), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
