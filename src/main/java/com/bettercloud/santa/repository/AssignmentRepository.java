package com.bettercloud.santa.repository;

import com.bettercloud.santa.model.FamilyAssignment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AssignmentRepository extends R2dbcRepository<FamilyAssignment, Long> {

    /**
     * Finds FamilyAssignment records where the assignment year is between the specified start and end years.
     *
     * @param startYear The start year of the range.
     * @param endYear   The end year of the range.
     * @return A Flux of FamilyAssignment objects that match the criteria.
     */
    @Query("SELECT * FROM assignments WHERE assignment_year BETWEEN :startYear AND :endYear")
    Flux<FamilyAssignment> findByYearsBetween(
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear);
}
