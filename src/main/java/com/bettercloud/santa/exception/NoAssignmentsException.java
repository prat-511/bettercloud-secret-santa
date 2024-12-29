package com.bettercloud.santa.exception;

/**
 * Exception thrown when no assignments can be made for Secret Santa.
 */
public class NoAssignmentsException extends RuntimeException {
    public NoAssignmentsException(String message) {
        super(message);
    }
}
