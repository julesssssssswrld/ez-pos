// app.js - mock authentication, storefront payment, and admin sync for Liquid Soap POS
const STORAGE_USERS_KEY = 'soap_users';
const STORAGE_PRODUCTS_KEY = 'soap_products';
const STORAGE_SALES_KEY = 'soap_sales';
const STORAGE_SESSION_KEY = 'soap_session';

function initMockData() {
  const existingUsers = localStorage.getItem(STORAGE_USERS_KEY);
  const existingProducts = localStorage.getItem(STORAGE_PRODUCTS_KEY);

  if (!existingUsers) {
    const users = [
      { username: 'admin', password: '123', role: 'admin', label: 'Admin Manager' },
      { username: 'store', password: '123', role: 'store', label: 'Store Watcher' }
    ];
    localStorage.setItem(STORAGE_USERS_KEY, JSON.stringify(users));
  }

  if (!existingProducts) {
    const products = [
      { id: 1, name: 'Liquid Soap Bulk', variant: '20L Drum', price: 650.00, cost: 420.00, stock: 24, min_warning: 6, unit: 'liters', image: '' },
      { id: 2, name: 'Liquid Soap Bottle', variant: '500ml', price: 45.00, cost: 28.00, stock: 180, min_warning: 20, unit: 'pcs', image: '' },
      { id: 3, name: 'Liquid Soap Pouch', variant: '250ml', price: 28.00, cost: 18.00, stock: 320, min_warning: 30, unit: 'pcs', image: '' }
    ];
    localStorage.setItem(STORAGE_PRODUCTS_KEY, JSON.stringify(products));
  }

  if (!localStorage.getItem(STORAGE_SALES_KEY)) {
    localStorage.setItem(STORAGE_SALES_KEY, JSON.stringify([]));
  }
}

function getUsers() {
  return JSON.parse(localStorage.getItem(STORAGE_USERS_KEY) || '[]');
}

function saveUsers(users) {
  localStorage.setItem(STORAGE_USERS_KEY, JSON.stringify(users));
}

function getProducts() {
  return JSON.parse(localStorage.getItem(STORAGE_PRODUCTS_KEY) || '[]');
}

function saveProducts(products) {
  localStorage.setItem(STORAGE_PRODUCTS_KEY, JSON.stringify(products));
}

function getSales() {
  return JSON.parse(localStorage.getItem(STORAGE_SALES_KEY) || '[]');
}

function saveSales(sales) {
  localStorage.setItem(STORAGE_SALES_KEY, JSON.stringify(sales));
}

function setSession(user) {
  localStorage.setItem(STORAGE_SESSION_KEY, JSON.stringify({ username: user.username, role: user.role }));
}

function getSession() {
  return JSON.parse(localStorage.getItem(STORAGE_SESSION_KEY) || 'null');
}

function clearSession() {
  localStorage.removeItem(STORAGE_SESSION_KEY);
}

function loginUser(username, password) {
  const users = getUsers();
  const user = users.find(u => u.username === username && u.password === password);
  if (!user) {
    return null;
  }
  setSession(user);
  return user;
}

function redirectAfterLogin(user) {
  if (user.role === 'admin') {
    window.location.href = 'admin.html';
  } else if (user.role === 'store') {
    window.location.href = 'store.html';
  } else {
    window.location.href = 'index.html';
  }
}

function requireAuth(allowedRoles = []) {
  const session = getSession();
  if (!session || !allowedRoles.includes(session.role)) {
    window.location.href = 'index.html';
    return null;
  }
  return session;
}

function logout() {
  clearSession();
  window.location.href = 'index.html';
}

function formatPeso(value) {
  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency: 'PHP',
    maximumFractionDigits: 2
  }).format(Number(value) || 0);
}

function formatStock(value) {
  return new Intl.NumberFormat('en-PH', { maximumFractionDigits: 0 }).format(Number(value) || 0);
}

function updateRoleBadges() {
  const session = getSession();
  if (!session) return;
  const storeBadge = document.getElementById('role-badge');
  if (storeBadge) {
    storeBadge.textContent = session.role === 'store' ? 'Cashier' : session.role;
  }
  const adminBadge = document.getElementById('admin-role-badge');
  if (adminBadge) {
    adminBadge.textContent = session.role === 'admin' ? 'Admin' : session.role;
  }
}

function renderStorefront() {
  const grid = document.getElementById('product-grid');
  if (!grid) return;
  const session = requireAuth(['store']);
  if (!session) return;
  updateRoleBadges();

  const products = getProducts();
  grid.innerHTML = '';

  if (products.length === 0) {
    grid.innerHTML = '<div class="rounded-3xl bg-white p-8 text-center text-slate-500 shadow-sm">No products available.</div>';
    return;
  }

  products.forEach(product => {
    const disabled = product.stock <= 0;
    const imageSrc = product.image ? product.image : 'https://via.placeholder.com/400x260?text=Liquid+Soap';
    const card = document.createElement('article');
    card.className = 'rounded-3xl border border-slate-200 bg-white p-5 shadow-sm';
    card.innerHTML = `
      <div class="mb-4 h-40 overflow-hidden rounded-3xl bg-slate-100">
        <img src="${imageSrc}" alt="${product.name}" class="h-full w-full object-cover" />
      </div>
      <div class="space-y-3">
        <p class="text-sm uppercase tracking-[0.3em] text-slate-400">${product.variant}</p>
        <h3 class="text-xl font-semibold text-slate-900">${product.name}</h3>
        <p class="text-sm text-slate-600">Stock: ${formatStock(product.stock)} ${product.unit}</p>
        <p class="text-lg font-semibold text-slate-900">${formatPeso(product.price)}</p>
        <button data-product-id="${product.id}" class="purchase-btn w-full rounded-2xl ${disabled ? 'bg-slate-300 text-slate-500 cursor-not-allowed' : 'bg-emerald-600 text-white hover:bg-emerald-700'} px-4 py-3 text-sm font-semibold" ${disabled ? 'disabled' : ''}>${disabled ? 'Out of Stock' : 'Purchase'}</button>
      </div>
    `;
    grid.appendChild(card);
  });
}

let currentProductId = null;

function openPaymentModal(productId) {
  const product = getProducts().find(p => p.id === productId);
  if (!product) return;
  currentProductId = productId;
  const modal = document.getElementById('payment-modal');
  const title = document.getElementById('modal-product-title');
  const totalPrice = document.getElementById('modal-total-price');
  const cashInput = document.getElementById('cash-received');
  const changeDisplay = document.getElementById('modal-change');

  title.textContent = `${product.name} · ${product.variant}`;
  totalPrice.textContent = formatPeso(product.price);
  cashInput.value = '';
  changeDisplay.textContent = formatPeso(0);
  modal.classList.remove('hidden');
}

function closePaymentModal() {
  const modal = document.getElementById('payment-modal');
  if (modal) {
    modal.classList.add('hidden');
  }
  currentProductId = null;
}

function updateChange() {
  const cashInput = document.getElementById('cash-received');
  const changeDisplay = document.getElementById('modal-change');
  if (!cashInput || !changeDisplay) return;
  const cashValue = parseFloat(cashInput.value) || 0;
  const product = getProducts().find(p => p.id === currentProductId);
  if (!product) {
    changeDisplay.textContent = formatPeso(0);
    return;
  }
  const change = cashValue - product.price;
  changeDisplay.textContent = formatPeso(change >= 0 ? change : 0);
}

function confirmSale() {
  const cashInput = document.getElementById('cash-received');
  const product = getProducts().find(p => p.id === currentProductId);
  if (!product) {
    alert('Product not found.');
    return;
  }

  const cashValue = parseFloat(cashInput.value) || 0;
  if (cashValue < product.price) {
    alert('Insufficient cash. Please enter enough money to cover the total price.');
    return;
  }
  if (product.stock <= 0) {
    alert('This item is out of stock.');
    closePaymentModal();
    return;
  }

  const products = getProducts();
  const updatedProducts = products.map(item => {
    if (item.id === product.id) {
      return { ...item, stock: item.stock - 1 };
    }
    return item;
  });
  saveProducts(updatedProducts);

  const sales = getSales();
  const totalAmount = product.price;
  const profitAmount = totalAmount - product.cost;
  const newSale = {
    id: Date.now(),
    product_id: product.id,
    product_name: product.name,
    quantity: 1,
    unit_price: product.price,
    total_amount: totalAmount,
    cost_amount: product.cost,
    profit_amount: profitAmount,
    created_at: new Date().toISOString()
  };
  saveSales([...sales, newSale]);

  const change = cashValue - totalAmount;
  alert(`Sale completed. Change: ${formatPeso(change)}`);
  closePaymentModal();
  renderStorefront();
}

function setupStorePage() {
  const grid = document.getElementById('product-grid');
  if (!grid) return;
  const session = requireAuth(['store']);
  if (!session) return;
  updateRoleBadges();
  renderStorefront();

  grid.addEventListener('click', event => {
    const button = event.target.closest('.purchase-btn');
    if (!button || button.disabled) return;
    const productId = Number(button.dataset.productId);
    openPaymentModal(productId);
  });

  const closeButton = document.getElementById('modal-close');
  if (closeButton) {
    closeButton.addEventListener('click', closePaymentModal);
  }

  const cashInput = document.getElementById('cash-received');
  if (cashInput) {
    cashInput.addEventListener('input', updateChange);
  }

  const confirmButton = document.getElementById('confirm-sale');
  if (confirmButton) {
    confirmButton.addEventListener('click', confirmSale);
  }

  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', logout);
  }

  window.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
      closePaymentModal();
    }
  });
}

function getTodaySales() {
  const sales = getSales();
  const todayKey = new Date().toISOString().slice(0, 10);
  return sales.filter(sale => sale.created_at.slice(0, 10) === todayKey);
}

function renderAdminCards() {
  const revenueEl = document.getElementById('card-revenue');
  const profitEl = document.getElementById('card-profit');
  const salesEl = document.getElementById('card-sales');
  const purchasesEl = document.getElementById('card-purchases');
  if (!revenueEl || !profitEl || !salesEl || !purchasesEl) return;

  const todaysSales = getTodaySales();
  const revenue = todaysSales.reduce((sum, sale) => sum + sale.total_amount, 0);
  const profit = todaysSales.reduce((sum, sale) => sum + sale.profit_amount, 0);
  const purchases = 0;

  revenueEl.textContent = formatPeso(revenue);
  profitEl.textContent = formatPeso(profit);
  salesEl.textContent = formatPeso(revenue);
  purchasesEl.textContent = formatPeso(purchases);
}

function renderAdminAlerts() {
  const alertContainer = document.getElementById('admin-alerts');
  if (!alertContainer) return;
  const products = getProducts();
  const alerts = products
    .filter(product => product.stock <= product.min_warning)
    .sort((a, b) => a.stock - b.stock);

  if (alerts.length === 0) {
    alertContainer.innerHTML = '<div class="rounded-2xl border border-green-200 bg-green-50 p-4 text-sm text-emerald-900">All products are above their warning thresholds.</div>';
    return;
  }

  alertContainer.innerHTML = '';
  alerts.forEach(product => {
    const status = product.stock <= 0 ? 'Out of stock' : 'Low stock';
    const alertCard = document.createElement('div');
    if (product.stock <= 0) {
      alertCard.className = 'rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-900';
    } else {
      alertCard.className = 'rounded-2xl border border-yellow-200 bg-yellow-50 p-4 text-sm text-yellow-900';
    }
    alertCard.innerHTML = `
      <div class="font-semibold text-slate-900">${product.name} · ${product.variant}</div>
      <div class="mt-1">Stock: ${formatStock(product.stock)} ${product.unit} — ${status}</div>
    `;
    alertContainer.appendChild(alertCard);
  });
}

function renderAdminInventory() {
  const tbody = document.getElementById('inventory-table-body');
  if (!tbody) return;
  const products = getProducts();
  tbody.innerHTML = '';

  if (products.length === 0) {
    tbody.innerHTML = '<tr><td class="px-4 py-4 text-center text-slate-500" colspan="8">No inventory items found.</td></tr>';
    return;
  }

  products.forEach(product => {
    const status = product.stock <= 0 ? 'Out' : product.stock <= product.min_warning ? 'Low' : 'OK';
    const row = document.createElement('tr');
    row.innerHTML = `
      <td class="px-4 py-4 text-left">
        <div class="inline-flex h-14 w-14 overflow-hidden rounded-3xl bg-slate-100">
          <img src="${product.image || 'https://via.placeholder.com/64?text=No+Img'}" alt="${product.name}" class="h-full w-full object-cover" />
        </div>
      </td>
      <td class="px-4 py-4 text-left">${product.name}</td>
      <td class="px-4 py-4 text-left">${product.variant}</td>
      <td class="px-4 py-4 text-right">${formatStock(product.stock)} ${product.unit}</td>
      <td class="px-4 py-4 text-right">${formatStock(product.min_warning)}</td>
      <td class="px-4 py-4 text-right">${formatPeso(product.price)}</td>
      <td class="px-4 py-4 text-center">
        <span class="inline-flex rounded-full px-3 py-1 text-xs font-semibold ${product.stock <= 0 ? 'bg-red-100 text-red-800' : product.stock <= product.min_warning ? 'bg-yellow-100 text-yellow-800' : 'bg-emerald-100 text-emerald-800'}">${status}</span>
      </td>
      <td class="px-4 py-4 text-center space-x-2">
        <button data-action="edit" data-product-id="${product.id}" class="rounded-2xl bg-slate-100 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200">Edit</button>
        <button data-action="remove" data-product-id="${product.id}" class="rounded-2xl bg-red-100 px-3 py-2 text-xs font-semibold text-red-700 hover:bg-red-200">Remove</button>
      </td>
    `;
    tbody.appendChild(row);
  });
}

function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.remove('hidden');
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.add('hidden');
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error('Unable to read image file'));
    reader.readAsDataURL(file);
  });
}

let editingProductId = null;

function openProductModal(productId = null) {
  const title = document.getElementById('product-modal-title');
  const nameInput = document.getElementById('product-name');
  const variantInput = document.getElementById('product-variant');
  const priceInput = document.getElementById('product-price');
  const costInput = document.getElementById('product-cost');
  const stockInput = document.getElementById('product-stock');
  const warningInput = document.getElementById('product-min-warning');
  const unitInput = document.getElementById('product-unit');
  const imageInput = document.getElementById('product-image');
  const imagePreview = document.getElementById('product-image-preview');

  editingProductId = productId;
  if (productId) {
    const product = getProducts().find(p => p.id === productId);
    if (product) {
      title.textContent = 'Edit Product';
      nameInput.value = product.name;
      variantInput.value = product.variant;
      priceInput.value = product.price;
      costInput.value = product.cost;
      stockInput.value = product.stock;
      warningInput.value = product.min_warning;
      unitInput.value = product.unit;
      imageInput.value = '';
      if (product.image) {
        imagePreview.src = product.image;
        imagePreview.classList.remove('hidden');
      } else {
        imagePreview.src = '';
        imagePreview.classList.add('hidden');
      }
    }
  } else {
    title.textContent = 'Add Product';
    nameInput.value = '';
    variantInput.value = '';
    priceInput.value = '';
    costInput.value = '';
    stockInput.value = '';
    warningInput.value = '';
    unitInput.value = '';
    imageInput.value = '';
    imagePreview.src = '';
    imagePreview.classList.add('hidden');
  }
  openModal('product-modal');
}

async function saveProductFromModal() {
  const nameInput = document.getElementById('product-name');
  const variantInput = document.getElementById('product-variant');
  const priceInput = document.getElementById('product-price');
  const costInput = document.getElementById('product-cost');
  const stockInput = document.getElementById('product-stock');
  const warningInput = document.getElementById('product-min-warning');
  const unitInput = document.getElementById('product-unit');
  const imageInput = document.getElementById('product-image');
  const imagePreview = document.getElementById('product-image-preview');

  const name = nameInput.value.trim();
  const variant = variantInput.value.trim();
  const price = parseFloat(priceInput.value) || 0;
  const cost = parseFloat(costInput.value) || 0;
  const stock = parseInt(stockInput.value, 10) || 0;
  const min_warning = parseInt(warningInput.value, 10) || 0;
  const unit = unitInput.value.trim() || 'pcs';
  let image = '';

  if (imageInput.files && imageInput.files[0]) {
    try {
      image = await readFileAsDataUrl(imageInput.files[0]);
      imagePreview.src = image;
      imagePreview.classList.remove('hidden');
    } catch (error) {
      alert('Failed to load the selected image. Please try a different file.');
      return;
    }
  } else if (editingProductId) {
    const existingProduct = getProducts().find(p => p.id === editingProductId);
    image = existingProduct?.image || '';
  }

  if (!name || !variant) {
    alert('Product name and variant are required.');
    return;
  }

  const products = getProducts();
  if (editingProductId) {
    const updatedProducts = products.map(product => product.id === editingProductId ? { ...product, name, variant, price, cost, stock, min_warning, unit, image } : product);
    saveProducts(updatedProducts);
    alert('Product updated successfully.');
  } else {
    const newProduct = {
      id: Date.now(),
      name,
      variant,
      price,
      cost,
      stock,
      min_warning,
      unit,
      image
    };
    saveProducts([...products, newProduct]);
    alert('Product added successfully.');
  }

  closeModal('product-modal');
  renderAdminInventory();
  renderAdminAlerts();
}

function removeProduct(productId) {
  const products = getProducts();
  const product = products.find(p => p.id === productId);
  if (!product) return;
  if (!confirm(`Remove ${product.name} (${product.variant}) from inventory?`)) return;
  saveProducts(products.filter(p => p.id !== productId));
  renderAdminInventory();
  renderAdminAlerts();
  alert('Product removed.');
}

function openPasswordModal() {
  document.getElementById('current-password').value = '';
  document.getElementById('new-password').value = '';
  document.getElementById('confirm-password').value = '';
  openModal('password-modal');
}

function savePassword() {
  const currentPassword = document.getElementById('current-password').value;
  const newPassword = document.getElementById('new-password').value;
  const confirmPassword = document.getElementById('confirm-password').value;
  const session = getSession();
  if (!session) return;

  if (!currentPassword || !newPassword || !confirmPassword) {
    alert('Please fill all password fields.');
    return;
  }
  if (newPassword !== confirmPassword) {
    alert('New password and confirmation do not match.');
    return;
  }

  const users = getUsers();
  const currentUser = users.find(u => u.username === session.username);
  if (!currentUser || currentUser.password !== currentPassword) {
    alert('Current password is incorrect.');
    return;
  }

  currentUser.password = newPassword;
  saveUsers(users);
  alert('Password updated successfully.');
  closeModal('password-modal');
}

function resetSystem() {
  if (!confirm('Reset the system to default settings and clear sales data?')) return;
  localStorage.removeItem(STORAGE_PRODUCTS_KEY);
  localStorage.removeItem(STORAGE_SALES_KEY);
  localStorage.removeItem(STORAGE_USERS_KEY);
  initMockData();
  renderAdminCards();
  renderAdminAlerts();
  renderAdminInventory();
  alert('System reset to defaults.');
}

function exportToExcel() {
  window.location.href = '/api/export?format=xlsx';
}

function setupAdminPage() {
  const revenueEl = document.getElementById('card-revenue');
  if (!revenueEl) return;
  const session = requireAuth(['admin']);
  if (!session) return;
  updateRoleBadges();
  renderAdminCards();
  renderAdminAlerts();
  renderAdminInventory();

  const settingsBtn = document.getElementById('admin-settings-btn');
  const settingsDropdown = document.getElementById('admin-settings-dropdown');
  if (settingsBtn && settingsDropdown) {
    settingsBtn.addEventListener('click', event => {
      event.stopPropagation();
      const isVisible = !settingsDropdown.classList.contains('invisible');
      if (isVisible) {
        settingsDropdown.classList.add('invisible');
      } else {
        settingsDropdown.classList.remove('invisible');
      }
    });
  }

  const dropdownExportBtn = document.getElementById('dropdown-export-btn');
  if (dropdownExportBtn) {
    dropdownExportBtn.addEventListener('click', () => {
      if (settingsDropdown) settingsDropdown.classList.add('invisible');
      exportToExcel();
    });
  }

  const dropdownChangePasswordBtn = document.getElementById('dropdown-change-password-btn');
  if (dropdownChangePasswordBtn) {
    dropdownChangePasswordBtn.addEventListener('click', () => {
      if (settingsDropdown) settingsDropdown.classList.add('invisible');
      openPasswordModal();
    });
  }

  const dropdownResetSystemBtn = document.getElementById('dropdown-reset-system-btn');
  if (dropdownResetSystemBtn) {
    dropdownResetSystemBtn.addEventListener('click', () => {
      if (settingsDropdown) settingsDropdown.classList.add('invisible');
      resetSystem();
    });
  }

  const dropdownLogoutBtn = document.getElementById('dropdown-logout-btn');
  if (dropdownLogoutBtn) {
    dropdownLogoutBtn.addEventListener('click', () => {
      if (settingsDropdown) settingsDropdown.classList.add('invisible');
      logout();
    });
  }

  document.addEventListener('click', () => {
    if (settingsDropdown && !settingsDropdown.classList.contains('invisible')) {
      settingsDropdown.classList.add('invisible');
    }
  });

  const settingsLogoutBtn = document.getElementById('settings-logout-btn');
  if (settingsLogoutBtn) {
    settingsLogoutBtn.addEventListener('click', logout);
  }

  const refreshAlertsBtn = document.getElementById('refresh-alerts');
  if (refreshAlertsBtn) {
    refreshAlertsBtn.addEventListener('click', () => {
      renderAdminAlerts();
      renderAdminCards();
      renderAdminInventory();
    });
  }

  const refreshInventoryBtn = document.getElementById('refresh-inventory');
  if (refreshInventoryBtn) {
    refreshInventoryBtn.addEventListener('click', () => {
      renderAdminInventory();
      renderAdminCards();
      renderAdminAlerts();
    });
  }

  const addProductBtn = document.getElementById('add-product-btn');
  if (addProductBtn) {
    addProductBtn.addEventListener('click', () => openProductModal());
  }

  const exportBtn = document.getElementById('export-excel');
  if (exportBtn) {
    exportBtn.addEventListener('click', exportToExcel);
  }

  const changePasswordBtn = document.getElementById('change-password-btn');
  if (changePasswordBtn) {
    changePasswordBtn.addEventListener('click', openPasswordModal);
  }

  const resetBtn = document.getElementById('reset-system-btn');
  if (resetBtn) {
    resetBtn.addEventListener('click', resetSystem);
  }

  const productModalClose = document.getElementById('product-modal-close');
  if (productModalClose) {
    productModalClose.addEventListener('click', () => closeModal('product-modal'));
  }

  const productImageInput = document.getElementById('product-image');
  if (productImageInput) {
    productImageInput.addEventListener('change', async event => {
      const file = event.target.files?.[0];
      const imagePreview = document.getElementById('product-image-preview');
      if (!file || !imagePreview) return;
      try {
        imagePreview.src = await readFileAsDataUrl(file);
        imagePreview.classList.remove('hidden');
      } catch (error) {
        imagePreview.classList.add('hidden');
      }
    });
  }

  const productSaveBtn = document.getElementById('product-save-btn');
  if (productSaveBtn) {
    productSaveBtn.addEventListener('click', saveProductFromModal);
  }

  const passwordModalClose = document.getElementById('password-modal-close');
  if (passwordModalClose) {
    passwordModalClose.addEventListener('click', () => closeModal('password-modal'));
  }

  const passwordSaveBtn = document.getElementById('password-save-btn');
  if (passwordSaveBtn) {
    passwordSaveBtn.addEventListener('click', savePassword);
  }

  const inventoryBody = document.getElementById('inventory-table-body');
  if (inventoryBody) {
    inventoryBody.addEventListener('click', event => {
      const button = event.target.closest('button');
      if (!button) return;
      const action = button.dataset.action;
      const productId = Number(button.dataset.productId);
      if (action === 'edit') {
        openProductModal(productId);
      } else if (action === 'remove') {
        removeProduct(productId);
      }
    });
  }

  window.addEventListener('storage', event => {
    if (event.key === STORAGE_PRODUCTS_KEY || event.key === STORAGE_SALES_KEY) {
      renderAdminCards();
      renderAdminAlerts();
      renderAdminInventory();
    }
  });
}

function initializePage() {
  setupStorePage();
  setupAdminPage();
}

// Initialize the data on every page load
initMockData();
initializePage();

window.loginUser = loginUser;
window.redirectAfterLogin = redirectAfterLogin;
window.requireAuth = requireAuth;
window.logout = logout;
window.getSession = getSession;
window.getProducts = getProducts;
window.saveProducts = saveProducts;
window.getSales = getSales;
window.saveSales = saveSales;
