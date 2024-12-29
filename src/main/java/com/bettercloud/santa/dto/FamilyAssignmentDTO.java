package com.bettercloud.santa.dto;

import com.bettercloud.santa.model.FamilyAssignment;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FamilyAssignmentDTO {
    private Long id;
    private Integer assignmentYear;
    private Long santaId;
    private String santaName;
    private Long recipientId;
    private String recipientName;

    public static FamilyAssignmentDTO from(FamilyAssignment assignment) {
        FamilyAssignmentDTO dto = new FamilyAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setAssignmentYear(assignment.getAssignmentYear());
        dto.setSantaId(assignment.getSantaId());
        dto.setRecipientId(assignment.getRecipientId());

        if (assignment.getSanta() != null) {
            dto.setSantaName(assignment.getSanta().getName());
        }

        if (assignment.getRecipient() != null) {
            dto.setRecipientName(assignment.getRecipient().getName());
        }

        return dto;
    }
}
