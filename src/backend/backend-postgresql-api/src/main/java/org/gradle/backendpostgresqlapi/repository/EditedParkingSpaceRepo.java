package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface EditedParkingSpaceRepo extends JpaRepository<EditedParkingSpace, Long> {

    String UPDATE_AREA_SQL = "UPDATE edited_parking_spaces SET edit_area = ROUND(CAST(ST_AREA(edit_coordinates) AS NUMERIC),2) WHERE edit_id = :id";
    String GET_ID_BY_POINT = "SELECT edit_id FROM edited_parking_spaces WHERE ST_Contains(cast(edit_coordinates as geometry), ST_GeomFromText(:pointWithin, 4326)) LIMIT 1";

    @Modifying
    @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
    void updateAreaColumnById(@Param("id") long id);

    List<EditedParkingSpace> findByOccupied(boolean occupied);

    boolean existsByParkingSpaceId(long id);

    @Query(value = GET_ID_BY_POINT, nativeQuery = true)
    Optional<Long> getIdByPointWithin(@Param("pointWithin") String pointWithin);
}
