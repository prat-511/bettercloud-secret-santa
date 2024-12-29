package com.bettercloud.santa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidParticipantsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParticipantsException(InvalidParticipantsException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.toString(),
                ex.getMessage()
        );
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(AssignmentImpossibleException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentImpossibleException(AssignmentImpossibleException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.toString(),
                ex.getMessage()
        );
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(NoAssignmentsException.class)
    public ResponseEntity<ErrorResponse> handleNoAssignmentsException(NoAssignmentsException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.toString(),
                ex.getMessage()
        );
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }
}
