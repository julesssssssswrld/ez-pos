const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const DBSOURCE = path.join(__dirname, 'inventory.db');
const db = new sqlite3.Database(DBSOURCE, (err) => {
  if (err) {
    console.error('Cannot open database', err.message);
    process.exit(1);
  }
  console.log('Opened database at', DBSOURCE);
});

db.serialize(() => {
  // Create products table
  db.run(`
    CREATE TABLE IF NOT EXISTS products (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      variant TEXT NOT NULL,
      unit TEXT NOT NULL,
      stock REAL NOT NULL DEFAULT 0,
      cost_price REAL NOT NULL DEFAULT 0,
      sale_price REAL NOT NULL DEFAULT 0,
      min_warning REAL NOT NULL DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );
  `, (err) => { if (err) console.error('Create products table error:', err.message); else console.log('products table OK'); });

  // Create transactions table
  db.run(`
    CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      product_id INTEGER NOT NULL,
      type TEXT NOT NULL CHECK(type IN ('inflow','outflow')),
      quantity REAL NOT NULL,
      unit_price REAL NOT NULL DEFAULT 0,
      total_amount REAL NOT NULL DEFAULT 0,
      cost_total REAL NOT NULL DEFAULT 0,
      note TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(product_id) REFERENCES products(id)
    );
  `, (err) => { if (err) console.error('Create transactions table error:', err.message); else console.log('transactions table OK'); });

  // Seed default products if table empty
  db.get(`SELECT COUNT(*) as cnt FROM products`, (err, row) => {
    if (err) {
      console.error('Count products failed:', err.message);
      return;
    }
    if (row && row.cnt === 0) {
      const insert = db.prepare(`INSERT INTO products (name, variant, unit, stock, cost_price, sale_price, min_warning) VALUES (?, ?, ?, ?, ?, ?, ?)`);

      insert.run('Liquid Soap', 'Bulk', 'liters', 50.0, 50.0, 75.0, 10.0); // 50L bulk
      insert.run('Liquid Soap', 'Bottle (500ml)', 'pcs', 200, 5.0, 8.0, 20); // 200 bottles
      insert.run('Liquid Soap', 'Pouch (250ml)', 'pcs', 300, 2.0, 3.5, 30); // 300 pouches

      insert.finalize(err => { if (err) console.error('Seed products error:', err.message); else console.log('Seeded default products'); });
    } else {
      console.log('Products table already seeded (count =', row.cnt, ')');
    }
  });

});

db.close((err) => {
  if (err) console.error('Error closing database', err.message);
  else console.log('Database connection closed');
});
