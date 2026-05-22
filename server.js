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

// SQLite database file (created on demand)
const DBSOURCE = path.join(__dirname, 'inventory.db');
const db = new sqlite3.Database(DBSOURCE, (err) => {
  if (err) {
    console.error('Failed to connect to SQLite database:', err.message);
    process.exit(1);
  }
  console.log('Connected to SQLite database at', DBSOURCE);
});

// Simple health check
app.get('/api/ping', (req, res) => {
  res.json({ ok: true, time: new Date().toISOString() });
});

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// --- API: Products list with low-stock status
app.get('/api/products', (req, res) => {
  const sql = `SELECT * FROM products ORDER BY id`;
  db.all(sql, [], (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    const products = rows.map(p => {
      let status = 'green';
      if (p.stock <= 0) status = 'red';
      else if (p.stock <= p.min_warning) status = 'yellow';
      return { ...p, low_status: status };
    });
    res.json({ products });
  });
});

// --- API: List transactions
app.get('/api/transactions', (req, res) => {
  const sql = `SELECT t.*, p.name, p.variant, p.unit FROM transactions t JOIN products p ON p.id = t.product_id ORDER BY t.created_at DESC LIMIT 1000`;
  db.all(sql, [], (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json({ transactions: rows });
  });
});

// --- API: Adjust product stock (inflow/outflow)
app.post('/api/products/:id/adjust', (req, res) => {
  const productId = Number(req.params.id);
  const { type, quantity, unit_price, note } = req.body;
  if (!['inflow', 'outflow'].includes(type)) return res.status(400).json({ error: 'type must be inflow or outflow' });
  const qty = Number(quantity);
  if (isNaN(qty) || qty <= 0) return res.status(400).json({ error: 'quantity must be a positive number' });

  db.get(`SELECT * FROM products WHERE id = ?`, [productId], (err, product) => {
    if (err) return res.status(500).json({ error: err.message });
    if (!product) return res.status(404).json({ error: 'product not found' });

    const usedUnitPrice = (unit_price !== undefined && unit_price !== null) ? Number(unit_price) : (type === 'outflow' ? product.sale_price : product.cost_price);
    const totalAmount = usedUnitPrice * qty;
    const costTotal = (type === 'outflow') ? (product.cost_price * qty) : totalAmount;
    const newStock = (type === 'inflow') ? (product.stock + qty) : (product.stock - qty);

    // Update product stock and insert transaction atomically (serialize)
    db.serialize(() => {
      const updateStmt = `UPDATE products SET stock = ? WHERE id = ?`;
      db.run(updateStmt, [newStock, productId], function (uerr) {
        if (uerr) return res.status(500).json({ error: uerr.message });

        const insertStmt = `INSERT INTO transactions (product_id, type, quantity, unit_price, total_amount, cost_total, note) VALUES (?, ?, ?, ?, ?, ?, ?)`;
        db.run(insertStmt, [productId, type, qty, usedUnitPrice, totalAmount, costTotal, note || null], function (ierr) {
          if (ierr) return res.status(500).json({ error: ierr.message });
          res.json({ ok: true, product_id: productId, new_stock: newStock, transaction_id: this.lastID });
        });
      });
    });
  });
});

// --- API: Financial analytics
app.get('/api/analytics', (req, res) => {
  // query param ?range=daily|week|month|year ; default daily
  const range = req.query.range || 'daily';
  let where = `date(t.created_at, 'localtime') = date('now','localtime')`;
  if (range === 'week') {
    where = `date(t.created_at, 'localtime') >= date('now','-6 days','localtime')`;
  } else if (range === 'month') {
    where = `strftime('%Y-%m', t.created_at) = strftime('%Y-%m','now','localtime')`;
  } else if (range === 'year') {
    where = `strftime('%Y', t.created_at) = strftime('%Y','now','localtime')`;
  }

  const sql = `SELECT t.type, SUM(t.total_amount) as total_amount_sum, SUM(t.cost_total) as cost_total_sum FROM transactions t WHERE ${where} GROUP BY t.type`;
  db.all(sql, [], (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    // compute revenue (outflow), cogs (from outflow), purchases (inflow)
    let revenue = 0, cogs = 0, purchases = 0;
    rows.forEach(r => {
      if (r.type === 'outflow') {
        revenue = Number(r.total_amount_sum) || 0;
        cogs = Number(r.cost_total_sum) || 0;
      } else if (r.type === 'inflow') {
        purchases = Number(r.total_amount_sum) || 0;
      }
    });
    const profit = revenue - cogs;
    res.json({ range, revenue, cogs, purchases, profit });
  });
});

// --- API: Export inventory and transactions as Excel (.xlsx)
app.get('/api/export', async (req, res) => {
  // supports ?format=csv or ?format=xlsx (default xlsx)
  const format = (req.query.format || 'xlsx').toLowerCase();
  try {
    db.all(`SELECT * FROM products ORDER BY id`, [], (perr, products) => {
      if (perr) return res.status(500).json({ error: perr.message });
      db.all(`SELECT t.*, p.name as product_name FROM transactions t JOIN products p ON p.id = t.product_id ORDER BY t.created_at DESC`, [], async (terr, txs) => {
        if (terr) return res.status(500).json({ error: terr.message });

        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        if (format === 'csv') {
          // Build combined CSV: Products then blank line then Transactions
          const { Parser } = require('json2csv');
          const prodFields = ['id', 'name', 'variant', 'unit', 'stock', 'cost_price', 'sale_price', 'min_warning', 'created_at'];
          const txFields = ['id', 'product_id', 'product_name', 'type', 'quantity', 'unit_price', 'total_amount', 'cost_total', 'note', 'created_at'];
          const prodParser = new Parser({ fields: prodFields });
          const txParser = new Parser({ fields: txFields });
          const prodCsv = prodParser.parse(products || []);
          const txCsv = txParser.parse(txs || []);
          const csvCombined = prodCsv + '\n\n' + txCsv;

          const filename = `inventory_report_${timestamp}.csv`;
          res.setHeader('Content-Type', 'text/csv');
          res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
          res.send(csvCombined);
        } else {
          const ExcelJS = require('exceljs');
          const wb = new ExcelJS.Workbook();
          wb.creator = 'Localhost Inventory System';
          wb.created = new Date();
          const prodSheet = wb.addWorksheet('Products');
          prodSheet.addRow(['ID', 'Name', 'Variant', 'Unit', 'Stock', 'Cost Price', 'Sale Price', 'Min Warning', 'Created At']);
          products.forEach(p => prodSheet.addRow([p.id, p.name, p.variant, p.unit, p.stock, p.cost_price, p.sale_price, p.min_warning, p.created_at]));
          const txSheet = wb.addWorksheet('Transactions');
          txSheet.addRow(['ID', 'Product ID', 'Product Name', 'Type', 'Quantity', 'Unit Price', 'Total Amount', 'Cost Total', 'Note', 'Created At']);
          txs.forEach(t => txSheet.addRow([t.id, t.product_id, t.product_name, t.type, t.quantity, t.unit_price, t.total_amount, t.cost_total, t.note, t.created_at]));

          const filename = `inventory_report_${timestamp}.xlsx`;
          res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
          res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
          await wb.xlsx.write(res);
          res.end();
        }
      });
    });
  } catch (ex) {
    console.error('Export error', ex);
    res.status(500).json({ error: String(ex) });
  }
});
app.listen(PORT, () => {
  console.log(`Server listening on http://localhost:${PORT}`);
});

module.exports = { app, db };