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

    String CREATE_EDITED_DATA_TABLE_SQL = "CREATE TABLE IF NOT EXISTS edited_parking_spaces (" +
                "eps_id SERIAL PRIMARY KEY, " +
                "eps_ps_id INTEGER, " +
                "eps_coordinates GEOGRAPHY(POLYGON, 4326), " +
                "eps_occupied BOOLEAN DEFAULT FALSE, " +
                "eps_area DOUBLE PRECISION, " +
                "eps_capacity INTEGER, " +
                "eps_position VARCHAR(255)" +   // in backend presented as enum
                ")";

    String UPDATE_AREA_SQL = "UPDATE edited_parking_spaces SET eps_area = ROUND(CAST(ST_AREA(eps_coordinates) AS NUMERIC),2) WHERE eps_id = :id";

    @Modifying
    @Query(value = CREATE_EDITED_DATA_TABLE_SQL, nativeQuery = true)
    void createEditedDataTable();

    @Modifying
    @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
    void updateAreaColumnById(@Param("id") int id);

    List<EditedParkingSpace> findByOccupied(boolean occupied);
}
