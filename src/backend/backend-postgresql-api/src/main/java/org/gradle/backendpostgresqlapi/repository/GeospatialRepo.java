package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.locationtech.jts.geom.Polygon;

@Repository
@Transactional
public interface GeospatialRepo extends JpaRepository<ParkingSpace, Integer> {

    String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS parking_spaces (" +
                                "ps_id SERIAL PRIMARY KEY, " +
                                "ps_coordinates GEOGRAPHY(POLYGON, 4326), " +
                                "ps_occupied BOOLEAN DEFAULT FALSE, " +
                                "ps_area DOUBLE PRECISION, " +
                                "ps_capacity INTEGER, " +
                                "ps_position VARCHAR(255)" +   // in Java program presented as enum, but still german words in postgres
                            ")";

    String CREATE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS ps_coordinates_idx ON parking_spaces USING GIST (ps_coordinates)";
    String UPDATE_AREA_SQL = "UPDATE parking_spaces SET ps_area = ROUND(CAST(ST_AREA(ps_coordinates) AS NUMERIC),2) WHERE ps_area = 0.0";

    @Modifying
    @Query(value = CREATE_TABLE_SQL, nativeQuery = true)
    void createTable();

    @Modifying
    @Query(value = CREATE_INDEX_SQL, nativeQuery = true)
    void createIndex();

    @Modifying
    @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
    void updateAreaColumn();

    @Modifying
    default void insertParkingSpaceFromPolygon(Polygon polygon) {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setPolygon(polygon);
        saveParkingSpace(parkingSpace);
    }

    @Modifying
    default void saveParkingSpace(ParkingSpace parkingSpace) {
        this.save(parkingSpace);
    }

    List<ParkingSpace> findByOccupied(boolean occupied);
}
