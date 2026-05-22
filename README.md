# Localhost Liquid Soap Inventory System

Local single-user inventory system using Node.js, Express, and SQLite3 with a Tailwind-powered single-page frontend.

## Requirements
- Node.js 16+ (recommended)

## Setup
1. Install dependencies:

```bash
npm install
```

2. Initialize the SQLite database and seed sample data:

```bash
npm run init-db
```

3. Start the server (development):

```bash
npm run dev
```

Or run in production mode:

```bash
npm start
```

4. Open the UI in your browser:

```
http://localhost:3000
```

## API Endpoints
- `GET /api/ping` — health check
- `GET /api/products` — returns product list with `low_status` (green/yellow/red)
- `GET /api/transactions` — recent transactions
- `POST /api/products/:id/adjust` — adjust stock
  - Body JSON: `{ "type":"inflow"|"outflow", "quantity":number, "unit_price"?:number, "note"?:string }`
- `GET /api/analytics?range=daily|week|month|year` — returns `{ revenue, cogs, purchases, profit }`
- `GET /api/export?format=csv|xlsx` — download inventory+transactions report

## Frontend
- `public/index.html` provides the dashboard (Tailwind via CDN)
- `public/app.js` contains the frontend logic: loading products, showing alerts, analytics, Add/Deduct actions, and export.

## Notes & Next Steps
- The app runs locally and stores data in `inventory.db` (ignored by `.gitignore`).
- You can modify `init_db.js` to change seed data.
- If you want authentication, multi-user support, or scheduling automated exports, I can add them.

