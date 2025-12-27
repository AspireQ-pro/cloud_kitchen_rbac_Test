package com.cloudkitchen.rbac.exception;

/**
 * Documentation-only entry point for exception handling.
 *
 * Exception handling is split across multiple {@code @RestControllerAdvice} classes:
 * - {@link ValidationExceptionHandler}
 * - {@link SecurityExceptionHandler}
 * - {@link BusinessExceptionHandler}
 * - {@link InfrastructureExceptionHandler}
 * - {@link GenericExceptionHandler}
 *
 * This class intentionally contains no handlers.
 */
public final class GlobalExceptionHandler {

    private GlobalExceptionHandler() {
        // Documentation-only holder.
    }
}
