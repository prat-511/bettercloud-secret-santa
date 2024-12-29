package com.bettercloud.santa.service;

import com.bettercloud.santa.model.FamilyAssignment;
import com.bettercloud.santa.model.FamilyMember;
import com.bettercloud.santa.repository.AssignmentRepository;
import com.bettercloud.santa.repository.MemberRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SecretSantaService {
    private static final int HISTORY_WINDOW = 3;

    private final MemberRepository memberRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentStrategy assignmentStrategy;
    private final AssignmentValidator validator;

    public SecretSantaService(
            MemberRepository memberRepository,
            AssignmentRepository assignmentRepository,
            AssignmentStrategy assignmentStrategy,
            AssignmentValidator validator
    ) {
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentStrategy = assignmentStrategy;
        this.validator = validator;
    }

    /**
     * Creates assignments for the given year.
     *
     * @param year The year for which assignments are to be created.
     * @return A Flux of FamilyAssignment objects.
     */
    public Flux<FamilyAssignment> createAssignments(Integer year) {
        return memberRepository.findAllWithRelations()
                .collectList()
                .flatMap(validator::validateParticipants)
                .flatMap(members -> generateValidAssignments(year, members))
                .flatMapMany(this::saveAndLoadAssignments);
    }

    /**
     * Saves the assignments and loads their details.
     *
     * @param assignments The list of FamilyAssignment objects to be saved.
     * @return A Flux of FamilyAssignment objects with details loaded.
     */
    private Flux<FamilyAssignment> saveAndLoadAssignments(List<FamilyAssignment> assignments) {
        return assignmentRepository.saveAll(assignments)
                .flatMap(this::loadAssignmentDetails);
    }

    /**
     * Generates valid assignments for the given year and list of members.
     *
     * @param year    The year for which assignments are to be generated.
     * @param members The list of family members.
     * @return A Mono containing the list of valid FamilyAssignment objects.
     */
    private Mono<List<FamilyAssignment>> generateValidAssignments(Integer year, List<FamilyMember> members) {
        return getRecentAssignments(year)
                .map(recentAssignments ->
                        assignmentStrategy.generateAssignments(year, members, recentAssignments));
    }

    /**
     * Retrieves recent assignments within the history window.
     *
     * @param year The current year.
     * @return A Mono containing a map of recent assignments.
     */
    private Mono<Map<Long, Set<Long>>> getRecentAssignments(Integer year) {
        return assignmentRepository.findByYearsBetween(year - HISTORY_WINDOW, year - 1)
                .collectList()
                .map(this::groupAssignmentsByGiver);
    }

    /**
     * Groups assignments by the giver's ID.
     *
     * @param history The list of past FamilyAssignment objects.
     * @return A map of giver IDs to sets of recipient IDs.
     */
    private Map<Long, Set<Long>> groupAssignmentsByGiver(List<FamilyAssignment> history) {
        return history.stream()
                .collect(Collectors.groupingBy(
                        FamilyAssignment::getSantaId,
                        Collectors.mapping(FamilyAssignment::getRecipientId, Collectors.toSet())
                ));
    }

    /**
     * Loads the details of a given assignment.
     *
     * @param assignment The FamilyAssignment object.
     * @return A Mono containing the FamilyAssignment object with details loaded.
     */
    private Mono<FamilyAssignment> loadAssignmentDetails(FamilyAssignment assignment) {
        return Mono.zip(
                memberRepository.findById(assignment.getSantaId()),
                memberRepository.findById(assignment.getRecipientId())
        ).map(tuple -> {
            assignment.setSanta(tuple.getT1());
            assignment.setRecipient(tuple.getT2());
            return assignment;
        });
    }
}
