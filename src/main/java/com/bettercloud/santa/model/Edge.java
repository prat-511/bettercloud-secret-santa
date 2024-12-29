package com.bettercloud.santa.model;

import lombok.NoArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@Table("edges")
public class Edge {
    @Id
    private Long edgeId;

    @Column("type")
    private RelationType type;

    @Column("member_id")
    private Long memberId;

    public Edge(RelationType type, Long memberId) {
        this.type = type;
        this.memberId = memberId;
    }
}
