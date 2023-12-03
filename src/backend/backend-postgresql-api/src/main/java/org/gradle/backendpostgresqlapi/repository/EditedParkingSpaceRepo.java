package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface EditedParkingSpaceRepo extends JpaRepository<EditedParkingSpace, Integer> {

    String UPDATE_AREA_SQL = "UPDATE edited_parking_spaces SET еdit_area = ROUND(CAST(ST_AREA(еdit_coordinates) AS NUMERIC),2) WHERE еdit_id = :id";

    @Modifying
    @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
    void updateAreaColumnById(@Param("id") int id);

    List<EditedParkingSpace> findByOccupied(boolean occupied);
}
