package com.bettercloud.santa.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@Table("assignments")
public class FamilyAssignment {
    @Id
    private Long id;
    private Integer assignmentYear;

    @Column("giver_id")
    private Long santaId;

    @Column("receiver_id")
    private Long recipientId;

    @Transient
    private FamilyMember santa;

    @Transient
    private FamilyMember recipient;

    public FamilyAssignment(Integer assignmentYear, Long santaId, Long recipientId) {
        this.assignmentYear = assignmentYear;
        this.santaId = santaId;
        this.recipientId = recipientId;
    }
}
