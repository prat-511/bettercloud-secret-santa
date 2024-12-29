package com.bettercloud.santa.service;

import com.bettercloud.santa.model.FamilyAssignment;
import com.bettercloud.santa.model.FamilyMember;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AssignmentStrategy {
    List<FamilyAssignment> generateAssignments(
            Integer year,
            List<FamilyMember> members,
            Map<Long, Set<Long>> recentAssignments
    );
}
