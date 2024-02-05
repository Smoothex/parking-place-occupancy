package org.gradle.backendpostgresqlapi.repository;

import java.util.List;
import java.util.Optional;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.util.TableNameUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.locationtech.jts.geom.Point;

@Repository
@Transactional
public interface ParkingSpaceRepo extends JpaRepository<ParkingSpace, Long> {

    String CREATE_MAIN_DATA_INDEX_SQL = 
    "CREATE INDEX IF NOT EXISTS ps_centroid_idx ON " + TableNameUtil.PARKING_SPACES + 
    " USING GIST (ps_centroid)";

    String UPDATE_AREA_SQL = 
    "UPDATE " + TableNameUtil.PARKING_SPACES +
    " SET ps_area = ROUND(CAST(ST_AREA(ps_coordinates) AS NUMERIC),2) " +
    "WHERE ps_id = :id";

    String FIND_ONE_DUPLICATE_POLYGON_BY_CENTROID = 
    "SELECT COUNT(*) FROM " + TableNameUtil.PARKING_SPACES +
    " WHERE ST_Equals(CAST(ps_centroid AS GEOMETRY), ST_GeomFromText(:centroidToCompareWith, 4326)) " +
    "LIMIT 1";

    String FIND_CLOSEST_PARKING_SPACES_BY_CENTROID = 
    "SELECT * FROM " + TableNameUtil.PARKING_SPACES +
    " WHERE ST_Distance(ST_GeomFromText(:point, 4326), CAST(ps_centroid AS GEOMETRY)) <= 10 " +
    "ORDER BY ST_Distance(ST_GeomFromText(:point, 4326), CAST(ps_centroid AS GEOMETRY)) " +
    "ASC LIMIT 3";

    String GET_INTERSECTION_AREA_OF_TWO_POLYGONS = 
    "SELECT ST_Area(ST_Intersection(ps_coordinates, ST_GeomFromText(:polygon, 4326))) / ps_area * 100 " +
    "FROM " + TableNameUtil.PARKING_SPACES + 
    " WHERE ps_id=:existing_id";

    String CALCULATE_CENTROID_FOR_POLYGON = 
    "SELECT ST_AsGeoJSON(ST_Centroid(ST_GeomFromText(:polygon, 4326)))";

    String GET_PARKING_SPACE_ID_BY_POINT_WITHIN = 
    "SELECT ps_id FROM " + TableNameUtil.PARKING_SPACES +
    " WHERE ST_Contains(CAST(ps_coordinates AS GEOMETRY), ST_GeomFromText(:pointWithin, 4326)) " +
    "LIMIT 1";

    @Modifying
    @Query(value = CREATE_MAIN_DATA_INDEX_SQL, nativeQuery = true)
    void createMainDataIndex();

    @Modifying
    @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
    void updateAreaColumn(@Param("id") Long parkingSpaceId);

    @Modifying
    default ParkingSpace insertParkingSpace(ParkingSpace parkingSpace, Point centroid) {
        parkingSpace.setCentroid(centroid);
        return this.save(parkingSpace);
    }

    @Query(value = CALCULATE_CENTROID_FOR_POLYGON, nativeQuery = true)
    String calculateCentroidForPolygon(@Param("polygon") String polygon);

    @Query(value = FIND_ONE_DUPLICATE_POLYGON_BY_CENTROID, nativeQuery = true)
    long findOneDuplicatePolygonByCentroid(@Param("centroidToCompareWith") String centroidToCompareWith);

    @Query(value = GET_INTERSECTION_AREA_OF_TWO_POLYGONS, nativeQuery = true)
    double findIntersectionAreaOfTwoPolygons(@Param("polygon") String polygon, @Param("existing_id") Long existingPolygonId);

    @Query(value = FIND_CLOSEST_PARKING_SPACES_BY_CENTROID, nativeQuery = true)
    List<ParkingSpace> findClosestParkingSpacesByCentroid(@Param("point") String point);

    @Query(value = GET_PARKING_SPACE_ID_BY_POINT_WITHIN, nativeQuery = true)
    Optional<Long> getParkingSpaceIdByPointWithin(@Param("pointWithin") String pointWithin);
}
