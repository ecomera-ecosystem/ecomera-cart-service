package com.ecomera.cart.shared.common.exception;

import org.springframework.http.HttpStatus;

public class AlreadyExistException extends ApiException {

    private static final String ERROR_MESSAGE_TEMPLATE = "%s already exists with %s: '%s'";

    public AlreadyExistException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public AlreadyExistException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, cause);
    }

    public AlreadyExistException(String resourceName, String fieldName, Object fieldValue) {
        super(
                String.format(ERROR_MESSAGE_TEMPLATE, resourceName, fieldName, fieldValue),
                HttpStatus.CONFLICT
        );
    }

    public AlreadyExistException(String resourceName, String fieldName, Object fieldValue, Throwable cause) {
        super(
                String.format(ERROR_MESSAGE_TEMPLATE, resourceName, fieldName, fieldValue),
                HttpStatus.CONFLICT,
                cause
        );
    }

    public AlreadyExistException(Class<?> resourceName, String fieldName, Object fieldValue) {
        super(
                String.format(ERROR_MESSAGE_TEMPLATE, resourceName.getSimpleName(), fieldName, fieldValue),
                HttpStatus.CONFLICT
        );
    }
}
