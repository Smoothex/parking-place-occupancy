package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Polygon;

@Repository
@Transactional
public interface GeospatialRepo extends JpaRepository<ParkingSpace, Integer> {

    String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS parking_spaces (ps_id SERIAL PRIMARY KEY, ps_coordinates GEOGRAPHY(POLYGON, 4326), ps_occupied BOOLEAN DEFAULT 'f', ps_area REAL)";
    String CREATE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS ps_coordinates_idx ON parking_spaces USING GIST (ps_coordinates)";

    @Modifying
    @Query(value = CREATE_TABLE_SQL, nativeQuery = true)
    void createTable();

    @Modifying
    @Query(value = CREATE_INDEX_SQL, nativeQuery = true)
    void createIndex();

    @Modifying
    default void insertParkingSpace(Polygon polygon) {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setPolygon(polygon);
        parkingSpace.setOccupied(false);
        this.save(parkingSpace);
    }

    List<ParkingSpace> findByOccupied(boolean occupied);

}
