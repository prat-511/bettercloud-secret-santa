package com.bettercloud.santa.controller;

import com.bettercloud.santa.exception.AssignmentImpossibleException;
import com.bettercloud.santa.exception.ErrorResponse;
import com.bettercloud.santa.exception.InvalidParticipantsException;
import com.bettercloud.santa.exception.NoAssignmentsException;
import com.bettercloud.santa.service.SecretSantaService;
import com.bettercloud.santa.dto.FamilyAssignmentDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/secret-santa")
@Tag(name = "Secret Santa", description = "API for managing Secret Santa assignments")
@RequiredArgsConstructor
@Slf4j
public class SecretSantaController {
    private final SecretSantaService secretSantaService;

    /**
     * Endpoint to create Secret Santa assignments for a given year.
     *
     * @param year       The year for which assignments are to be created.
     * @param apiVersion The API version (default is 1).
     * @return A Flux of FamilyAssignmentDTO objects.
     */
    @Operation(
            summary = "Get yearly assignments",
            description = "Retrieves existing Secret Santa assignments for the specified year or creates new ones if none exist"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved or created assignments",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = FamilyAssignmentDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - Occurs when there aren't enough participants or assignments are impossible",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping(
            value = "/assignments/{year}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Flux<FamilyAssignmentDTO> getAssignments(
            @Parameter(description = "Year for assignments", example = "2024")
            @PathVariable Integer year,
            @Parameter(description = "API Version", example = "1")
            @RequestHeader(value = "API-Version", defaultValue = "1") String apiVersion
    ) {
        log.info("Retrieving Secret Santa assignments for year: {}, API Version: {}", year, apiVersion);
        return secretSantaService.createAssignments(year)
                .map(FamilyAssignmentDTO::from)
                .doOnComplete(() -> log.info("Completed retrieving assignments for year: {}", year))
                .onErrorResume(this::handleError);
    }

    /**
     * Handles errors that occur during the creation of assignments.
     *
     * @param error The error that occurred.
     * @return A Mono error with the appropriate exception.
     */
    private Mono<FamilyAssignmentDTO> handleError(Throwable error) {
        log.error("Error creating assignments: {}", error.getMessage());
        if (error instanceof InvalidParticipantsException || error instanceof AssignmentImpossibleException || error instanceof NoAssignmentsException) {
            return Mono.error(error);
        }
        return Mono.error(new RuntimeException("Unexpected error occurred"));
    }
}
