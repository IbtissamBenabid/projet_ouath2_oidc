# Architecture Diagram - Microservices E-commerce Application

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           User Browser (React)                              │
│                           http://localhost:3000                             │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                                      │ HTTP/HTTPS
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (Spring Cloud Gateway)               │
│                           Port: 8085                                       │
│                           - JWT Token Validation                           │
│                           - Route to microservices                         │
│                           - CORS Configuration                             │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
┌─────────────────────┐ ┌──────────────┬─────────────┐ ┌─────────────────────┐
│   Product Service   │ │ Order Service│             │ │     Keycloak        │
│   Port: 8081        │ │ Port: 8082   │             │ │   Port: 8088        │
│   - CRUD Products   │ │ - CRUD Orders│             │ │ - OAuth2/OIDC       │
│   - Stock Management│ │ - Order Calc │             │ │ - JWT Tokens        │
│   - JWT Security    │ │ - JWT Security│            │ │ - User Management   │
└─────────────────────┘ └──────────────┼─────────────┘ └─────────────────────┘
                    │                 │                 │
                    ▼                 ▼                 ▼
┌─────────────────────┐ ┌──────────────┬─────────────┐ ┌─────────────────────┐
│   PostgreSQL        │ │ PostgreSQL   │             │ │   PostgreSQL        │
│   product_db        │ │ order_db     │             │ │   keycloak_db       │
│   Port: 5432        │ │ Port: 5432   │             │ │   Port: 5432        │
└─────────────────────┘ └──────────────┴─────────────┘ └─────────────────────┘

Communication Flow:
1. User logs in via Keycloak → JWT Token
2. React app sends requests to Gateway (port 8085)
3. Gateway validates JWT and routes to appropriate service
4. Services communicate with each other via REST (Order → Product for stock check)
5. All services use separate PostgreSQL databases
```

## Security Layers

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Security Architecture                             │
├─────────────────────────────────────────────────────────────────────────────┤
│   Frontend (React)                                                         │
│   - Keycloak JS Adapter                                                    │
│   - JWT Token Storage                                                      │
│   - Role-based UI (ADMIN/CLIENT)                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│   API Gateway                                                              │
│   - JWT Token Validation                                                   │
│   - Route-level Authorization                                              │
│   - CORS Configuration                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│   Microservices                                                            │
│   - Method-level Security (@PreAuthorize)                                  │
│   - JWT Authentication                                                     │
│   - Role-based Access Control                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│   Database                                                                 │
│   - Separate databases per service                                         │
│   - Secure credentials                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Data Flow

```
User Action: Place Order
1. User selects products → React
2. React sends POST /orders to Gateway
3. Gateway validates JWT → Routes to Order Service
4. Order Service:
   - Validates order data
   - Calls Product Service to check stock (GET /products/{id}/stock)
   - If stock OK: Creates order, updates stock (PUT /products/{id}/reduce-stock)
   - Returns order confirmation
5. React displays success/error message
```