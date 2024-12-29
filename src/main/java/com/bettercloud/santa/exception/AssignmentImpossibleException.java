package com.bettercloud.santa.exception;

/**
 * Exception thrown when it is impossible to assign Secret Santa due to constraints.
 */
public class AssignmentImpossibleException extends RuntimeException {
    public AssignmentImpossibleException(String message) {
        super(message);
    }
}
