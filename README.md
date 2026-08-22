<div align="center">

# 🛒 Zaki Supermarket — Backend API

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)

</div>

---

## 📖 Project Overview

**Zaki Supermarket** is a retail management system originally forked from [SmartPharma](https://github.com/amer-rouby/smartpharma-backend), reusing its multi-tenant retail engine (auth, inventory, POS, purchasing, payments, reporting, notifications) as the base for a **supermarket** business instead of a pharmacy. The Java package (`com.zakisupermarket.*`) and database (`zaki_supermarket`) have their own identity, and a dedicated set of "Zaki AI" features (below) has since been built on top of that base specifically for supermarket operations.

**Runs independently of SmartPharma** — different Maven artifact, different port (`8082` vs `8081`), different database — so both can run side by side on the same machine.

## 🤖 Zaki AI Features

Every feature below is real, rule-based/statistical logic over the store's own data — **no paid AI/ML API is used anywhere**, and no feature auto-creates orders, auto-changes prices, or auto-accuses a user. Each one is toggled per store from **Settings → Zaki Features** (`zaki_feature_settings` table) and every endpoint re-checks its own flag server-side (fails closed with `403` if disabled), not just in the UI.

| # | Feature | What it does |
|---|---|---|
| 1 | Smart Inventory Prediction | Stockout date + risk level (`LOW`/`MEDIUM`/`HIGH`/`CRITICAL`) on top of the existing demand-forecast engine |
| 2 | Smart Reorder Recommendations | Recommended reorder quantity per product, sorted by risk — review only, never auto-orders |
| 3 | Smart Pricing / Expiry Recommendations | Suggested discount % for near-expiry or slow-moving stock — suggestion only, never changes `Product.sellPrice` |
| 4 | Supplier Order Recommendations | Reorder recommendations grouped by supplier |
| 5 | Zaki Owner Dashboard Insights | Aggregated sales/risk/expiry/anomaly counters on the owner dashboard |
| 6 | Zaki Daily Brief | One-screen daily summary composed from the same real data as the insights above |
| 7 | Unusual Activity Detection | Flags excessive refunds, unusual discounts, frequent cancellations, repeated stock adjustments — always "unusual activity", never "fraud"; never auto-blocks anyone |
| 8 | Real-Time Inventory Updates | Stock screens patch live via the existing SSE stream instead of requiring a manual refresh |
| 9 | Voice Product Search | Browser-native speech recognition feeds the existing product search — no key, no server component |
| 10 | Customer Credit / Debt Management | `Customer` + append-only ledger, credit-limit enforcement on credit sales |
| 11 | Zaki Assistant | Pattern-matched Q&A over real backend data (top sellers, stockout risk, near-expiry, sales vs average, reorder recommendations) — never fabricates an answer |
| 12 | Egyptian E-Invoice (ETA) integration | Structural layer only until real credentials are supplied — see [`docs/eta-integration.md`](docs/eta-integration.md) |
| 13 | Offline-First POS | Service worker + IndexedDB sale queue; syncs on reconnect; conflicts surface for manual review, never silently overwritten |

## 🛠 Tech Stack

Spring Boot 3.2.0, Java 17, PostgreSQL/Hibernate (`ddl-auto=update`, Flyway disabled), Spring Security + JWT, Maven.

## 🚀 Running locally

```bash
# create the database once
psql -U root -c "CREATE DATABASE zaki_supermarket;"

# required
export DB_URL=jdbc:postgresql://localhost:5432/zaki_supermarket
export DB_USERNAME=root
export DB_PASSWORD=root
export JWT_SECRET=$(openssl rand -base64 64)

mvn spring-boot:run
```
API starts on `http://localhost:8082/api`.

### Optional environment variables

None of these have a real default in production — each fails closed (feature disabled / send rejected) with a clear error rather than silently no-op'ing or faking success:

| Variable | Purpose |
|---|---|
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins (has a dev-friendly default) |
| `PLATFORM_ADMIN_API_KEY` | `X-Platform-Admin-Key` for whole-database backup/restore endpoints |
| `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_ACCESS_TOKEN` | WhatsApp Cloud API, for supplier/customer notifications |
| `ETA_CLIENT_ID`, `ETA_CLIENT_SECRET`, `ETA_ENVIRONMENT` | Egyptian e-invoice (ETA) — see [`docs/eta-integration.md`](docs/eta-integration.md) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Outgoing SMTP for supplier-facing emails |

A `dev` Spring profile (`-Dspring-boot.run.profiles=dev`) exists with fallback defaults for local development — always override `DB_URL`/`JWT_SECRET` with your own real values rather than relying on it for anything containing real data.

## ⚠️ Known carry-over from the SmartPharma fork

Some naming still reflects the pharmacy origin and hasn't been renamed, since it doesn't affect supermarket behavior: the `PHARMACIST` role is the de facto cashier/staff role, and a handful of internal identifiers/log messages still say "SmartPharma" or "pharmacy" in places that were never business-critical to rename.

## 🔗 Related Repositories

- **Frontend**: [zaki-supermarket-frontend](https://github.com/amer-rouby/zaki-supermarket-frontend)
- **Mobile**: not published yet
- **Forked from**: [smartpharma-backend](https://github.com/amer-rouby/smartpharma-backend)

## 📄 License

This project is proprietary and protected by intellectual property rights.
