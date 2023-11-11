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

@Repository
@Transactional
public interface GeospatialRepo extends JpaRepository<ParkingSpace, Integer> {

    String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS parking_spaces (ps_id SERIAL PRIMARY KEY, ps_coordinates GEOGRAPHY(POLYGON, 4326), ps_occupied BOOLEAN DEFAULT 'f')";
    String CREATE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS ps_coordinates_idx ON parking_spaces USING GIST (ps_coordinates)";
    String INSERT_GEOJSON_SQL = "INSERT INTO parking_spaces (ps_coordinates) VALUES (ST_GeomFromGeoJSON(:parkSpace))";
    String SELECT_PARKING_SPACES_SQL = "SELECT ps_id, ST_AsGeoJSON(ps_coordinates) AS ps_coordinates, ps_occupied FROM parking_spaces";
    String SELECT_PARKING_SPACE_BY_ID_SQL = "SELECT ps_id, ST_AsGeoJSON(ps_coordinates) AS ps_coordinates, ps_occupied FROM parking_spaces WHERE ps_id = :id";

    @Modifying
    @Query(value = CREATE_TABLE_SQL, nativeQuery = true)
    void createTable();

    @Modifying
    @Query(value = CREATE_INDEX_SQL, nativeQuery = true)
    void createIndex();

    @Modifying
    @Query(value = INSERT_GEOJSON_SQL, nativeQuery = true)
    void insertParkingSpace(@Param("parkSpace") String parkingSpace);

    @Query(value = SELECT_PARKING_SPACES_SQL, nativeQuery = true)
    List<ParkingSpace> getAllParkingSpaces();

    @Query(value = SELECT_PARKING_SPACE_BY_ID_SQL, nativeQuery = true)
    Optional<ParkingSpace> findById(@Param("id") Integer id);

    List<ParkingSpace> findByOccupied(boolean occupied);

}
