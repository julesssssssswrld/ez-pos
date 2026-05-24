package com.example.butigersandbloompos.ui.cashier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}

@Composable
fun PaymentDialog(
    cartTotal: Double,
    cartItemCount: Int,
    cashInput: String,
    canConfirm: Boolean,
    onKeyPress: (String) -> Unit,
    onQuickAmount: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cashValue = cashInput.toDoubleOrNull() ?: 0.0
    val change = maxOf(0.0, cashValue - cartTotal)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxHeight(0.95f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Complete Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FilledTonalIconButton(
                        onClick = onDismiss, modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) { Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp)) }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // ── Content: all weight-based, no scroll ──────
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Summary row (compact)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$cartItemCount item${if (cartItemCount != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pesoFormat.format(cartTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Cash tendered display
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (cashInput.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = if (cashInput.isNotEmpty()) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            if (cashInput.isNotEmpty()) "₱$cashInput" else "₱0.00",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Change row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pesoFormat.format(change), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Quick amounts
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(20, 50, 100, 200, 500, 1000).forEach { amount ->
                            OutlinedButton(
                                onClick = { onQuickAmount(amount) },
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("₱$amount", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }

                    // Numpad (takes remaining space)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf(".", "0", "backspace"),
                            listOf("clear", "exact", "confirm")
                        )
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.forEach { key ->
                                    NumpadButton(
                                        key = key,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        canConfirm = canConfirm,
                                        onClick = {
                                            if (key == "confirm") onConfirm()
                                            else onKeyPress(key)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadButton(
    key: String,
    modifier: Modifier = Modifier,
    canConfirm: Boolean = false,
    onClick: () -> Unit
) {
    val isConfirm = key == "confirm"
    val isAction = key in listOf("clear", "exact", "backspace")

    val containerColor = when {
        isConfirm && canConfirm -> MaterialTheme.colorScheme.primary
        isConfirm -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        isAction -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isConfirm && canConfirm -> MaterialTheme.colorScheme.onPrimary
        isConfirm -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        isAction -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        enabled = !isConfirm || canConfirm,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = if (!isConfirm && !isAction) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (key) {
                "backspace" -> Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = contentColor, modifier = Modifier.size(18.dp))
                "confirm" -> Icon(Icons.Default.Check, contentDescription = "Confirm", tint = contentColor, modifier = Modifier.size(22.dp))
                "clear" -> Text("C", color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                "exact" -> Text("Exact", color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                else -> Text(key, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
