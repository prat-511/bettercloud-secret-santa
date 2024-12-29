package com.bettercloud.santa.service;

import com.bettercloud.santa.exception.AssignmentImpossibleException;
import com.bettercloud.santa.exception.InvalidParticipantsException;
import com.bettercloud.santa.model.FamilyMember;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssignmentValidator {

    /**
     * Validates the list of participants for the Secret Santa assignment.
     *
     * @param members The list of family members participating.
     * @return A Mono containing the validated list of family members.
     */
    public Mono<List<FamilyMember>> validateParticipants(List<FamilyMember> members) {
        if (isParticipantListInvalid(members)) {
            return createParticipantError(members);
        }

        Map<Integer, Long> familySizes = calculateFamilySizes(members);
        if (isLargestFamilyTooLarge(familySizes, members.size())) {
            return Mono.error(new AssignmentImpossibleException(
                    "Assignment impossible: largest family too big relative to participants"
            ));
        }

        return Mono.just(members);
    }

    /**
     * Checks if the participant list is invalid.
     *
     * @param members The list of family members.
     * @return True if the list is empty or has fewer than 2 participants, false otherwise.
     */
    private boolean isParticipantListInvalid(List<FamilyMember> members) {
        return members.isEmpty() || members.size() < 2;
    }

    /**
     * Creates an error Mono for invalid participant lists.
     *
     * @param members The list of family members.
     * @return A Mono error with the appropriate exception.
     */
    private Mono<List<FamilyMember>> createParticipantError(List<FamilyMember> members) {
        String errorMessage = members.isEmpty() ? "No participants found" : "Need at least 2 participants";
        return Mono.error(new InvalidParticipantsException(errorMessage));
    }

    /**
     * Calculates the sizes of each family in the participant list.
     *
     * @param members The list of family members.
     * @return A map of family IDs to their respective sizes.
     */
    private Map<Integer, Long> calculateFamilySizes(List<FamilyMember> members) {
        return members.stream()
                .collect(Collectors.groupingBy(FamilyMember::getFamilyId, Collectors.counting()));
    }

    /**
     * Checks if the largest family is too large relative to the total number of participants.
     *
     * @param familySizes  A map of family IDs to their respective sizes.
     * @param totalMembers The total number of participants.
     * @return True if the largest family is too large, false otherwise.
     */
    private boolean isLargestFamilyTooLarge(Map<Integer, Long> familySizes, int totalMembers) {
        long maxFamilySize = familySizes.values().stream()
                .mapToLong(Long::valueOf)
                .max()
                .orElse(0);
        return maxFamilySize > (totalMembers - maxFamilySize);
    }
}
