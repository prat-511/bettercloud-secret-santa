package com.bettercloud.santa.service;

import com.bettercloud.santa.exception.AssignmentImpossibleException;
import com.bettercloud.santa.exception.InvalidParticipantsException;
import com.bettercloud.santa.model.FamilyAssignment;
import com.bettercloud.santa.model.FamilyMember;
import com.bettercloud.santa.repository.AssignmentRepository;
import com.bettercloud.santa.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretSantaServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AssignmentRepository assignmentRepository;

    private SecretSantaService secretSantaService;
    private HamiltonianCycleStrategy strategy;

    @BeforeEach
    void setUp() {
        AssignmentValidator validator = new AssignmentValidator();
        strategy = new HamiltonianCycleStrategy();
        secretSantaService = new SecretSantaService(
                memberRepository,
                assignmentRepository,
                strategy,
                validator
        );
    }

    @Test
    void whenNoParticipants_thenThrowInvalidParticipantsException() {
        // Given
        when(memberRepository.findAllWithRelations()).thenReturn(Flux.empty());
        when(assignmentRepository.findByYearsBetween(2024, 2024)).thenReturn(Flux.empty());

        // When & Then
        assertThrows(
                InvalidParticipantsException.class,
                () -> secretSantaService.createAssignments(2024).collectList().block()
        );
    }

    @Test
    void whenFamilyTooLarge_thenThrowAssignmentImpossibleException() {
        // Given
        List<FamilyMember> sameFamily = Arrays.asList(
                new FamilyMember(1L, 1, "A"),
                new FamilyMember(2L, 1, "B"),
                new FamilyMember(3L, 1, "C")  // All from family 1
        );
        when(memberRepository.findAllWithRelations()).thenReturn(Flux.fromIterable(sameFamily));
        when(assignmentRepository.findByYearsBetween(2024, 2024)).thenReturn(Flux.empty());

        // When & Then
        assertThrows(AssignmentImpossibleException.class, () ->
                secretSantaService.createAssignments(2024).blockFirst(Duration.ofSeconds(5))
        );
    }

    @Test
    void whenValidParticipants_thenCreateAssignments() {
        // Given
        List<FamilyMember> members = Arrays.asList(
                new FamilyMember(1L, 1, "A"),
                new FamilyMember(2L, 1, "B"),
                new FamilyMember(3L, 2, "C"),
                new FamilyMember(4L, 2, "D"),
                new FamilyMember(5L, 3, "E"),
                new FamilyMember(6L, 3, "F")
        );

        List<FamilyAssignment> assignments = strategy.generateAssignments(2024, members, new HashMap<>());

        when(memberRepository.findAllWithRelations()).thenReturn(Flux.fromIterable(members));
        when(assignmentRepository.findByYearsBetween(any(), any())).thenReturn(Flux.empty());
        when(assignmentRepository.saveAll((Iterable<FamilyAssignment>) any())).thenReturn(Flux.fromIterable(assignments));
        when(memberRepository.findById((Long) any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return members.stream()
                    .filter(m -> m.getId().equals(id))
                    .findFirst()
                    .map(Mono::just)
                    .orElse(Mono.empty());
        });

        // When
        List<FamilyAssignment> result = secretSantaService.createAssignments(2024)
                .collectList()
                .block(Duration.ofSeconds(5));

        // Then
        assertNotNull(result);
        assertEquals(6, result.size());
    }

    @Test
    void whenValidParticipants_thenEnsureNoFamilyMemberAssignment() {
        // Given
        List<FamilyMember> members = Arrays.asList(
                new FamilyMember(1L, 1, "A"),
                new FamilyMember(2L, 1, "B"),
                new FamilyMember(3L, 2, "C"),
                new FamilyMember(4L, 2, "D"),
                new FamilyMember(5L, 3, "E"),
                new FamilyMember(6L, 3, "F")
        );

        // When
        List<FamilyAssignment> assignments = strategy.generateAssignments(2024, members, new HashMap<>());

        // Then
        assertNotNull(assignments);
        assertEquals(6, assignments.size());

        for (FamilyAssignment assignment : assignments) {
            FamilyMember santa = findMember(members, assignment.getSantaId());
            FamilyMember recipient = findMember(members, assignment.getRecipientId());

            // Verify no self-assignments
            assertNotEquals(santa.getId(), recipient.getId(),
                    String.format("%s cannot be their own Secret Santa", santa.getName()));

            // Verify no family member assignments
            assertNotEquals(santa.getFamilyId(), recipient.getFamilyId(),
                    String.format("%s cannot give to family member %s",
                            santa.getName(), recipient.getName()));
        }

    }

    @Test
    void whenPreviousAssignmentsExist_thenEnsureNoRepeatsWithinThreeYears() {
        // Given
        List<FamilyMember> members = Arrays.asList(
                new FamilyMember(1L, 1, "A"),
                new FamilyMember(2L, 1, "B"),
                new FamilyMember(3L, 2, "C"),
                new FamilyMember(4L, 2, "D"),
                new FamilyMember(5L, 3, "E"),
                new FamilyMember(6L, 3, "F")
        );

        Map<Integer, List<FamilyAssignment>> allAssignments = new HashMap<>();
        Map<Long, Set<Long>> recentAssignments = new HashMap<>();

        // Generate assignments for 2024-2027
        for (int year = 2024; year <= 2027; year++) {
            List<FamilyAssignment> yearAssignments = strategy.generateAssignments(year, members, recentAssignments);
            allAssignments.put(year, yearAssignments);

            // Verify rules for this year
            verifyAssignments(yearAssignments, members, year, recentAssignments);

            // Update recent assignments for next year
            updateRecentAssignments(recentAssignments, yearAssignments);
        }

        // Verify no repeats within 3-year window
        verifyNoRepeatsWithinThreeYears(allAssignments, members);
    }

    private void verifyAssignments(List<FamilyAssignment> assignments, List<FamilyMember> members,
                                   int year, Map<Long, Set<Long>> recentAssignments) {
        // Basic verification
        assertNotNull(assignments);
        assertEquals(members.size(), assignments.size());

        // Verify no self-assignments and no family member assignments
        for (FamilyAssignment assignment : assignments) {
            FamilyMember santa = findMember(members, assignment.getSantaId());
            FamilyMember recipient = findMember(members, assignment.getRecipientId());

            assertNotEquals(santa.getId(), recipient.getId(),
                    String.format("Year %d: %s cannot be their own Secret Santa",
                            year, santa.getName()));

            assertNotEquals(santa.getFamilyId(), recipient.getFamilyId(),
                    String.format("Year %d: %s cannot give to family member %s",
                            year, santa.getName(), recipient.getName()));

            // Verify no recent repeats
            Set<Long> previousRecipients = recentAssignments.get(santa.getId());
            if (previousRecipients != null) {
                assertFalse(previousRecipients.contains(recipient.getId()),
                        String.format("Year %d: %s cannot give to %s (assigned within last 3 years)",
                                year, santa.getName(), recipient.getName()));
            }
        }
    }

    private void verifyNoRepeatsWithinThreeYears(Map<Integer, List<FamilyAssignment>> allAssignments,
                                                 List<FamilyMember> members) {
        for (int year = 2025; year <= 2027; year++) {
            List<FamilyAssignment> currentYear = allAssignments.get(year);
            for (FamilyAssignment current : currentYear) {
                // Check previous three years
                for (int prevYear = year - 3; prevYear < year; prevYear++) {
                    if (allAssignments.containsKey(prevYear)) {
                        List<FamilyAssignment> previousYear = allAssignments.get(prevYear);
                        for (FamilyAssignment previous : previousYear) {
                            if (current.getSantaId().equals(previous.getSantaId())) {
                                assertNotEquals(current.getRecipientId(), previous.getRecipientId(),
                                        String.format("%s cannot give to %s in year %d (previously assigned in %d)",
                                                getMemberName(members, current.getSantaId()),
                                                getMemberName(members, current.getRecipientId()),
                                                year, prevYear));
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateRecentAssignments(Map<Long, Set<Long>> recentAssignments,
                                         List<FamilyAssignment> newAssignments) {
        for (FamilyAssignment assignment : newAssignments) {
            recentAssignments.computeIfAbsent(assignment.getSantaId(), k -> new HashSet<>())
                    .add(assignment.getRecipientId());
        }
    }

    private FamilyMember findMember(List<FamilyMember> members, Long id) {
        return members.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Member not found: " + id));
    }

    private String getMemberName(List<FamilyMember> members, Long id) {
        return members.stream()
                .filter(m -> m.getId().equals(id))
                .map(FamilyMember::getName)
                .findFirst()
                .orElse("Unknown");
    }

    @Test
    void whenMultipleUsersCreateAssignmentsConcurrently_thenAllSucceed() {
        // Given
        List<FamilyMember> members = Arrays.asList(
                new FamilyMember(1L, 1, "A"),
                new FamilyMember(2L, 1, "B"),
                new FamilyMember(3L, 2, "C"),
                new FamilyMember(4L, 2, "D")
        );
        when(memberRepository.findAllWithRelations()).thenReturn(Flux.fromIterable(members));
        when(assignmentRepository.findByYearsBetween(any(), any())).thenReturn(Flux.empty());
        when(assignmentRepository.saveAll(any(Iterable.class)))
                .thenAnswer(i -> Flux.fromIterable((Iterable<FamilyAssignment>) i.getArgument(0)));
        when(memberRepository.findById((Long) any())).thenAnswer(i -> Mono.just(members.get(0)));

        // When - Simulate multiple concurrent requests
        int numberOfConcurrentUsers = 10;
        List<Mono<List<FamilyAssignment>>> concurrentRequests = new ArrayList<>();

        for (int i = 0; i < numberOfConcurrentUsers; i++) {
            concurrentRequests.add(
                    secretSantaService.createAssignments(2024 + i)
                            .collectList()
            );
        }

        // Then
        List<List<FamilyAssignment>> results = Flux.merge(concurrentRequests)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertNotNull(results);
        assertEquals(numberOfConcurrentUsers, results.size());
    }

    @Test
    void whenConcurrentUsersRequestSameYear_thenAllGetValidAssignments() {
        // Given
        List<FamilyMember> members = Arrays.asList(
                new FamilyMember(1L, 1, "A"),
                new FamilyMember(2L, 1, "B"),
                new FamilyMember(3L, 2, "C"),
                new FamilyMember(4L, 2, "D")
        );
        when(memberRepository.findAllWithRelations()).thenReturn(Flux.fromIterable(members));
        when(assignmentRepository.findByYearsBetween(any(), any())).thenReturn(Flux.empty());
        when(assignmentRepository.saveAll(any(Iterable.class)))
                .thenAnswer(i -> Flux.fromIterable((Iterable<FamilyAssignment>) i.getArgument(0)));
        when(memberRepository.findById((Long) any())).thenAnswer(i ->
                Mono.just(members.stream()
                        .filter(m -> m.getId().equals(i.getArgument(0)))
                        .findFirst()
                        .orElse(members.get(0))));

        // When - Simulate multiple concurrent requests for same year
        int numberOfConcurrentUsers = 10;
        List<Mono<List<FamilyAssignment>>> concurrentRequests = new ArrayList<>();

        for (int i = 0; i < numberOfConcurrentUsers; i++) {
            concurrentRequests.add(
                    secretSantaService.createAssignments(2024)
                            .collectList()
            );
        }

        // Then
        List<List<FamilyAssignment>> results = Flux.merge(concurrentRequests)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertNotNull(results);
        assertEquals(numberOfConcurrentUsers, results.size());

        // Verify each assignment set is valid
        for (List<FamilyAssignment> assignments : results) {
            verifyAssignments(assignments, members, 2024, new HashMap<>());
        }
    }
}
