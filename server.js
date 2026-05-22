const express = require('express');
const cors = require('cors');
const path = require('path');
const sqlite3 = require('sqlite3').verbose();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// ── Database ───────────────────────────────────────────────────
const DB_PATH = path.join(__dirname, 'soap.db');
const db = new sqlite3.Database(DB_PATH, (err) => {
  if (err) { console.error('DB open failed:', err.message); process.exit(1); }
  console.log('Connected to', DB_PATH);
});

// Promisified helpers
const dbRun = (sql, params = []) => new Promise((resolve, reject) => {
  db.run(sql, params, function (err) { err ? reject(err) : resolve(this); });
});
const dbGet = (sql, params = []) => new Promise((resolve, reject) => {
  db.get(sql, params, (err, row) => { err ? reject(err) : resolve(row); });
});
const dbAll = (sql, params = []) => new Promise((resolve, reject) => {
  db.all(sql, params, (err, rows) => { err ? reject(err) : resolve(rows); });
});

// Init schema
db.serialize(() => {
  db.run('PRAGMA foreign_keys = ON');
  db.run(`
    CREATE TABLE IF NOT EXISTS products (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      variant TEXT NOT NULL DEFAULT '',
      unit TEXT NOT NULL DEFAULT 'pcs',
      stock REAL NOT NULL DEFAULT 0,
      cost_price REAL NOT NULL DEFAULT 0,
      sale_price REAL NOT NULL DEFAULT 0,
      min_warning REAL NOT NULL DEFAULT 0,
      barcode TEXT DEFAULT '',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
  db.run(`
    CREATE TABLE IF NOT EXISTS sales (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      total_amount REAL NOT NULL DEFAULT 0,
      cash_tendered REAL NOT NULL DEFAULT 0,
      change_amount REAL NOT NULL DEFAULT 0,
      item_count INTEGER NOT NULL DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
  db.run(`
    CREATE TABLE IF NOT EXISTS sale_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      sale_id INTEGER NOT NULL,
      product_id INTEGER NOT NULL,
      product_name TEXT NOT NULL,
      product_variant TEXT NOT NULL DEFAULT '',
      quantity REAL NOT NULL,
      unit_price REAL NOT NULL,
      subtotal REAL NOT NULL DEFAULT 0,
      FOREIGN KEY(sale_id) REFERENCES sales(id) ON DELETE CASCADE,
      FOREIGN KEY(product_id) REFERENCES products(id)
    )
  `);

  // Seed if empty
  db.get('SELECT COUNT(*) as cnt FROM products', (err, row) => {
    if (err || (row && row.cnt > 0)) return;
    const stmt = db.prepare(`
      INSERT INTO products (name, variant, unit, stock, cost_price, sale_price, min_warning, barcode)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);
    stmt.run('Liquid Soap', 'Bulk 20L Drum', 'liters', 50, 420, 650, 10, '1000001');
    stmt.run('Liquid Soap', 'Bottle 500ml', 'pcs', 180, 28, 45, 20, '1000002');
    stmt.run('Liquid Soap', 'Pouch 250ml', 'pcs', 300, 18, 28, 30, '1000003');
    stmt.finalize(() => console.log('Seeded default products'));
  });
});

// ── Products API ───────────────────────────────────────────────

app.get('/api/products', async (req, res) => {
  try {
    const { q } = req.query;
    let rows;
    if (q && q.trim()) {
      const s = `%${q.trim()}%`;
      rows = await dbAll(
        `SELECT * FROM products WHERE name LIKE ? OR variant LIKE ? OR barcode LIKE ? ORDER BY name, variant`,
        [s, s, s]
      );
    } else {
      rows = await dbAll('SELECT * FROM products ORDER BY name, variant');
    }
    res.json({ products: rows });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/products/:id', async (req, res) => {
  try {
    const row = await dbGet('SELECT * FROM products WHERE id = ?', [req.params.id]);
    if (!row) return res.status(404).json({ error: 'Product not found' });
    res.json({ product: row });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/products', async (req, res) => {
  try {
    const { name, variant, unit, stock, cost_price, sale_price, min_warning, barcode } = req.body;
    if (!name) return res.status(400).json({ error: 'name is required' });
    const result = await dbRun(
      `INSERT INTO products (name, variant, unit, stock, cost_price, sale_price, min_warning, barcode)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [name, variant || '', unit || 'pcs', Number(stock) || 0, Number(cost_price) || 0,
       Number(sale_price) || 0, Number(min_warning) || 0, barcode || '']
    );
    const product = await dbGet('SELECT * FROM products WHERE id = ?', [result.lastID]);
    res.json({ ok: true, product });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/products/:id', async (req, res) => {
  try {
    const existing = await dbGet('SELECT * FROM products WHERE id = ?', [req.params.id]);
    if (!existing) return res.status(404).json({ error: 'Product not found' });
    const { name, variant, unit, stock, cost_price, sale_price, min_warning, barcode } = req.body;
    await dbRun(
      `UPDATE products SET name=?, variant=?, unit=?, stock=?, cost_price=?, sale_price=?, min_warning=?, barcode=? WHERE id=?`,
      [
        name ?? existing.name,
        variant ?? existing.variant,
        unit ?? existing.unit,
        stock !== undefined ? Number(stock) : existing.stock,
        cost_price !== undefined ? Number(cost_price) : existing.cost_price,
        sale_price !== undefined ? Number(sale_price) : existing.sale_price,
        min_warning !== undefined ? Number(min_warning) : existing.min_warning,
        barcode ?? existing.barcode,
        req.params.id,
      ]
    );
    const updated = await dbGet('SELECT * FROM products WHERE id = ?', [req.params.id]);
    res.json({ ok: true, product: updated });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/products/:id', async (req, res) => {
  try {
    const existing = await dbGet('SELECT * FROM products WHERE id = ?', [req.params.id]);
    if (!existing) return res.status(404).json({ error: 'Product not found' });
    await dbRun('DELETE FROM products WHERE id = ?', [req.params.id]);
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Sales API ──────────────────────────────────────────────────

app.post('/api/sales', async (req, res) => {
  try {
    const { items, cash_tendered } = req.body;
    if (!items || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ error: 'items array is required' });
    }
    const cash = Number(cash_tendered) || 0;
    let totalAmount = 0;
    let itemCount = 0;

    // Validate all items
    const resolved = [];
    for (const item of items) {
      const product = await dbGet('SELECT * FROM products WHERE id = ?', [item.product_id]);
      if (!product) return res.status(400).json({ error: `Product ${item.product_id} not found` });
      const qty = Number(item.quantity) || 1;
      if (qty <= 0) return res.status(400).json({ error: 'Quantity must be positive' });
      if (product.stock < qty) {
        return res.status(400).json({
          error: `Insufficient stock for ${product.name} ${product.variant} (have ${product.stock}, need ${qty})`
        });
      }
      const unitPrice = product.sale_price;
      const subtotal = unitPrice * qty;
      totalAmount += subtotal;
      itemCount += qty;
      resolved.push({ product, qty, unitPrice, subtotal });
    }

    if (cash < totalAmount) {
      return res.status(400).json({ error: 'Insufficient cash tendered' });
    }
    const changeAmount = cash - totalAmount;

    // Insert sale
    const saleResult = await dbRun(
      `INSERT INTO sales (total_amount, cash_tendered, change_amount, item_count) VALUES (?, ?, ?, ?)`,
      [totalAmount, cash, changeAmount, itemCount]
    );
    const saleId = saleResult.lastID;

    // Insert items + deduct stock
    for (const r of resolved) {
      await dbRun(
        `INSERT INTO sale_items (sale_id, product_id, product_name, product_variant, quantity, unit_price, subtotal)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [saleId, r.product.id, r.product.name, r.product.variant, r.qty, r.unitPrice, r.subtotal]
      );
      await dbRun('UPDATE products SET stock = stock - ? WHERE id = ?', [r.qty, r.product.id]);
    }

    const sale = await dbGet('SELECT * FROM sales WHERE id = ?', [saleId]);
    const saleItems = await dbAll('SELECT * FROM sale_items WHERE sale_id = ?', [saleId]);
    res.json({ ok: true, sale, items: saleItems });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/sales', async (_req, res) => {
  try {
    const sales = await dbAll('SELECT * FROM sales ORDER BY created_at DESC LIMIT 500');
    res.json({ sales });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/sales/:id', async (req, res) => {
  try {
    const sale = await dbGet('SELECT * FROM sales WHERE id = ?', [req.params.id]);
    if (!sale) return res.status(404).json({ error: 'Sale not found' });
    const items = await dbAll('SELECT * FROM sale_items WHERE sale_id = ?', [req.params.id]);
    res.json({ sale, items });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Fallback ───────────────────────────────────────────────────
app.get('*', (_req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Soap POS running on http://localhost:${PORT}`);
});

module.exports = { app, db };

/* @julesssssssswrld */