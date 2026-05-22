'use strict';

// ── Utilities ──────────────────────────────────────────────────
const API = '';

function formatPeso(val) {
  return new Intl.NumberFormat('en-PH', {
    style: 'currency', currency: 'PHP', minimumFractionDigits: 2
  }).format(Number(val) || 0);
}

function formatDate(iso) {
  const d = new Date(iso);
  return d.toLocaleString('en-PH', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}

function $(sel) { return document.querySelector(sel); }
function $$(sel) { return document.querySelectorAll(sel); }

async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Request failed');
  return data;
}

function showToast(msg, type = '') {
  const container = $('#toast-container');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = msg;
  container.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 300); }, 2500);
}

// ── State ──────────────────────────────────────────────────────
let cart = [];          // { product, quantity }
let allProducts = [];
let editingProductId = null;
let confirmCallback = null;

// ── Mode Toggle (Cashier / Admin) ──────────────────────────────
const modeToggle = $('#mode-toggle');
const pillBtns = $$('.pill-btn');
const cashierView = $('#cashier-view');
const adminView = $('#admin-view');

function setMode(mode) {
  modeToggle.dataset.active = mode;
  pillBtns.forEach(btn => btn.classList.toggle('active', btn.dataset.mode === mode));
  cashierView.classList.toggle('active', mode === 'cashier');
  adminView.classList.toggle('active', mode === 'admin');

  if (mode === 'cashier') {
    loadProducts();
    setTimeout(() => $('#cashier-search')?.focus(), 100);
  } else {
    loadInventory();
    loadHistory();
  }
}

pillBtns.forEach(btn => {
  btn.addEventListener('click', () => setMode(btn.dataset.mode));
});

// ── Admin Tabs ─────────────────────────────────────────────────
$$('.admin-tab').forEach(tab => {
  tab.addEventListener('click', () => {
    $$('.admin-tab').forEach(t => t.classList.remove('active'));
    $$('.admin-panel').forEach(p => p.classList.remove('active'));
    tab.classList.add('active');
    $(`#${tab.dataset.panel}`).classList.add('active');
  });
});

// ── Products (Cashier) ─────────────────────────────────────────
const productGrid = $('#product-grid');
const cashierSearch = $('#cashier-search');
const cashierSearchClear = $('#cashier-search-clear');
const inventorySearchClear = $('#inventory-search-clear');
let searchTimeout;



async function loadProducts(query) {
  try {
    const qp = query ? `?q=${encodeURIComponent(query)}` : '';
    const data = await api('GET', `/api/products${qp}`);
    allProducts = data.products;
    renderProductGrid(data.products);
  } catch (err) {
    productGrid.innerHTML = `<div class="empty-state"><p>Failed to load products</p></div>`;
  }
}

function renderProductGrid(products) {
  if (products.length === 0) {
    productGrid.innerHTML = `
      <div class="empty-state" style="grid-column: 1/-1">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 002 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0022 16z"/>
          <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
          <line x1="12" y1="22.08" x2="12" y2="12"/>
        </svg>
        <h3>No products found</h3>
        <p>Try a different search term or add products from the Admin panel.</p>
      </div>`;
    return;
  }

  productGrid.innerHTML = products.map((p, i) => {
    const outClass = p.stock <= 0 ? ' out-of-stock' : '';
    let stockClass = '';
    let stockLabel = `${p.stock} ${p.unit}`;
    if (p.stock <= 0) { stockClass = 'out'; stockLabel = 'Out of stock'; }
    else if (p.stock <= p.min_warning) { stockClass = 'low'; stockLabel = `${p.stock} ${p.unit} (low)`; }

    return `
      <div class="product-card${outClass}" data-product-id="${p.id}">
        <span class="product-variant">${esc(p.variant)}</span>
        <span class="product-name">${esc(p.name)}</span>
        <span class="product-stock ${stockClass}">${stockLabel}</span>
        ${p.barcode ? `<span class="product-barcode">${esc(p.barcode)}</span>` : ''}
        <span class="product-price">${formatPeso(p.sale_price)}</span>
      </div>`;
  }).join('');
}

productGrid.addEventListener('click', (e) => {
  const card = e.target.closest('.product-card');
  if (!card || card.classList.contains('out-of-stock')) return;
  const id = Number(card.dataset.productId);
  const product = allProducts.find(p => p.id === id);
  if (product) {
    addToCart(product);
    cashierSearch.value = '';
    loadProducts('');
    cashierSearch.focus();
  }
});

cashierSearch.addEventListener('input', () => {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => loadProducts(cashierSearch.value), 200);
});

cashierSearchClear.addEventListener('click', () => {
  cashierSearch.value = '';
  loadProducts('');
  cashierSearch.focus();
});

cashierSearch.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    const query = cashierSearch.value.trim().toLowerCase();
    if (!query) return;

    // Match strategy:
    // 1. Try exact barcode match (case-insensitive)
    let matchedProduct = allProducts.find(p => p.barcode && p.barcode.trim().toLowerCase() === query);

    // 2. Try exact name match (case-insensitive)
    if (!matchedProduct) {
      matchedProduct = allProducts.find(p => p.name.trim().toLowerCase() === query);
    }

    // 3. Fallback: if there is exactly 1 product matched in allProducts (or if local match is unique)
    if (!matchedProduct) {
      if (allProducts.length === 1) {
        matchedProduct = allProducts[0];
      } else {
        const localMatches = allProducts.filter(p => 
          p.name.toLowerCase().includes(query) ||
          p.variant.toLowerCase().includes(query) ||
          (p.barcode && p.barcode.toLowerCase().includes(query))
        );
        if (localMatches.length === 1) {
          matchedProduct = localMatches[0];
        }
      }
    }

    if (matchedProduct) {
      if (matchedProduct.stock <= 0) {
        showToast(`${matchedProduct.name} is out of stock`, 'error');
        return;
      }
      addToCart(matchedProduct);
      cashierSearch.value = '';
      loadProducts('');
      cashierSearch.focus();
    } else {
      showToast('No unique matching product found', 'error');
    }
  }
});

// Auto-focus search for barcode scanning
document.addEventListener('keydown', (e) => {
  if (cashierView.classList.contains('active') &&
      !e.target.closest('.modal-backdrop') &&
      e.target.tagName !== 'INPUT' &&
      e.target.tagName !== 'TEXTAREA') {
    cashierSearch.focus();
  }
});

// ── Cart ───────────────────────────────────────────────────────
const cartItems = $('#cart-items');
const cartCount = $('#cart-count');
const cartTotal = $('#cart-total');
const finalizeBtn = $('#finalize-btn');
const clearCartBtn = $('#clear-cart-btn');

function addToCart(product) {
  const existing = cart.find(c => c.product.id === product.id);
  if (existing) {
    if (existing.quantity < product.stock) {
      existing.quantity++;
    } else {
      showToast(`Maximum stock reached for ${product.name}`, 'error');
      return;
    }
  } else {
    cart.push({ product, quantity: 1 });
  }
  renderCart();
  showToast(`${product.name} added`);
}

function updateCartQty(productId, delta) {
  const item = cart.find(c => c.product.id === productId);
  if (!item) return;
  const newQty = item.quantity + delta;
  if (newQty <= 0) {
    cart = cart.filter(c => c.product.id !== productId);
  } else if (newQty > item.product.stock) {
    showToast('Maximum stock reached', 'error');
    return;
  } else {
    item.quantity = newQty;
  }
  renderCart();
}

function removeFromCart(productId) {
  cart = cart.filter(c => c.product.id !== productId);
  renderCart();
}

function clearCart() {
  cart = [];
  renderCart();
}

function getCartTotal() {
  return cart.reduce((sum, c) => sum + c.product.sale_price * c.quantity, 0);
}

function renderCart() {
  const total = getCartTotal();
  const count = cart.reduce((s, c) => s + c.quantity, 0);

  cartCount.textContent = `${count} item${count !== 1 ? 's' : ''}`;
  cartTotal.textContent = formatPeso(total);
  finalizeBtn.disabled = cart.length === 0;
  clearCartBtn.style.display = cart.length > 0 ? '' : 'none';

  if (cart.length === 0) {
    cartItems.innerHTML = `
      <div class="cart-empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
          <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/>
        </svg>
        <p>Cart is empty</p>
        <p style="font-size:0.75rem">Tap a product to add it here</p>
      </div>`;
    return;
  }

  cartItems.innerHTML = cart.map(c => `
    <div class="cart-item">
      <div class="cart-item-info">
        <div class="cart-item-name">${esc(c.product.name)} ${esc(c.product.variant)}</div>
        <div class="cart-item-price">${formatPeso(c.product.sale_price)} each</div>
      </div>
      <div class="cart-item-qty">
        <button class="remove-btn" data-id="${c.product.id}" data-action="minus" aria-label="Decrease quantity">−</button>
        <span class="qty-value">${c.quantity}</span>
        <button data-id="${c.product.id}" data-action="plus" aria-label="Increase quantity">+</button>
      </div>
      <div class="cart-item-subtotal">${formatPeso(c.product.sale_price * c.quantity)}</div>
    </div>
  `).join('');
}

cartItems.addEventListener('click', (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  if (btn.dataset.action === 'plus') updateCartQty(id, 1);
  else if (btn.dataset.action === 'minus') updateCartQty(id, -1);
});

clearCartBtn.addEventListener('click', () => {
  showConfirm('Clear Cart', 'Remove all items from the current sale?', () => {
    clearCart();
    showToast('Cart cleared');
  });
});

// ── Payment Modal ──────────────────────────────────────────────
const paymentModal = $('#payment-modal');
const paymentSummary = $('#payment-summary');
const cashDisplay = $('#cash-display');
const changeDisplay = $('#change-display');
const confirmPaymentBtn = $('#confirm-payment-btn');
let cashInput = '';

function openPaymentModal() {
  const total = getCartTotal();
  cashInput = '';
  updateCashDisplay();

  // Build summary
  paymentSummary.innerHTML = `
    <div class="payment-row"><span class="label">Items</span><span class="value">${cart.reduce((s, c) => s + c.quantity, 0)}</span></div>
    <div class="payment-row total"><span class="label">Total Due</span><span class="value">${formatPeso(total)}</span></div>
  `;

  paymentModal.classList.add('open');
}

function closePaymentModal() {
  paymentModal.classList.remove('open');
  cashInput = '';
}

function updateCashDisplay() {
  const cash = parseFloat(cashInput) || 0;
  const total = getCartTotal();
  const change = Math.max(0, cash - total);

  cashDisplay.textContent = cashInput ? `₱${cashInput}` : '₱0.00';
  cashDisplay.classList.toggle('has-value', cashInput.length > 0);

  changeDisplay.innerHTML = `
    <span class="label">Change</span>
    <span class="value">${formatPeso(change)}</span>
  `;
  confirmPaymentBtn.disabled = cash < total;
}

// Numpad handlers
$('#numpad').addEventListener('click', (e) => {
  const btn = e.target.closest('.numpad-btn');
  if (!btn) return;
  const key = btn.dataset.key;

  if (key >= '0' && key <= '9') {
    if (cashInput.length < 10) cashInput += key;
  } else if (key === '.') {
    if (!cashInput.includes('.')) cashInput += cashInput.length === 0 ? '0.' : '.';
  } else if (key === 'backspace') {
    cashInput = cashInput.slice(0, -1);
  } else if (key === 'clear') {
    cashInput = '';
  } else if (key === 'exact') {
    cashInput = getCartTotal().toString();
  } else if (key === 'confirm') {
    if (!confirmPaymentBtn.disabled) processSale();
    return;
  }

  updateCashDisplay();
});

// Quick amount buttons
$('#quick-amounts').addEventListener('click', (e) => {
  const btn = e.target.closest('.quick-amount-btn');
  if (!btn) return;
  const amount = Number(btn.dataset.amount);
  const current = parseFloat(cashInput) || 0;
  cashInput = (current + amount).toString();
  updateCashDisplay();
});

// Keyboard support in payment modal
document.addEventListener('keydown', (e) => {
  if (!paymentModal.classList.contains('open')) return;
  if (e.key >= '0' && e.key <= '9') {
    if (cashInput.length < 10) cashInput += e.key;
  } else if (e.key === '.') {
    if (!cashInput.includes('.')) cashInput += cashInput.length === 0 ? '0.' : '.';
  } else if (e.key === 'Backspace') {
    cashInput = cashInput.slice(0, -1);
  } else if (e.key === 'Escape') {
    closePaymentModal();
    return;
  } else if (e.key === 'Enter') {
    if (!confirmPaymentBtn.disabled) processSale();
    return;
  } else {
    return;
  }
  e.preventDefault();
  updateCashDisplay();
});

async function processSale() {
  const cash = parseFloat(cashInput) || 0;
  const total = getCartTotal();
  if (cash < total) return;

  try {
    confirmPaymentBtn.disabled = true;
    const items = cart.map(c => ({ product_id: c.product.id, quantity: c.quantity }));
    const result = await api('POST', '/api/sales', { items, cash_tendered: cash });

    closePaymentModal();
    clearCart();

    showToast(`Sale #${result.sale.id} completed — Change: ${formatPeso(result.sale.change_amount)}`, 'success');
    loadProducts(cashierSearch.value);
  } catch (err) {
    showToast(err.message, 'error');
    confirmPaymentBtn.disabled = false;
  }
}

finalizeBtn.addEventListener('click', openPaymentModal);
$('#payment-close').addEventListener('click', closePaymentModal);
paymentModal.addEventListener('click', (e) => {
  if (e.target === paymentModal) closePaymentModal();
});

// ── Inventory (Admin) ──────────────────────────────────────────
const inventoryTbody = $('#inventory-tbody');
const inventorySearch = $('#inventory-search');
let invSearchTimeout;

async function loadInventory(query) {
  try {
    const qp = query ? `?q=${encodeURIComponent(query)}` : '';
    const data = await api('GET', `/api/products${qp}`);
    renderInventory(data.products);
  } catch (err) {
    inventoryTbody.innerHTML = `<tr><td colspan="8" class="empty-state">Failed to load inventory</td></tr>`;
  }
}

function renderInventory(products) {
  if (products.length === 0) {
    inventoryTbody.innerHTML = `<tr><td colspan="8"><div class="empty-state"><h3>No products</h3><p>Add your first product to get started.</p></div></td></tr>`;
    return;
  }

  inventoryTbody.innerHTML = products.map(p => {
    let badgeClass = 'badge-ok', statusText = 'OK';
    if (p.stock <= 0) { badgeClass = 'badge-danger'; statusText = 'Out'; }
    else if (p.stock <= p.min_warning) { badgeClass = 'badge-warn'; statusText = 'Low'; }

    return `
      <tr>
        <td><strong>${esc(p.name)}</strong></td>
        <td>${esc(p.variant)}</td>
        <td style="font-family:var(--font-mono);font-size:var(--font-xs)">${esc(p.barcode || '—')}</td>
        <td class="text-right">${p.stock} ${esc(p.unit)}</td>
        <td class="text-right">${formatPeso(p.cost_price)}</td>
        <td class="text-right">${formatPeso(p.sale_price)}</td>
        <td class="text-center"><span class="badge ${badgeClass}">${statusText}</span></td>
        <td class="text-center">
          <div class="action-btns" style="justify-content:center">
            <button class="action-btn edit" data-id="${p.id}" data-action="edit">Edit</button>
            <button class="action-btn delete" data-id="${p.id}" data-action="delete">Delete</button>
          </div>
        </td>
      </tr>`;
  }).join('');
}

inventoryTbody.addEventListener('click', (e) => {
  const btn = e.target.closest('.action-btn');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  if (btn.dataset.action === 'edit') openProductModal(id);
  else if (btn.dataset.action === 'delete') deleteProduct(id);
});

inventorySearch.addEventListener('input', () => {
  clearTimeout(invSearchTimeout);
  invSearchTimeout = setTimeout(() => loadInventory(inventorySearch.value), 200);
});

inventorySearchClear.addEventListener('click', () => {
  inventorySearch.value = '';
  loadInventory('');
  inventorySearch.focus();
});

// ── Product Modal (Add/Edit) ───────────────────────────────────
const productModal = $('#product-modal');

function openProductModal(productId = null) {
  editingProductId = productId;
  $('#product-modal-title').textContent = productId ? 'Edit Product' : 'Add Product';

  if (productId) {
    api('GET', `/api/products/${productId}`).then(data => {
      const p = data.product;
      $('#pf-name').value = p.name;
      $('#pf-variant').value = p.variant;
      $('#pf-unit').value = p.unit;
      $('#pf-cost').value = p.cost_price;
      $('#pf-price').value = p.sale_price;
      $('#pf-stock').value = p.stock;
      $('#pf-warning').value = p.min_warning;
      $('#pf-barcode').value = p.barcode || '';
    });
  } else {
    $('#pf-name').value = '';
    $('#pf-variant').value = '';
    $('#pf-unit').value = 'pcs';
    $('#pf-cost').value = '';
    $('#pf-price').value = '';
    $('#pf-stock').value = '';
    $('#pf-warning').value = '';
    $('#pf-barcode').value = '';
  }

  productModal.classList.add('open');
  setTimeout(() => $('#pf-name').focus(), 150);
}

function closeProductModal() {
  productModal.classList.remove('open');
  editingProductId = null;
}

async function saveProduct() {
  const body = {
    name: $('#pf-name').value.trim(),
    variant: $('#pf-variant').value.trim(),
    unit: $('#pf-unit').value.trim() || 'pcs',
    cost_price: parseFloat($('#pf-cost').value) || 0,
    sale_price: parseFloat($('#pf-price').value) || 0,
    stock: parseFloat($('#pf-stock').value) || 0,
    min_warning: parseFloat($('#pf-warning').value) || 0,
    barcode: $('#pf-barcode').value.trim(),
  };

  if (!body.name) {
    showToast('Product name is required', 'error');
    return;
  }

  try {
    if (editingProductId) {
      await api('PUT', `/api/products/${editingProductId}`, body);
      showToast('Product updated', 'success');
    } else {
      await api('POST', '/api/products', body);
      showToast('Product added', 'success');
    }
    closeProductModal();
    loadInventory(inventorySearch.value);
    loadProducts(cashierSearch.value);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteProduct(id) {
  try {
    const data = await api('GET', `/api/products/${id}`);
    const p = data.product;
    showConfirm('Delete Product', `Remove "${p.name} ${p.variant}" from inventory? This cannot be undone.`, async () => {
      try {
        await api('DELETE', `/api/products/${id}`);
        showToast('Product deleted', 'success');
        loadInventory(inventorySearch.value);
        loadProducts(cashierSearch.value);
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  } catch (err) {
    showToast(err.message, 'error');
  }
}

$('#add-product-btn').addEventListener('click', () => openProductModal());
$('#product-save-btn').addEventListener('click', saveProduct);
$('#product-cancel-btn').addEventListener('click', closeProductModal);
$('#product-modal-close').addEventListener('click', closeProductModal);
productModal.addEventListener('click', (e) => { if (e.target === productModal) closeProductModal(); });

// Handle Enter key in product form
$$('#product-modal .form-input').forEach(input => {
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') saveProduct();
  });
});

// ── History (Admin) ────────────────────────────────────────────
const historyList = $('#history-list');

async function loadHistory() {
  try {
    const data = await api('GET', '/api/sales');
    renderHistory(data.sales);
  } catch (err) {
    historyList.innerHTML = `<div class="empty-state"><p>Failed to load history</p></div>`;
  }
}

function renderHistory(sales) {
  if (sales.length === 0) {
    historyList.innerHTML = `
      <div class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
        </svg>
        <h3>No transactions yet</h3>
        <p>Completed sales will appear here.</p>
      </div>`;
    return;
  }

  historyList.innerHTML = sales.map(s => `
    <div class="history-card" data-sale-id="${s.id}">
      <div class="history-card-header" data-sale-id="${s.id}">
        <div class="history-card-left">
          <span class="history-card-id">Sale #${s.id}</span>
          <span class="history-card-date">${formatDate(s.created_at)}</span>
        </div>
        <div class="history-card-right">
          <span class="history-card-items-count">${s.item_count} item${s.item_count !== 1 ? 's' : ''}</span>
          <span class="history-card-total">${formatPeso(s.total_amount)}</span>
          <svg class="history-card-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"/>
          </svg>
        </div>
      </div>
      <div class="history-card-body" id="sale-detail-${s.id}">
        <p style="color:var(--text-muted);font-size:var(--font-sm)">Loading...</p>
      </div>
    </div>
  `).join('');
}

historyList.addEventListener('click', async (e) => {
  const header = e.target.closest('.history-card-header');
  if (!header) return;
  const card = header.closest('.history-card');
  const saleId = card.dataset.saleId;
  const wasExpanded = card.classList.contains('expanded');

  // Collapse all others
  $$('.history-card.expanded').forEach(c => c.classList.remove('expanded'));

  if (wasExpanded) return;

  card.classList.add('expanded');

  // Load detail
  const detailEl = $(`#sale-detail-${saleId}`);
  try {
    const data = await api('GET', `/api/sales/${saleId}`);
    const items = data.items;
    const sale = data.sale;

    detailEl.innerHTML = `
      <table class="history-detail-table">
        <thead>
          <tr><th>Product</th><th>Variant</th><th style="text-align:right">Qty</th><th style="text-align:right">Price</th><th style="text-align:right">Subtotal</th></tr>
        </thead>
        <tbody>
          ${items.map(it => `
            <tr>
              <td>${esc(it.product_name)}</td>
              <td>${esc(it.product_variant)}</td>
              <td style="text-align:right">${it.quantity}</td>
              <td style="text-align:right">${formatPeso(it.unit_price)}</td>
              <td style="text-align:right">${formatPeso(it.subtotal)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
      <div class="history-payment-info">
        <div>Cash: <span>${formatPeso(sale.cash_tendered)}</span></div>
        <div>Change: <span>${formatPeso(sale.change_amount)}</span></div>
      </div>
    `;
  } catch (err) {
    detailEl.innerHTML = `<p style="color:var(--danger)">Failed to load details</p>`;
  }
});

// ── Confirm Modal ──────────────────────────────────────────────
const confirmModalEl = $('#confirm-modal');

function showConfirm(title, message, onConfirm) {
  $('#confirm-modal-title').textContent = title;
  $('#confirm-modal-message').textContent = message;
  confirmCallback = onConfirm;
  confirmModalEl.classList.add('open');
}

function closeConfirmModal() {
  confirmModalEl.classList.remove('open');
  confirmCallback = null;
}

$('#confirm-yes-btn').addEventListener('click', () => {
  if (confirmCallback) confirmCallback();
  closeConfirmModal();
});
$('#confirm-no-btn').addEventListener('click', closeConfirmModal);
$('#confirm-modal-close').addEventListener('click', closeConfirmModal);
confirmModalEl.addEventListener('click', (e) => { if (e.target === confirmModalEl) closeConfirmModal(); });

// ── Escape key for all modals ──────────────────────────────────
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    if (confirmModalEl.classList.contains('open')) closeConfirmModal();
    else if (productModal.classList.contains('open')) closeProductModal();
    else if (paymentModal.classList.contains('open')) closePaymentModal();
  }
});

// ── HTML Escaping ──────────────────────────────────────────────
function esc(str) {
  const d = document.createElement('div');
  d.textContent = str || '';
  return d.innerHTML;
}

// ── Init ───────────────────────────────────────────────────────
loadProducts();
renderCart();
