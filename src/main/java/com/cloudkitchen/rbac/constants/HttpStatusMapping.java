package com.cloudkitchen.rbac.constants;

/**
 * HTTP Status Code Mapping for Error Codes
 * 
 * 400 Series - Client Error (Bad Request)
 * These errors indicate that the client has made an error in the request
 */
public class HttpStatusMapping {
    
    /**
     * 400 BAD REQUEST - Client Error Series
     * 
     * Error Code -> HTTP Status -> Reason
     * 
     * ERR_1001 -> 400 -> Validation failed - Input data doesn't meet requirements
     * ERR_1002 -> 400 -> Invalid phone format - Phone number format is incorrect  
     * ERR_1003 -> 400 -> Invalid password format - Password doesn't meet security requirements
     * ERR_1004 -> 400 -> Invalid OTP format - OTP format is incorrect
     * ERR_1005 -> 400 -> Missing required field - Required field not provided
     * ERR_1006 -> 400 -> Invalid merchant ID - Merchant ID is invalid or doesn't exist
     * ERR_1007 -> 400 -> Invalid name format - Name contains invalid characters
     * ERR_1008 -> 400 -> Invalid email format - Email format is incorrect
     * 
     * ERR_4001 -> 400 -> User already exists - Phone number already registered (duplicate data)
     * ERR_4005 -> 400 -> Phone already registered - Phone number is already in use
     * ERR_4006 -> 400 -> Email already registered - Email address is already in use
     * 
     * ERR_5001 -> 400 -> OTP invalid - Provided OTP is incorrect
     * ERR_5002 -> 400 -> OTP expired - OTP has expired and is no longer valid
     * ERR_5004 -> 400 -> OTP max attempts - Too many failed OTP attempts
     * ERR_5006 -> 400 -> OTP already verified - OTP has already been used
     */
    
    // Example Error Response for ERR_4001:
    /*
    {
      "code": "ERR_4001",
      "message": "User already exists", 
      "details": "The phone number you provided is already registered. Please use a different phone number or try logging in.",
      "timestamp": "2024-01-15T10:30:00",
      "path": "/api/auth/signup",
      "traceId": "abc12345"
    }
    HTTP Status: 400 Bad Request
    
    Reason: Client provided invalid data (duplicate phone number) which violates business rules.
    This is a client error because the user should check if they already have an account
    or use a different phone number.
    */
}