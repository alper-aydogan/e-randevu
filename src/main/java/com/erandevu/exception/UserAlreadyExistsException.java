package com.erandevu.exception;

/**
 * Exception thrown when attempting to create a user that already exists.
 * Extends BaseException for standardized error handling.
 */
public class UserAlreadyExistsException extends BaseException {

    public UserAlreadyExistsException(String message) {
        super(ErrorCode.USER_ALREADY_EXISTS, message);
    }

    public UserAlreadyExistsException(String username) {
        super(ErrorCode.USER_ALREADY_EXISTS,
              String.format("User already exists with username: %s", username));
    }
}
