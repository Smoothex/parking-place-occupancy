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
public interface EditedParkingSpaceRepo extends JpaRepository<EditedParkingSpace, Long> {

    String UPDATE_AREA_SQL = "UPDATE edited_parking_spaces SET edit_area = ROUND(CAST(ST_AREA(edit_coordinates) AS NUMERIC),2) WHERE edit_id = :id";

    @Modifying
    @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
    void updateAreaColumnById(@Param("id") long id);

    List<EditedParkingSpace> findByOccupied(boolean occupied);

    boolean existsByParkingSpaceId(long id);
}
