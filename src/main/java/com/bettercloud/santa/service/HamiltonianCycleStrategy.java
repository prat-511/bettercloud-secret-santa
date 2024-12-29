package com.bettercloud.santa.service;

import com.bettercloud.santa.exception.AssignmentImpossibleException;
import com.bettercloud.santa.model.FamilyAssignment;
import com.bettercloud.santa.model.FamilyMember;

import com.bettercloud.santa.model.RelationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class HamiltonianCycleStrategy implements AssignmentStrategy {
    private static final Logger logger = LoggerFactory.getLogger(HamiltonianCycleStrategy.class);

    @Override
    public List<FamilyAssignment> generateAssignments(
            Integer year,
            List<FamilyMember> members,
            Map<Long, Set<Long>> recentAssignments
    ) {
        Map<FamilyMember, List<FamilyMember>> graph = buildGraph(members, recentAssignments);
        List<FamilyMember> cycle = findHamiltonianCycle(graph, members);

        if (cycle == null) {
            throw new AssignmentImpossibleException("Failed to generate valid assignments due to family constraints");
        }

        logger.info("Successfully generated assignments using HAMILTONIAN_CYCLE strategy");
        return createAssignmentsFromCycle(year, cycle);
    }

    /**
     * Creates FamilyAssignment objects from the Hamiltonian cycle.
     *
     * @param year  The year of the assignment.
     * @param cycle The Hamiltonian cycle of family members.
     * @return A list of FamilyAssignment objects.
     */
    private List<FamilyAssignment> createAssignmentsFromCycle(Integer year, List<FamilyMember> cycle) {
        List<FamilyAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < cycle.size(); i++) {
            assignments.add(new FamilyAssignment(
                    year,
                    cycle.get(i).getId(),
                    cycle.get((i + 1) % cycle.size()).getId()
            ));
        }
        return assignments;
    }

    /**
     * Builds a graph of valid assignments based on the given members and recent assignments.
     *
     * @param members           The list of family members.
     * @param recentAssignments A map of recent assignments.
     * @return A graph represented as a map of FamilyMember to a list of valid recipients.
     */
    private Map<FamilyMember, List<FamilyMember>> buildGraph(
            List<FamilyMember> members,
            Map<Long, Set<Long>> recentAssignments
    ) {
        Map<FamilyMember, List<FamilyMember>> graph = new HashMap<>();
        BitSet assigned = new BitSet(members.size());

        for (FamilyMember santa : members) {
            graph.put(santa, new ArrayList<>());
            for (FamilyMember recipient : members) {
                if (isValidAssignment(santa, recipient, recentAssignments, assigned, members)) {
                    graph.get(santa).add(recipient);
                }
            }
        }
        return graph;
    }

    /**
     * Finds a Hamiltonian cycle in the given graph.
     *
     * @param graph   The graph of valid assignments.
     * @param members The list of family members.
     * @return A list representing the Hamiltonian cycle, or null if no cycle is found.
     */
    private List<FamilyMember> findHamiltonianCycle(
            Map<FamilyMember, List<FamilyMember>> graph,
            List<FamilyMember> members
    ) {
        List<FamilyMember> path = new ArrayList<>();
        Set<FamilyMember> visited = new HashSet<>();
        FamilyMember start = members.get(0);

        path.add(start);
        visited.add(start);

        if (dfs(graph, path, visited, members.size())) {
            return path;
        }
        return null;
    }

    /**
     * Performs a depth-first search to find a Hamiltonian cycle.
     *
     * @param graph        The graph of valid assignments.
     * @param path         The current path in the search.
     * @param visited      The set of visited family members.
     * @param totalMembers The total number of family members.
     * @return True if a Hamiltonian cycle is found, false otherwise.
     */
    private boolean dfs(
            Map<FamilyMember, List<FamilyMember>> graph,
            List<FamilyMember> path,
            Set<FamilyMember> visited,
            int totalMembers
    ) {
        if (path.size() == totalMembers) {
            return isValidCycle(graph, path);
        }

        FamilyMember current = path.get(path.size() - 1);
        return tryAllPossibleNext(graph, path, visited, totalMembers, current);
    }

    /**
     * Checks if the current path forms a valid Hamiltonian cycle.
     *
     * @param graph The graph of valid assignments.
     * @param path  The current path in the search.
     * @return True if the path forms a valid Hamiltonian cycle, false otherwise.
     */
    private boolean isValidCycle(Map<FamilyMember, List<FamilyMember>> graph, List<FamilyMember> path) {
        FamilyMember last = path.get(path.size() - 1);
        FamilyMember first = path.get(0);
        return graph.get(last).contains(first);
    }

    /**
     * Tries all possible next steps in the depth-first search.
     *
     * @param graph        The graph of valid assignments.
     * @param path         The current path in the search.
     * @param visited      The set of visited family members.
     * @param totalMembers The total number of family members.
     * @param current      The current family member in the search.
     * @return True if a Hamiltonian cycle is found, false otherwise.
     */
    private boolean tryAllPossibleNext(
            Map<FamilyMember, List<FamilyMember>> graph,
            List<FamilyMember> path,
            Set<FamilyMember> visited,
            int totalMembers,
            FamilyMember current
    ) {
        for (FamilyMember next : graph.get(current)) {
            if (!visited.contains(next)) {
                visited.add(next);
                path.add(next);
                if (dfs(graph, path, visited, totalMembers)) {
                    return true;
                }
                visited.remove(next);
                path.remove(path.size() - 1);
            }
        }
        return false;
    }

    /**
     * Checks if the given assignment is valid.
     *
     * @param santa             The family member acting as Santa.
     * @param recipient         The family member receiving the gift.
     * @param recentAssignments A map of recent assignments.
     * @param assigned          A BitSet tracking assigned recipients.
     * @param members           The list of family members.
     * @return True if the assignment is valid, false otherwise.
     */
    private boolean isValidAssignment(
            FamilyMember santa,
            FamilyMember recipient,
            Map<Long, Set<Long>> recentAssignments,
            BitSet assigned,
            List<FamilyMember> members
    ) {
        if (isSelfAssignment(santa, recipient)) {
            return false;
        }

        if (isRecipientAlreadyAssigned(recipient, assigned, members)) {
            return false;
        }

        if (isImmediateFamily(santa, recipient)) {
            return false;
        }

        return !hasRecentlyGivenTo(santa, recipient, recentAssignments);
    }

    /**
     * Checks if the assignment is a self-assignment.
     *
     * @param santa     The family member acting as Santa.
     * @param recipient The family member receiving the gift.
     * @return True if the assignment is a self-assignment, false otherwise.
     */
    private boolean isSelfAssignment(FamilyMember santa, FamilyMember recipient) {
        return santa.getId().equals(recipient.getId());
    }

    /**
     * Checks if the recipient has already been assigned.
     *
     * @param recipient The family member receiving the gift.
     * @param assigned  A BitSet tracking assigned recipients.
     * @param members   The list of family members.
     * @return True if the recipient has already been assigned, false otherwise.
     */
    private boolean isRecipientAlreadyAssigned(FamilyMember recipient, BitSet assigned, List<FamilyMember> members) {
        return assigned != null && assigned.get(members.indexOf(recipient));
    }

    /**
     * Checks if the assignment is within the immediate family.
     *
     * @param santa     The family member acting as Santa.
     * @param recipient The family member receiving the gift.
     * @return True if the assignment is within the immediate family, false otherwise.
     */
    private boolean isImmediateFamily(FamilyMember santa, FamilyMember recipient) {
        boolean sameFamily = Objects.equals(santa.getFamilyId(), recipient.getFamilyId());
        boolean hasImmediateFamilyEdge = santa.getRelations().stream()
                .anyMatch(edge -> edge.getType() == RelationType.IMMEDIATE_FAMILY);

        return sameFamily || hasImmediateFamilyEdge;
    }

    /**
     * Checks if the Santa has recently given a gift to the recipient.
     *
     * @param santa             The family member acting as Santa.
     * @param recipient         The family member receiving the gift.
     * @param recentAssignments A map of recent assignments.
     * @return True if the Santa has recently given a gift to the recipient, false otherwise.
     */
    private boolean hasRecentlyGivenTo(FamilyMember santa, FamilyMember recipient, Map<Long, Set<Long>> recentAssignments) {
        Set<Long> previousRecipients = recentAssignments.getOrDefault(santa.getId(), Collections.emptySet());
        return previousRecipients.contains(recipient.getId());
    }
}
