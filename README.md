<div align="center">

# 🛒 Zaki Supermarket — Backend API

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)

</div>

---

## 📖 Project Overview

**Zaki Supermarket** is a retail management system forked from [SmartPharma](https://github.com/amer-rouby/smartpharma-backend), reusing its proven multi-tenant retail engine (auth, inventory, POS, purchasing, payments, reporting, notifications) as a starting point for a **supermarket** business instead of a pharmacy.

## ⚠️ Fork status — read before assuming anything works as a supermarket

This repository is a **direct file copy** of the SmartPharma backend, with only project identity changed so far (Maven coordinates, app name, port, database). It is **not yet adapted to supermarket business rules**:

- The Java package is still `com.smartpharma.*` — not yet renamed.
- Domain logic is still pharmacy-shaped: prescription-required products, controlled-substance flags, drug-specific demand-prediction seasonality (antibiotics/painkillers/allergy-season factors), pharmacy-specific category seed data, etc.
- None of this has been reviewed or rewritten for a supermarket's actual domain (perishables without prescriptions, FMCG categories, different seasonality, etc.).

**Runs independently of SmartPharma** — different Maven artifact, different port (`8082` vs `8081`), different database (`zaki_supermarket` vs `smartpharma`) — so both can run side by side on the same machine without conflict while the supermarket-specific rework happens.

## 🛠 Tech Stack

Same as SmartPharma — Spring Boot 3.2.0, Java 17, PostgreSQL/Hibernate, Spring Security + JWT, Maven. See the [SmartPharma backend README](https://github.com/amer-rouby/smartpharma-backend) for full architecture and module details until this one gets its own pass.

## 🚀 Running locally

```bash
# create the database once
psql -U root -c "CREATE DATABASE zaki_supermarket;"

# required env vars
export DB_URL=jdbc:postgresql://localhost:5432/zaki_supermarket
export DB_USERNAME=root
export DB_PASSWORD=root
export JWT_SECRET=$(openssl rand -base64 64)

mvn spring-boot:run
```
API starts on `http://localhost:8082/api`.

## 🔗 Related Repositories

- **Frontend**: [zaki-supermarket-frontend](https://github.com/amer-rouby/zaki-supermarket-frontend)
- **Mobile**: [zaki-supermarket-mobile](https://github.com/amer-rouby/zaki-supermarket-mobile)
- **Forked from**: [smartpharma-backend](https://github.com/amer-rouby/smartpharma-backend)

## 📄 License

This project is proprietary and protected by intellectual property rights.
