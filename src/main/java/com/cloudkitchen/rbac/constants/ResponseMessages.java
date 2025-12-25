package com.cloudkitchen.rbac.constants;

/**
 * Centralized response messages for consistent API responses
 */
public final class ResponseMessages {
    
    private ResponseMessages() {
        // Utility class
    }
    
    // Authentication Messages
    public static final class Auth {
        public static final String LOGIN_SUCCESS = "Login successful";
        public static final String CUSTOMER_LOGIN_SUCCESS = "Customer login successful";
        public static final String LOGOUT_SUCCESS = "Logged out successfully";
        public static final String REGISTRATION_SUCCESS = "Customer registration successful";
        public static final String TOKEN_REFRESH_SUCCESS = "Token refreshed successfully";
        public static final String INVALID_CREDENTIALS = "Invalid username or password";
        public static final String ACCOUNT_INACTIVE = "Account is inactive";
        public static final String TOKEN_EXPIRED = "Your session has expired. Please login again.";
        public static final String INVALID_TOKEN = "Invalid token";
        public static final String ACCESS_DENIED = "Access denied";
        public static final String MERCHANT_LOGIN_ONLY = "Only merchant (0) login allowed";
        public static final String CUSTOMER_MERCHANT_ID_REQUIRED = "Valid merchantId (>0) is required for customer login";
    }
    
    // OTP Messages
    public static final class Otp {
        public static final String OTP_SENT = "OTP sent to your phone successfully.";
        public static final String PASSWORD_RESET_OTP = "Password reset OTP sent to your phone. Valid for 5 minutes.";
        public static final String PHONE_VERIFICATION_OTP = "Phone verification OTP sent. Valid for 10 minutes.";
        public static final String ACCOUNT_VERIFICATION_OTP = "Account verification OTP sent. Valid for 15 minutes.";
        public static final String OTP_VERIFIED = "Verification successful";
        public static final String OTP_PASSWORD_RESET_SUCCESS = "OTP verified. Default password has been set. Please change it in your profile.";
        public static final String OTP_EXPIRED = "OTP expired";
        public static final String OTP_INVALID = "Invalid OTP";
        public static final String OTP_ALREADY_USED = "OTP already used";
        public static final String NO_OTP_REQUEST = "OTP not requested";
        public static final String PHONE_NOT_REGISTERED = "Mobile number not registered";
        public static final String PHONE_REQUIRED = "Phone number is required";
        public static final String MOBILE_REQUIRED = "Mobile number is required";
        public static final String OTP_REQUIRED = "OTP is required";
        public static final String INVALID_OTP_FORMAT = "Invalid OTP format";
        public static final String INVALID_MOBILE_FORMAT = "Invalid mobile number format";
        public static final String MERCHANT_ID_REQUIRED_OTP = "MerchantId is required (use 0 for OTP by phone number)";
        public static final String SMS_SERVICE_UNAVAILABLE = "SMS service temporarily unavailable";
        public static final String OTP_REQUEST_FAILED = "OTP request failed";
        public static final String OTP_VERIFICATION_FAILED = "OTP verification failed";
        public static final String OTP_ATTEMPTS_EXCEEDED = "OTP verification attempts exceeded";
    }
    
    // Merchant Messages
    public static final class Merchant {
        public static final String CREATED_SUCCESS = "Merchant created successfully";
        public static final String UPDATED_SUCCESS = "Merchant updated successfully";
        public static final String DELETED_SUCCESS = "Merchant deleted successfully";
        public static final String RETRIEVED_SUCCESS = "Merchant profile retrieved successfully";
        public static final String LIST_SUCCESS = "Merchants retrieved successfully";
        public static final String NOT_FOUND = "Merchant not found";
        public static final String ACCESS_DENIED = "Access denied: You can only access your own merchant data";
        public static final String CANNOT_DELETE = "Merchant cannot be deleted";
        public static final String ALREADY_EXISTS = "Merchant already exists";
    }
    
    // Customer Messages
    public static final class Customer {
        public static final String CREATED_SUCCESS = "Customer created successfully";
        public static final String UPDATED_SUCCESS = "Customer profile updated successfully";
        public static final String DELETED_SUCCESS = "Customer deleted successfully";
        public static final String RETRIEVED_SUCCESS = "Customer details retrieved successfully";
        public static final String PROFILE_RETRIEVED_SUCCESS = "Profile retrieved successfully";
        public static final String LIST_SUCCESS = "Customers retrieved successfully";
        public static final String MERCHANT_CUSTOMERS_SUCCESS = "Merchant customers retrieved successfully";
        public static final String NOT_FOUND = "Customer not found";
        public static final String PROFILE_NOT_FOUND = "Profile not found";
        public static final String ACCESS_DENIED_PROFILE = "Access denied. Customers can only update their own profile.";
        public static final String ACCESS_DENIED_VIEW = "Access denied. Customers can only view their own profile.";
        public static final String ACCESS_DENIED_LIST = "Access denied. Only merchants and admins can view customer lists.";
        public static final String ACCESS_DENIED_MERCHANT_CUSTOMERS = "Access denied. Customers cannot view other customers. Merchants can only view their own customers.";
        public static final String UPDATE_FAILED = "Failed to update customer";
        public static final String RETRIEVE_FAILED = "Failed to retrieve customers";
        public static final String PROFILE_INCOMPLETE = "Please complete your profile";
    }
    
    // User Messages
    public static final class User {
        public static final String CREATED_SUCCESS = "User created successfully";
        public static final String UPDATED_SUCCESS = "User updated successfully";
        public static final String DELETED_SUCCESS = "User deleted successfully";
        public static final String RETRIEVED_SUCCESS = "User retrieved successfully";
        public static final String LIST_SUCCESS = "Users retrieved successfully";
        public static final String NOT_FOUND = "User not found";
        public static final String ALREADY_EXISTS = "User already exists";
        public static final String PASSWORD_CHANGED = "Password changed successfully";
        public static final String PROFILE_UPDATED = "Profile updated successfully";
    }
    
    // File/Upload Messages
    public static final class File {
        public static final String UPLOAD_SUCCESS = "File uploaded successfully";
        public static final String UPLOAD_FAILED = "File upload failed";
        public static final String DELETE_SUCCESS = "File deleted successfully";
        public static final String DELETE_FAILED = "File deletion failed";
        public static final String INVALID_FILE_TYPE = "Invalid file type";
        public static final String FILE_TOO_LARGE = "File size exceeds maximum limit";
        public static final String FILE_NOT_FOUND = "File not found";
        public static final String STORAGE_ERROR = "Storage service error";
    }
    
    // Validation Messages
    public static final class Validation {
        public static final String INVALID_REQUEST = "Invalid request parameters";
        public static final String REQUIRED_FIELD_MISSING = "Required field is missing";
        public static final String INVALID_FORMAT = "Invalid format";
        public static final String INVALID_EMAIL = "Invalid email format";
        public static final String INVALID_PHONE = "Invalid phone number format";
        public static final String PASSWORD_TOO_WEAK = "Password does not meet security requirements";
        public static final String INVALID_DATE = "Invalid date format";
        public static final String VALUE_OUT_OF_RANGE = "Value is out of acceptable range";
        public static final String FIELD_CANNOT_BE_EMPTY = "Field cannot be empty or contain only whitespace";
        public static final String MERCHANT_NAME_WHITESPACE = "Merchant name cannot be empty or contain only whitespace";
        public static final String USERNAME_WHITESPACE = "Username cannot be empty or contain only whitespace";
        public static final String EMAIL_WHITESPACE = "Email cannot be empty or contain only whitespace";
        public static final String OTP_MUST_BE_NUMERIC = "OTP must be numeric";
    }
    
    // System Messages
    public static final class System {
        public static final String HEALTH_OK = "Service is healthy";
        public static final String MAINTENANCE_MODE = "System is under maintenance";
        public static final String SERVICE_UNAVAILABLE = "Service temporarily unavailable";
        public static final String INTERNAL_ERROR = "Internal server error";
        public static final String RATE_LIMIT_EXCEEDED = "Too many requests. Please try again later.";
        public static final String OPERATION_SUCCESS = "Operation completed successfully";
        public static final String OPERATION_FAILED = "Operation failed";
    }
    
    // Permission/Role Messages
    public static final class Permission {
        public static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions";
        public static final String ROLE_ASSIGNED = "Role assigned successfully";
        public static final String ROLE_REMOVED = "Role removed successfully";
        public static final String PERMISSION_GRANTED = "Permission granted successfully";
        public static final String PERMISSION_REVOKED = "Permission revoked successfully";
        public static final String INVALID_ROLE = "Invalid role specified";
        public static final String ROLE_NOT_FOUND = "Role not found";
    }
    
    // Data Integrity Messages
    public static final class DataIntegrity {
        public static final String EMAIL_ALREADY_EXISTS = "A user with this email address already exists. Please use a different email.";
        public static final String PHONE_ALREADY_EXISTS = "A user with this phone number already exists. Please use a different phone number.";
        public static final String USERNAME_ALREADY_EXISTS = "This username is already taken. Please choose a different username.";
        public static final String MERCHANT_EMAIL_EXISTS = "A merchant with this email address already exists. Please use a different email.";
        public static final String MERCHANT_PHONE_EXISTS = "A merchant with this phone number already exists. Please use a different phone number.";
        public static final String DUPLICATE_ENTRY = "This information is already registered in the system. Please check your details.";
        public static final String CONSTRAINT_VIOLATION = "Data constraint violation";
    }
}