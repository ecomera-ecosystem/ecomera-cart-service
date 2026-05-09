# Ecomera Cart Service

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.1-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![OpenFeign](https://img.shields.io/badge/OpenFeign-Integrated-6DB33F)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?logo=open-source-initiative&logoColor=white)

Shopping cart microservice for the Ecomera ecosystem. Manages user carts with Redis caching, scheduled expiration cleanup, and product data synchronization via OpenFeign.

---

## Overview

Provides a full shopping cart API for authenticated users. Carts are stored in PostgreSQL with Redis caching for fast access. Product data (title, price, stock, image) is fetched live from the **Product Service** via OpenFeign — the client only sends `productId` and `quantity`. Carts auto-expire after 7 days with a scheduled cleanup.

---

## Tech Stack

- **Spring Boot** 3.5.11
- **Spring Data JPA** - Database persistence
- **Spring Cloud OpenFeign** - Inter-service communication
- **PostgreSQL** - Cart data storage
- **Redis** - Distributed caching
- **Liquibase** - Database migrations
- **MapStruct** - DTO mapping
- **Spring Cloud Config** - Centralized configuration
- **Eureka Client** - Service registration
- **Springdoc OpenAPI** - API documentation

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 16+ (database: `ecomera_cart`)
- Redis 7+
- Config Server running on port 8888
- Eureka Server running on port 8761

### Start the Service
```bash
mvn spring-boot:run
```

**Service available at:** `http://localhost:8083`

---

## API Endpoints

### User Endpoints (requires JWT — gateway injects `X-User-Id`)

| Method | Endpoint | Description | Body |
|--------|----------|-------------|------|
| POST | `/api/v1/cart/items` | Add item to cart | `{ "productId": "uuid", "quantity": 2 }` |
| GET | `/api/v1/cart` | Get full cart with item details | — |
| GET | `/api/v1/cart/summary` | Get lightweight totals (no items) | — |
| PATCH | `/api/v1/cart/items/{itemId}` | Update item quantity | `{ "quantity": 3 }` |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove item from cart | — |
| DELETE | `/api/v1/cart` | Clear entire cart | — |

### Admin/Manager Endpoints (requires `ADMIN` or `MANAGER` role in JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/cart/{cartId}` | Get any cart by its UUID |
| GET | `/api/v1/cart/user/{userId}` | Get any user's cart by user UUID |

### Health & Docs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | OpenAPI documentation |

---

## Database Schema

```
cart
├── id (UUID, PK)
├── user_id (UUID, UNIQUE, NOT NULL)
├── expires_at (TIMESTAMP)
├── created_at
├── created_by
├── updated_at
└── updated_by

cart_item
├── id (UUID, PK)
├── cart_id (UUID, FK → cart.id)
├── product_id (UUID, NOT NULL)
├── product_title (VARCHAR, NOT NULL)
├── product_image (VARCHAR)
├── unit_price (DECIMAL, NOT NULL)
├── quantity (INTEGER, NOT NULL)
├── available_stock (INTEGER, NOT NULL)
├── created_at
├── created_by
├── updated_at
└── updated_by
```

---

## Architecture

```
Client → API Gateway (port 8080)
              ↓
    Cart Service (port 8083)
         ↓           ↓
    PostgreSQL    Redis Cache
         ↓
    Product Service (via Feign)
         ↓
    Config Server (configs)
         ↓
    Eureka Server (registration)
```

When a user adds an item, the cart service:
1. Receives `{ productId, quantity }` from the client
2. Calls **Product Service** via Feign to fetch current product data
3. Stores a **snapshot** (title, price, stock, image) in the cart item
4. Validates requested quantity against live stock

---

## Features

- **Add to Cart** — Client sends productId + quantity; title, price, stock, image fetched live from Product Service via OpenFeign
- **Update Quantity** — Change item quantity with stock validation
- **Remove Items** — Remove individual items from the cart
- **Clear Cart** — Empty the entire cart
- **Full Cart (GET)** — Returns all items with subtotals
- **Cart Summary** — Lightweight endpoint returning only totals (no item details) for header badges
- **Admin Lookup** — Admins/Managers can inspect any user's cart by cart ID or user ID
- **Redis Caching** — Cart data cached with 15-min TTL; cart summary cached with 10-min TTL
- **Cart Expiration** — Carts auto-expire after 7 days
- **Scheduled Cleanup** — Expired carts purged hourly via `@Scheduled`
- **Liquibase Migrations** — Version-controlled database schema
- **Audit Fields** — Automatic created/updated timestamps

---

## Configuration

Configuration fetched from **Config Server** (`cart-service.yml`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecomera_cart
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8083

cart:
  cleanup:
    cron: "0 0 * * * *"   # hourly
```

---

## Docker Support

### Build Image
```bash
docker build -t ecomera-cart-service .
```

### Run Container
```bash
docker run -p 8083:8083 \
  -e CONFIG_SERVER_URL=http://config-server:8888 \
  -e EUREKA_SERVER_URL=http://eureka:8761/eureka/ \
  ecomera-cart-service
```

---

## Testing

```bash
# Unit tests
mvn test
```

---

## Related Services

**Infrastructure:**
- [Config Server](https://github.com/ecomera-ecosystem/ecomera-config-server) - Centralized configuration
- [Eureka Server](https://github.com/ecomera-ecosystem/ecomera-eureka-service-registry) - Service discovery
- [API Gateway](https://github.com/ecomera-ecosystem/ecomera-api-gateway) - Entry point

**Business Services:**
- [Auth Service](https://github.com/ecomera-ecosystem/ecomera-auth-service) - Authentication & authorization
- [Product Service](https://github.com/ecomera-ecosystem/ecomera-product-service) - Product catalog (cart fetches data via Feign)

---

## License

MIT License — see [LICENSE](LICENSE) file for details

---

**Status:** Active Development
