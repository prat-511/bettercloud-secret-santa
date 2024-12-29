package com.bettercloud.santa.controller;

import com.bettercloud.santa.exception.AssignmentImpossibleException;
import com.bettercloud.santa.exception.InvalidParticipantsException;
import com.bettercloud.santa.exception.NoAssignmentsException;
import com.bettercloud.santa.service.SecretSantaService;
import com.bettercloud.santa.dto.FamilyAssignmentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/secret-santa")
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
    @PostMapping(
            value = "/assignments/{year}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Flux<FamilyAssignmentDTO> createAssignments(
            @PathVariable Integer year,
            @RequestHeader(value = "API-Version", defaultValue = "1") String apiVersion
    ) {
        log.info("Creating Secret Santa assignments for year: {}, API Version: {}", year, apiVersion);
        return secretSantaService.createAssignments(year)
                .map(FamilyAssignmentDTO::from)
                .doOnComplete(() -> log.info("Completed creating assignments for year: {}", year))
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
