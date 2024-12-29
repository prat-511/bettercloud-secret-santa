package com.bettercloud.santa.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@Table("members")
public class FamilyMember {
    @Id
    private Long id;
    private Integer familyId;
    private String name;

    @Transient
    private List<Edge> relations = new ArrayList<>();

    public FamilyMember(Long id, Integer familyId, String name) {
        this.id = id;
        this.familyId = familyId;
        this.name = name;
    }
}
