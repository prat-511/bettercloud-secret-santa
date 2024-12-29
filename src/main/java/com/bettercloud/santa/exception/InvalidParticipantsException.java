package com.bettercloud.santa.exception;

/**
 * Exception thrown when participants are invalid for Secret Santa assignment.
 */
public class InvalidParticipantsException extends RuntimeException {
    public InvalidParticipantsException(String message) {
        super(message);
    }
}
