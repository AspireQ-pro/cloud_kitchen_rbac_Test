package com.cloudkitchen.rbac.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.constants.ResponseMessages;
import com.cloudkitchen.rbac.exception.BusinessExceptions.CustomerNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.InvalidOtpException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.LoginMethodNotAllowedException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MerchantAlreadyExistsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MerchantNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MobileNotRegisteredException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpAttemptsExceededException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpExpiredException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpInvalidException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.UserAlreadyExistsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.UserNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.ValidationException;
import com.cloudkitchen.rbac.util.ErrorSanitizer;
import com.cloudkitchen.rbac.util.ResponseBuilder;

/**
 * Exception handler for business rule and domain errors.
 */
@RestControllerAdvice
public class BusinessExceptionHandler extends ExceptionHandlerSupport {

    private static final Logger logger = LoggerFactory.getLogger(BusinessExceptionHandler.class);

    /**
     * Handle user-not-found errors.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, "User not found");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "The requested user does not exist"));
        addRequestContext(response, request);

        logger.warn("User not found for request");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle merchant-not-found errors.
     */
    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMerchantNotFoundException(MerchantNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, "Merchant not found");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "The requested merchant does not exist"));
        addRequestContext(response, request);

        logger.warn("Merchant not found for request");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle customer-not-found errors.
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCustomerNotFoundException(CustomerNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, ResponseMessages.Customer.NOT_FOUND);
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "The requested customer does not exist"));
        addRequestContext(response, request);

        logger.warn("Customer not found for request");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle conflict errors for existing merchants or users.
     */
    @ExceptionHandler({MerchantAlreadyExistsException.class, UserAlreadyExistsException.class})
    public ResponseEntity<Map<String, Object>> handleAlreadyExistsException(RuntimeException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(409, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Resource conflict occurred: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle OTP-related errors.
     */
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<Map<String, Object>> handleOtpException(OtpException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "OTP operation failed");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "OTP operation failed. Please try again."));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "OTP exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle business validation errors.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle cases where the mobile number is not registered.
     */
    @ExceptionHandler(MobileNotRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleMobileNotRegisteredException(MobileNotRegisteredException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        logger.warn("Mobile not registered for request");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle missing OTP records.
     */
    @ExceptionHandler(OtpNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOtpNotFoundException(OtpNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        logger.warn("OTP not found for request");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle expired OTPs.
     */
    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleOtpExpiredException(OtpExpiredException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        logger.warn("OTP expired for request");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle invalid OTP submissions.
     */
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOtpException(InvalidOtpException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        logger.warn("Invalid OTP for request");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle OTP attempt limit exceeded errors.
     */
    @ExceptionHandler(OtpAttemptsExceededException.class)
    public ResponseEntity<Map<String, Object>> handleOtpAttemptsExceededException(OtpAttemptsExceededException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(429, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        logger.warn("OTP attempts exceeded for request");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Handle unsupported login method errors.
     */
    @ExceptionHandler(LoginMethodNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleLoginMethodNotAllowedException(LoginMethodNotAllowedException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(403, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Login method not allowed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle OTP format or verification errors.
     */
    @ExceptionHandler(OtpInvalidException.class)
    public ResponseEntity<Map<String, Object>> handleOtpInvalidException(OtpInvalidException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, ErrorSanitizer.sanitizeErrorMessage(ex.getMessage()));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "OTP invalid: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
