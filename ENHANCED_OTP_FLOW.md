# Enhanced OTP Flow with merchantId=0

## Overview
The OTP system now supports generating OTP by mobile number when merchantId=0, allowing more flexible OTP generation for any user type.

## Key Changes Made

### 1. Updated User Lookup Logic
- **Before**: merchantId=0 only looked for admin/merchant users
- **After**: merchantId=0 first tries admin/merchant users, then falls back to any user with the phone number

### 2. Relaxed User Role Validation
- **Before**: Strict role validation for merchantId=0 (only super_admin/merchant)
- **After**: Allows OTP generation for any user type when merchantId=0

### 3. Default Values
- **OtpRequest**: merchantId defaults to 0
- **OtpVerifyRequest**: merchantId defaults to 0
- **OtpRequest**: otpType defaults to "login"

## API Usage Examples

### 1. Generate OTP by Phone Number (Any User)
```json
POST /api/auth/otp/request
{
  "phone": "9876543210",
  "merchantId": 0,
  "otpType": "login"
}
```

### 2. Generate OTP with Minimal Payload
```json
POST /api/auth/otp/request
{
  "phone": "9876543210"
}
```
*Note: merchantId and otpType will default to 0 and "login" respectively*

### 3. Verify OTP by Phone Number
```json
POST /api/auth/otp/verify
{
  "phone": "9876543210",
  "otp": "1234",
  "merchantId": 0
}
```

### 4. Verify OTP with Minimal Payload
```json
POST /api/auth/otp/verify
{
  "phone": "9876543210",
  "otp": "1234"
}
```

## Flow Logic

### OTP Request Flow (merchantId=0)
1. **Input Validation**: Validate phone number format
2. **User Lookup**: 
   - First: Try to find admin/merchant user (merchant_id IS NULL)
   - Fallback: Find any user with the phone number
3. **Role Validation**: Allow OTP generation for any user type
4. **Rate Limiting**: Check OTP request limits (3 requests per 30 minutes)
5. **OTP Generation**: Generate 4-digit OTP
6. **SMS Sending**: Send OTP via SMS service
7. **Audit Logging**: Log OTP request for security tracking

### OTP Verification Flow (merchantId=0)
1. **User Lookup**: Same logic as OTP request
2. **OTP Validation**: Check OTP code, expiry, and attempt limits
3. **Success Handling**: Clear OTP data and generate JWT tokens
4. **Token Generation**: Create access and refresh tokens

## Benefits

### 1. Flexibility
- Can generate OTP for any user regardless of their type
- Simplified API calls with default values
- Works for customers, merchants, and admin users

### 2. Backward Compatibility
- Existing flows with specific merchantId still work
- No breaking changes to current API contracts

### 3. Enhanced User Experience
- Users don't need to know their merchant association
- Single endpoint for all OTP requests
- Automatic fallback logic

## Security Considerations

### 1. Rate Limiting
- 3 OTP requests per 30 minutes per phone number
- Prevents abuse and spam

### 2. OTP Expiry
- 5-minute expiry for all OTP types
- Automatic cleanup of expired OTPs

### 3. Attempt Limits
- Maximum 3 verification attempts per OTP
- Account blocking after excessive failures

### 4. Audit Logging
- Complete audit trail for all OTP operations
- Security monitoring and compliance

## Testing Scenarios

### Scenario 1: Customer OTP with merchantId=0
```bash
# Request OTP
curl -X POST http://localhost:8081/api/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543210"}'

# Verify OTP
curl -X POST http://localhost:8081/api/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543210", "otp": "1234"}'
```

### Scenario 2: Merchant OTP with merchantId=0
```bash
# Request OTP for merchant user
curl -X POST http://localhost:8081/api/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543211", "merchantId": 0, "otpType": "password_reset"}'
```

### Scenario 3: Specific Merchant Customer
```bash
# Request OTP for specific merchant customer
curl -X POST http://localhost:8081/api/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543212", "merchantId": 1, "otpType": "login"}'
```

## Implementation Details

### Modified Methods
1. **findUserByPhoneAndMerchantId()**: Enhanced user lookup with fallback
2. **validateUserRoleForOtp()**: Relaxed validation for merchantId=0
3. **AuthController.requestOtp()**: Added default value handling
4. **AuthController.verifyOtp()**: Added default value handling

### Configuration Updates
- **OtpRequest.merchantId**: Default value = 0
- **OtpRequest.otpType**: Default value = "login"
- **OtpVerifyRequest.merchantId**: Default value = 0

## Error Handling

### Common Error Responses
```json
// User not found
{
  "status": 404,
  "message": "No user found with phone: ****3210"
}

// Rate limit exceeded
{
  "status": 429,
  "message": "Too many OTP requests. You have exceeded the limit of 3 OTP requests. Please try again after 30 minutes."
}

// Invalid OTP
{
  "status": 400,
  "message": "Invalid or expired OTP"
}
```

## Conclusion

The enhanced OTP flow provides greater flexibility while maintaining security and backward compatibility. Users can now generate OTPs using just their phone number, making the system more user-friendly while preserving all existing functionality.