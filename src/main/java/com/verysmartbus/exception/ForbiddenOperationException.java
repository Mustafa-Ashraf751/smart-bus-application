package com.verysmartbus.exception;

/**
 * Thrown when a user tries to act on a resource (preference, reservation)
 * that belongs to a different user. A global @ControllerAdvice can catch
 * this and map it to HTTP 403 Forbidden.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
