package org.gradle.backendpostgresqlapi.repository;

import java.math.BigDecimal;
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

import org.locationtech.jts.geom.Polygon;

@Repository
@Transactional
public interface ParkingSpaceRepo extends JpaRepository<ParkingSpace, Long> {

    String CREATE_MAIN_DATA_INDEX_SQL = "CREATE INDEX IF NOT EXISTS ps_coordinates_idx ON " + TableNameUtil.PARKING_SPACES
        + " USING GIST (ps_coordinates)";
    String UPDATE_AREA_SQL = "UPDATE " + TableNameUtil.PARKING_SPACES +
        " SET ps_area = ROUND(CAST(ST_AREA(ps_coordinates) AS NUMERIC),2) WHERE ps_area = 0.0";
    String COUNT_NUM_OF_POLYGONS = "SELECT COUNT(*) FROM " + TableNameUtil.PARKING_SPACES
        + " WHERE ST_Equals(cast(ps_coordinates as geometry), ST_GeomFromText(:polygonToCompareWith, 4326)) LIMIT 1";
    String CHECK_FOR_OVERLAPS = 
        "SELECT  edit_id, " +
                "ROUND(CAST(ST_Area(ST_Intersection(CAST(edit_coordinates AS GEOMETRY), (SELECT edit_coordinates FROM public.edited_parking_spaces WHERE edit_id = 1255))) AS NUMERIC), 2) AS overlap_area, " + //
                "ST_AsText(edit_coordinates) " + 
        "FROM  " + TableNameUtil.EDITED_PARKING_SPACES + 
        " WHERE ST_Intersects(CAST(edit_coordinates AS GEOMETRY), (SELECT edit_coordinates FROM public.edited_parking_spaces WHERE edit_id = :id))" + 
        "AND edit_id <> :id " + 
        "AND ST_Area(ST_Intersection(CAST(edit_coordinates AS GEOMETRY), (SELECT edit_coordinates FROM public.edited_parking_spaces WHERE edit_id = :id))) > 0;";

    String CHECK_FOR_OVERLAPS_BY_POLYGON = 
        "SELECT  edit_id, " +
                "ROUND(CAST(ST_Area(ST_Intersection(CAST(edit_coordinates AS GEOMETRY), ST_GeomFromText(:polygon, 4326))) AS NUMERIC), 2) AS overlap_area, " +
                "ST_AsText(edit_coordinates) " + 
        "FROM  " + TableNameUtil.EDITED_PARKING_SPACES + 
        " WHERE ST_Intersects(CAST(edit_coordinates AS GEOMETRY), ST_GeomFromText(:polygon, 4326))" + 
        "AND ST_Area(ST_Intersection(CAST(edit_coordinates AS GEOMETRY), ST_GeomFromText(:polygon, 4326))) > 0;";

    String GET_OVERLAPPING_PARTS_OF_TWO_POLYGONS =
        "SELECT  edit_id, " +
                "ROUND(CAST(ST_Area(ST_Intersection(ST_GeomFromText(:oldPolygon, 4326), ST_GeomFromText(:newPolygon, 4326))) AS NUMERIC), 2) AS overlap_area, " +
                "ST_AsText(edit_coordinates) " + 
        "FROM  " + TableNameUtil.EDITED_PARKING_SPACES + 
        " WHERE ST_Intersects(ST_GeomFromText(:oldPolygon, 4326), ST_GeomFromText(:newPolygon, 4326))" + 
        "AND ST_Area(ST_Intersection(ST_GeomFromText(:oldPolygon, 4326), ST_GeomFromText(:newPolygon, 4326))) > 0;";

    String GET_AREA_BY_ID = "SELECT ROUND(CAST(ST_Area(CAST(edit_coordinates AS GEOMETRY)) AS NUMERIC), 2) FROM " + TableNameUtil.EDITED_PARKING_SPACES + " WHERE edit_id = :id";
    
    String FIND_POLYGON_BY_CENTROID = "SELECT edit_coordinates FROM " + TableNameUtil.EDITED_PARKING_SPACES + " WHERE ST_AsText(ST_Centroid(CAST(edit_coordinates AS GEOMETRY))) = :centroid";

    String GET_CENTROID_BY_ID = "SELECT ST_AsText(ST_Centroid(CAST(edit_coordinates AS GEOMETRY))) FROM " + TableNameUtil.EDITED_PARKING_SPACES + " WHERE edit_id = :id";

    String FIND_CLOSEST_CENTROIDS = "SELECT ST_AsText(ST_Centroid(CAST(edit_coordinates AS GEOMETRY))) AS centroid " +
                                    "FROM " + TableNameUtil.EDITED_PARKING_SPACES + " " +
                                    "ORDER BY ST_Distance(ST_GeomFromText(:point, 4326), ST_Centroid(CAST(edit_coordinates AS GEOMETRY))) ASC " +
                                    "LIMIT 3";

    @Modifying
    @Query(value = CREATE_MAIN_DATA_INDEX_SQL, nativeQuery = true)
    void createMainDataIndex();

    @Modifying
    @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
    void updateAreaColumn();

    @Modifying
    default void insertParkingSpaceFromPolygon(Polygon polygon) {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setPolygon(polygon);
        this.save(parkingSpace);
    }

    @Query(value = COUNT_NUM_OF_POLYGONS, nativeQuery = true)
    long countSamePolygons(@Param("polygonToCompareWith") String polygonToCompareWith);

    @Query(value = CHECK_FOR_OVERLAPS, nativeQuery = true)
    List<Object[]> findOverlappingSpacesById(@Param("id") Long id);

    @Query(value = CHECK_FOR_OVERLAPS_BY_POLYGON, nativeQuery = true)
    List<Object[]> findOverlappingSpacesByPolygon(@Param("polygon") Polygon polygon);

    @Query(value = GET_AREA_BY_ID, nativeQuery = true)
    Optional<BigDecimal> findAreaById(@Param("id") Long id);

    @Query(value = GET_CENTROID_BY_ID, nativeQuery = true)
    Optional<String> findCentroidById(@Param("id") Long id);

    @Query(value = FIND_CLOSEST_CENTROIDS, nativeQuery = true)
    List<String> findClosestCentroids(@Param("point") String point);

    @Query(value = FIND_POLYGON_BY_CENTROID, nativeQuery = true)
    Optional<String> findPolygonByCentroid(@Param("centroid") String centroid);

    @Query(value = GET_OVERLAPPING_PARTS_OF_TWO_POLYGONS, nativeQuery = true)
    List<Object[]> findOverlapsByPolygon(@Param("oldPolygon") String oldPolygon, @Param("newPolygon") String newPolygon);
}
