package com.bettercloud.santa.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Error Response Structure")
public class ErrorResponse {
    @Schema(description = "Error code", example = "INVALID_PARTICIPANTS")
    private String code;

    @Schema(description = "Error message", example = "Not enough participants for Secret Santa assignment")
    private String message;

    @Schema(description = "Error timestamp", example = "2024-01-01T10:00:00")
    private LocalDateTime timestamp;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
