# Cloud Kitchen RBAC - Simple API Access Guide

## User Types & Access

- **Super Admin**: Full access to everything
- **Merchant**: Access to own merchant data + own customers  
- **Customer**: Access to own profile only

## Authentication

### Login
```bash
POST /api/auth/login
{
  "username": "your_username",
  "password": "your_password", 
  "merchantId": 0  // Use 0 for admin/merchant, actual ID for customer
}
```

### Use Token
```bash
Authorization: Bearer <your_token>
```

## API Endpoints

### Merchant APIs

#### Get Own Merchant Data
```bash
GET /api/merchants/{merchant_id}
Authorization: Bearer <token>
```
- ✅ **Super Admin**: Any merchant
- ✅ **Merchant**: Own merchant only
- ❌ **Customer**: No access

#### Get All Merchants  
```bash
GET /api/merchants
Authorization: Bearer <token>
```
- ✅ **Super Admin**: All merchants
- ❌ **Merchant**: No access
- ❌ **Customer**: No access

### Customer APIs

#### Get All Customers
```bash
GET /api/customers
Authorization: Bearer <token>
```
- ✅ **Super Admin**: All customers
- ✅ **Merchant**: Own customers only
- ❌ **Customer**: No access

#### Get Customer by ID
```bash
GET /api/customers/{customer_id}
Authorization: Bearer <token>
```
- ✅ **Super Admin**: Any customer
- ✅ **Merchant**: Own customers only
- ✅ **Customer**: Own profile only

#### Get Customers by Merchant
```bash
GET /api/customers/merchant/{merchant_id}
Authorization: Bearer <token>
```
- ✅ **Super Admin**: Any merchant's customers
- ✅ **Merchant**: Own customers only
- ❌ **Customer**: No access

## Examples

### Super Admin
```bash
# Login
POST /api/auth/login
{"username": "admin", "password": "Admin@123", "merchantId": 0}

# Access everything
GET /api/merchants        # All merchants
GET /api/merchants/1      # Any merchant
GET /api/customers        # All customers  
GET /api/customers/5      # Any customer
```

### Merchant Admin
```bash
# Login  
POST /api/auth/login
{"username": "merchant_user", "password": "password", "merchantId": 0}

# Access own data
GET /api/merchants/3      # Own merchant (ID 3)
GET /api/customers        # Own customers only
GET /api/customers/10     # Own customer (ID 10)
```

### Customer
```bash
# Login
POST /api/auth/login  
{"username": "9876543210", "password": "password", "merchantId": 3}

# Access own profile
GET /api/customers/25     # Own profile (ID 25)
```

## Error Responses

- **401**: No token or invalid token
- **403**: Access denied (wrong permissions)
- **404**: Resource not found

## Notes

- Tokens expire after 7 days
- Use HTTPS in production
- Swagger docs: `http://localhost:8081/swagger-ui.html`