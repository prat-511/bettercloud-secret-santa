package com.bettercloud.santa.repository;

import com.bettercloud.santa.model.FamilyMember;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MemberRepository extends R2dbcRepository<FamilyMember, Long> {

    /**
     * Finds all FamilyMember records with their relations.
     *
     * @return A Flux of FamilyMember objects with their relations.
     */
    @Query("SELECT DISTINCT m.*, e.edge_id, e.type FROM members m LEFT JOIN edges e ON m.id = e.member_id")
    Flux<FamilyMember> findAllWithRelations();
}
