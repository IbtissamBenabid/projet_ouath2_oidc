# Sequence Diagram - Order Creation Process

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User      │    │   React     │    │   Gateway   │    │Order Service│    │Product     │
│             │    │   App       │    │             │    │             │    │Service     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
      │                   │                   │                   │                   │
      │ 1. Login via      │                   │                   │                   │
      │    Keycloak       │                   │                   │                   │
      │ ─────────────────►│                   │                   │                   │
      │                   │                   │                   │                   │
      │ 2. Select products│                   │                   │                   │
      │    & place order  │                   │                   │                   │
      │                   │                   │                   │                   │
      │                   │ 3. POST /orders   │                   │                   │
      │                   │ ─────────────────►│                   │                   │
      │                   │                   │ 4. Validate JWT   │                   │
      │                   │                   │    & Route        │                   │
      │                   │                   │ ─────────────────►│                   │
      │                   │                   │                   │ 5. Validate      │
      │                   │                   │                   │    order data     │
      │                   │                   │                   │                   │
      │                   │                   │                   │ 6. Check stock   │
      │                   │                   │                   │ ─────────────────►│
      │                   │                   │                   │                   │ 7. Return stock
      │                   │                   │                   │ ◄─────────────────│
      │                   │                   │                   │                   │
      │                   │                   │                   │ 8. If stock OK:  │
      │                   │                   │                   │    - Calculate total
      │                   │                   │                   │    - Create order
      │                   │                   │                   │    - Save to DB   │
      │                   │                   │                   │                   │
      │                   │                   │                   │ 9. Reduce stock  │
      │                   │                   │                   │ ─────────────────►│
      │                   │                   │                   │                   │ 10. Stock reduced
      │                   │                   │                   │ ◄─────────────────│
      │                   │                   │                   │                   │
      │                   │                   │                   │ 11. Return order │
      │                   │                   │ ◄─────────────────│                   │
      │                   │ 12. Order created│                   │                   │
      │                   │ ◄─────────────────│                   │                   │
      │ 13. Display       │                   │                   │                   │
      │     success       │                   │                   │                   │
      │ ◄─────────────────│                   │                   │                   │
```

## Detailed Sequence Steps:

### 1. User Authentication
- User logs in via Keycloak (OAuth2/OIDC)
- Receives JWT token with roles (ADMIN/CLIENT)

### 2. Order Initiation
- User selects products in React app
- React app collects product IDs, quantities, prices
- User clicks "Order Now" button

### 3. Order Request
- React sends POST /orders to Gateway (localhost:8085)
- Includes JWT token in Authorization header
- Request body contains order data with productItems[]

### 4. Gateway Processing
- Gateway validates JWT token
- Checks user roles and permissions
- Routes request to Order Service (localhost:8082)

### 5. Order Validation
- Order Service validates order data
- Checks required fields (productItems, quantities, prices)
- Validates business rules

### 6. Stock Verification
- For each product in order:
- Order Service calls Product Service GET /products/{id}/stock?quantity=X
- Includes JWT token for authentication
- Product Service checks if requested quantity <= available stock

### 7. Stock Response
- Product Service returns boolean: true (stock available) or false

### 8. Order Processing (if stock OK)
- Calculate total amount: sum(price × quantity for all items)
- Set order properties: date, status="PENDING", userId, amount
- Save order to order_db

### 9. Stock Reduction
- For each product in order:
- Order Service calls Product Service PUT /products/{id}/reduce-stock?quantity=X
- Product Service reduces stock quantity in product_db

### 10. Stock Reduction Confirmation
- Product Service confirms stock reduction

### 11. Order Response
- Order Service returns created order with ID

### 12. Gateway Response
- Gateway forwards order response to React

### 13. UI Update
- React displays success message
- Refreshes product list (to show updated stock)
- Refreshes user's order list

## Error Handling:
- **401 Unauthorized**: Invalid/expired JWT → Redirect to login
- **403 Forbidden**: Insufficient permissions → Show error message
- **400 Bad Request**: Invalid order data → Show validation errors
- **Stock Insufficient**: Custom error → Show "Out of stock" message
- **Network Error**: Generic error → Show "Failed to place order"

## Security Considerations:
- All requests include JWT token
- Role-based access control at each layer
- Input validation on all services
- Secure inter-service communication
- Audit logging for all operations