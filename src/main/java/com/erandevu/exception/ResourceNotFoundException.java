package com.erandevu.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Extends BaseException for standardized error handling.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
              String.format("%s not found with id: %d", resourceName, id));
    }
}
