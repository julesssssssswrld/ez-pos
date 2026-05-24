package com.iandevs.ezpos.ui.cashier

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.iandevs.ezpos.data.entity.Product
import com.iandevs.ezpos.theme.Warning
import com.iandevs.ezpos.util.formatPeso
import com.iandevs.ezpos.util.formatQty
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    uiState: CashierUiState,
    onSearchChange: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onCartQuantityChange: (Long, Int) -> Unit,
    onSetCartItemQuantity: (Long, Double) -> Unit,
    onStartEditQuantity: (Long) -> Unit,
    onCancelEditQuantity: () -> Unit,
    onClearCart: () -> Unit,
    onFinalizeSale: () -> Unit,
    onBarcodeScanRequest: () -> Unit,
    onBarcodeScanned: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cartTotal = uiState.cart.sumOf { it.product.price * it.quantity }
    val cartCount = uiState.cart.sumOf { it.quantity.toInt() }
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    // Barcode scanner state
    var scannerEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }

    // Cart bottom sheet (phone only)
    var showCart by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isTablet) {
        // ── TABLET: side-by-side layout ───────────────────────
        Row(modifier = modifier.fillMaxSize()) {
            // Left: product area
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SearchBarRow(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchChange,
                    scannerEnabled = scannerEnabled,
                    onToggleScanner = {
                        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
                        else scannerEnabled = !scannerEnabled
                    },
                    hasCameraPermission = hasCameraPermission
                )

                // Inline scanner strip (tablet)
                AnimatedVisibility(visible = scannerEnabled && hasCameraPermission) {
                    InlineScannerStrip(
                        onBarcodeScanned = onBarcodeScanned,
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                ProductGrid(
                    products = uiState.products,
                    onProductClick = onProductClick,
                    modifier = Modifier.weight(1f),
                    bottomPadding = 64.dp
                )
            }

            // Right: persistent cart panel
            Surface(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                CartPanel(
                    cart = uiState.cart,
                    cartTotal = cartTotal,
                    editingQuantityProductId = uiState.editingQuantityProductId,
                    onIncrease = { onCartQuantityChange(it, 1) },
                    onDecrease = { onCartQuantityChange(it, -1) },
                    onStartEdit = onStartEditQuantity,
                    onSetQty = onSetCartItemQuantity,
                    onCancelEdit = onCancelEditQuantity,
                    onClear = onClearCart,
                    onFinalize = onFinalizeSale
                )
            }
        }
    } else {
        // ── PHONE: product grid + floating cart bar ────────────
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBarRow(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchChange,
                    scannerEnabled = scannerEnabled,
                    onToggleScanner = {
                        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
                        else scannerEnabled = !scannerEnabled
                    },
                    hasCameraPermission = hasCameraPermission
                )

                // Inline scanner strip (phone)
                AnimatedVisibility(visible = scannerEnabled && hasCameraPermission) {
                    InlineScannerStrip(
                        onBarcodeScanned = onBarcodeScanned,
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                ProductGrid(
                    products = uiState.products,
                    onProductClick = onProductClick,
                    modifier = Modifier.weight(1f),
                    bottomPadding = if (cartCount > 0) 80.dp else 64.dp
                )
            }

            // Floating cart bar
            AnimatedVisibility(
                visible = cartCount > 0,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 10.dp,
                    onClick = { showCart = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("$cartCount", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Text("View Cart", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(cartTotal.formatPeso(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        if (showCart) {
            ModalBottomSheet(onDismissRequest = { showCart = false }, sheetState = sheetState) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Current Sale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onClearCart) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                    }
                    HorizontalDivider()
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(uiState.cart, key = { it.product.id }) { cartItem ->
                            CartItemRow(cartItem, uiState.editingQuantityProductId == cartItem.product.id,
                                onIncrease = { onCartQuantityChange(cartItem.product.id, 1) },
                                onDecrease = { onCartQuantityChange(cartItem.product.id, -1) },
                                onQuantityTap = { onStartEditQuantity(cartItem.product.id) },
                                onQuantitySet = { onSetCartItemQuantity(cartItem.product.id, it) },
                                onCancelEdit = onCancelEditQuantity)
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(cartTotal.formatPeso(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { showCart = false; onFinalizeSale() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.CreditCard, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Proceed to Payment", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Shared sub-composables ─────────────────────────────────────────────────────

@Composable
private fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    scannerEnabled: Boolean,
    onToggleScanner: () -> Unit,
    hasCameraPermission: Boolean
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("Search products…", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                },
                singleLine = true, shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
            // Scanner toggle icon button
            FilledTonalIconButton(
                onClick = onToggleScanner,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (scannerEnabled && hasCameraPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    if (scannerEnabled && hasCameraPermission) Icons.Default.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                    contentDescription = "Toggle scanner",
                    modifier = Modifier.size(18.dp),
                    tint = if (scannerEnabled && hasCameraPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 12.dp
) {
    val config = LocalConfiguration.current
    val columns = when {
        config.screenWidthDp >= 840 -> 4
        config.screenWidthDp >= 600 -> 3
        else -> 2
    }

    Box(modifier = modifier) {
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Inventory2, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Text("No products found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onProductClick(product) })
                }
            }
        }
    }
}

@Composable
private fun CartPanel(
    cart: List<CartItem>,
    cartTotal: Double,
    editingQuantityProductId: Long?,
    onIncrease: (Long) -> Unit,
    onDecrease: (Long) -> Unit,
    onStartEdit: (Long) -> Unit,
    onSetQty: (Long, Double) -> Unit,
    onCancelEdit: () -> Unit,
    onClear: () -> Unit,
    onFinalize: () -> Unit
) {
    // Easter egg state
    var eggTaps by remember { mutableIntStateOf(0) }
    var eggLastTap by remember { mutableLongStateOf(0L) }
    var showEgg by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Current Sale", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (cart.isNotEmpty()) TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Clear", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        if (cart.isEmpty()) {
            Box(
                Modifier.weight(1f).fillMaxWidth().clickable {
                    val now = System.currentTimeMillis()
                    eggTaps = if (now - eggLastTap < 800) eggTaps + 1 else 1
                    eggLastTap = now
                    if (eggTaps >= 5) { showEgg = true; eggTaps = 0 }
                },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Text("Cart is empty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(cart, key = { it.product.id }) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        isEditingQuantity = editingQuantityProductId == cartItem.product.id,
                        onIncrease = { onIncrease(cartItem.product.id) },
                        onDecrease = { onDecrease(cartItem.product.id) },
                        onQuantityTap = { onStartEdit(cartItem.product.id) },
                        onQuantitySet = { onSetQty(cartItem.product.id, it) },
                        onCancelEdit = onCancelEdit
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(cartTotal.formatPeso(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = onFinalize, enabled = cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CreditCard, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Finalize Sale", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showEgg) {
        AlertDialog(
            onDismissRequest = { showEgg = false },
            title = {
                Text(
                    text = "made with <3",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "@julesssssssswrld",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "@ItsYoyong",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { showEgg = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = !isOutOfStock && product.minWarning > 0 && product.stock <= product.minWarning

    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isOutOfStock) 0.45f else 1f).clickable(enabled = !isOutOfStock, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (product.variant.isNotBlank()) Text(product.variant, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            val (stockLabel, stockColor) = when {
                isOutOfStock -> "Out of stock" to MaterialTheme.colorScheme.error
                isLowStock -> "Low: ${product.stock.formatQty()}" to Warning
                else -> "${product.stock.formatQty()} in stock" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(stockLabel, style = MaterialTheme.typography.labelSmall, color = stockColor, fontWeight = if (isOutOfStock || isLowStock) FontWeight.SemiBold else FontWeight.Normal)
            Spacer(Modifier.height(4.dp))
            Text(product.price.formatPeso(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    isEditingQuantity: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onQuantityTap: () -> Unit,
    onQuantitySet: (Double) -> Unit,
    onCancelEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${cartItem.product.name} ${cartItem.product.variant}".trim(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(cartItem.product.price.formatPeso(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(onClick = onDecrease, modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if (cartItem.quantity <= 1) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp), tint = if (cartItem.quantity <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }

            if (isEditingQuantity) {
                var text by remember { mutableStateOf(cartItem.quantity.toInt().toString()) }
                val fr = remember { FocusRequester() }
                LaunchedEffect(Unit) { fr.requestFocus() }
                OutlinedTextField(value = text, onValueChange = { v -> if (v.length <= 4) text = v.filter { it.isDigit() } },
                    modifier = Modifier.width(60.dp).focusRequester(fr),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onQuantitySet(text.toDoubleOrNull() ?: 0.0) }), shape = RoundedCornerShape(8.dp))
            } else {
                Surface(onClick = onQuantityTap, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.widthIn(min = 36.dp)) {
                    Text(cartItem.quantity.toInt().toString(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            FilledTonalIconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
        }
        Text((cartItem.product.price * cartItem.quantity).formatPeso(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 72.dp), textAlign = TextAlign.End)
    }
}

// ── Always-on inline barcode scanner strip ─────────────────────────────────────

/** Generates and plays a short sine wave beep tone on the main thread via AudioTrack */
private fun playBeep() {
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        try {
            val sampleRate = 44100
            val durationMs = 150
            val freqHz = 1800.0
            val numSamples = sampleRate * durationMs / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i / (sampleRate / freqHz)
                samples[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.7).toInt().toShort()
            }
            val bufferSize = samples.size * 2
            val audioTrack = android.media.AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    android.media.AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                .build()
            audioTrack.write(samples, 0, samples.size)
            audioTrack.setNotificationMarkerPosition(samples.size)
            audioTrack.setPlaybackPositionUpdateListener(object : android.media.AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: android.media.AudioTrack?) { track?.release() }
                override fun onPeriodicNotification(track: android.media.AudioTrack?) {}
            })
            audioTrack.play()
        } catch (_: Exception) { /* silently fail — audio is non-critical */ }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun InlineScannerStrip(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use rememberUpdatedState so the analyzer lambda always calls the latest callback
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    // ── Two-layer barcode protection ──────────────────────────
    // Layer 1: Multi-frame confirmation — same barcode must be seen for N consecutive frames
    val pendingBarcodeRef = remember { mutableStateOf<String?>(null) }
    val confirmCountRef = remember { mutableIntStateOf(0) }
    val confirmThreshold = 15 // ~0.5 second at 30fps

    // Layer 2: Hard lockout — after a successful fire, ignore ALL detections for 3 seconds
    val lockoutUntilRef = remember { mutableStateOf(0L) }
    val lockoutDurationMs = 3000L

    Card(modifier = modifier.clip(RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        AndroidView(
            factory = { ctx ->
                // Wrap PreviewView in a clipping FrameLayout to prevent camera overflow
                val cornerRadiusPx = (14 * ctx.resources.displayMetrics.density).toInt()
                val wrapper = android.widget.FrameLayout(ctx).apply {
                    clipChildren = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
                        }
                    }
                    clipToOutline = true
                }
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                wrapper.addView(previewView)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val scanner = BarcodeScanning.getClient()
                    val executor = Executors.newSingleThreadExecutor()
                    val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val now = System.currentTimeMillis()

                                    // Layer 2: Hard lockout check
                                    if (now < lockoutUntilRef.value) {
                                        // Still in lockout — ignore everything
                                        return@addOnSuccessListener
                                    }

                                    if (barcodes.isEmpty()) {
                                        // No barcode in view — reset confirmation streak
                                        pendingBarcodeRef.value = null
                                        confirmCountRef.intValue = 0
                                    } else {
                                        val firstValid = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf { v -> v.isNotBlank() } }
                                        if (firstValid != null) {
                                            if (firstValid == pendingBarcodeRef.value) {
                                                // Same barcode as pending — increment confirmation
                                                confirmCountRef.intValue++
                                            } else {
                                                // Different barcode — restart confirmation
                                                pendingBarcodeRef.value = firstValid
                                                confirmCountRef.intValue = 1
                                            }

                                            // Layer 1: Check if confirmation threshold reached
                                            if (confirmCountRef.intValue >= confirmThreshold) {
                                                // FIRE! Reset state and start lockout
                                                lockoutUntilRef.value = now + lockoutDurationMs
                                                pendingBarcodeRef.value = null
                                                confirmCountRef.intValue = 0
                                                playBeep()
                                                currentOnBarcodeScanned(firstValid)
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else imageProxy.close()
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                    } catch (e: Exception) { Log.e("InlineScanner", "Camera bind failed", e) }
                }, ContextCompat.getMainExecutor(ctx))
                wrapper
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
