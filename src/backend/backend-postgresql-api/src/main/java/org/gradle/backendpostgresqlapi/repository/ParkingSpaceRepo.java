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
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Repository
@Transactional
public interface ParkingSpaceRepo extends JpaRepository<ParkingSpace, Long> {

    String CREATE_MAIN_DATA_INDEX_SQL = "CREATE INDEX IF NOT EXISTS ps_centroid_idx ON " + TableNameUtil.PARKING_SPACES
        + " USING GIST (ps_centroid)";

    String UPDATE_AREA_SQL = "UPDATE " + TableNameUtil.PARKING_SPACES +
        " SET ps_area = ROUND(CAST(ST_AREA(ps_coordinates) AS NUMERIC),2) WHERE ps_id = :id";

    String FIND_ONE_DUPLICATE_POLYGON_BY_CENTROID = "SELECT COUNT(*) FROM " + TableNameUtil.PARKING_SPACES
        + " WHERE ST_Equals(CAST(ps_centroid AS GEOMETRY), ST_GeomFromText(:centroidToCompareWith, 4326)) LIMIT 1";

    String FIND_CLOSEST_PARKING_SPACES_BY_CENTROID = "SELECT * FROM " + TableNameUtil.PARKING_SPACES +
        " WHERE ST_Distance(ST_GeomFromText(:point, 4326), CAST(ps_centroid AS GEOMETRY)) <= 10 " +
        "ORDER BY ST_Distance(ST_GeomFromText(:point, 4326), CAST(ps_centroid AS GEOMETRY)) ASC LIMIT 3";

    String GET_INTERSECTION_AREA_OF_TWO_POLYGONS = "SELECT " +
        "ST_Area(ST_Intersection(ps_coordinates, ST_GeomFromText(:polygon, 4326))) / ps_area * 100" +
        " FROM " + TableNameUtil.PARKING_SPACES + " WHERE ps_id=:existing_id";

    String CALCULATE_CENTROID_FOR_POLYGON = "SELECT ST_AsGeoJSON(ST_Centroid(ST_GeomFromText(:polygon, 4326)))";

    String GET_PARKING_SPACE_ID_BY_POINT_WITHIN = "SELECT ps_id FROM " + TableNameUtil.PARKING_SPACES
        + " WHERE ST_Contains(CAST(ps_coordinates AS GEOMETRY), ST_GeomFromText(:pointWithin, 4326)) LIMIT 1";

    // todo remove redundant code
    String CHECK_FOR_OVERLAPS = 
        "SELECT  edit_id, " +
                "ROUND(CAST(ST_Area(ST_Intersection(CAST(edit_coordinates AS GEOMETRY), (SELECT edit_coordinates FROM public.edited_parking_spaces WHERE edit_id = :id))) AS NUMERIC), 2) AS overlap_area, " + //
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
    String TEST_POLYGON = "POLYGON((13.372025189826015 52.55565700904496, 13.372027848995522 52.55566115830258, 13.37200636560613 52.555675856061384, 13.372011490057442 52.555683729674314, 13.372016523756699 52.555691395237865, 13.372021494445649 52.55569889099473, 13.372026447692448 52.55570629612042, 13.372031428417475 52.55571366046074, 13.372033929792536 52.555717318704744, 13.372070699510417 52.55570800474592, 13.372068198132792 52.55570434650266, 13.372063218154732 52.555696983265186, 13.372058266393319 52.555689580328284, 13.37205329942109 52.55568209003426, 13.372048274364465 52.55567443717892, 13.372043165112391 52.55566658596133, 13.37203802050229 52.5556586050404, 13.372035361331967 52.555654455783, 13.372025189826015 52.55565700904496))";

    String GET_POLYGON_BY_CENTROID = "SELECT ST_AsText(ps_coordinates) FROM " + TableNameUtil.PARKING_SPACES + " WHERE ST_AsText(ST_Centroid(CAST(ps_coordinates AS GEOMETRY))) = :centroid";

    String GET_CENTROID_BY_POLYGON = "SELECT ST_AsText(ST_Centroid(ST_GeomFromText(ps_coordinates, 4326))) FROM " + TableNameUtil.PARKING_SPACES + " WHERE ST_AsText(ps_coordinates) = :polygon";

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

    // todo please remove them
    @Query(value = CHECK_FOR_OVERLAPS, nativeQuery = true)
    List<Object[]> findOverlappingSpacesById(@Param("id") Long id);

    @Query(value = CHECK_FOR_OVERLAPS_BY_POLYGON, nativeQuery = true)
    List<Object[]> findOverlappingSpacesByPolygon(@Param("polygon") Polygon polygon);

    @Query(value = GET_AREA_BY_ID, nativeQuery = true)
    Optional<BigDecimal> findAreaById(@Param("id") Long id);

    @Query(value = GET_CENTROID_BY_POLYGON, nativeQuery = true)
    Point getCentroidByPolygon(@Param("polygon") String polygon);

    @Query(value = GET_POLYGON_BY_CENTROID, nativeQuery = true)
    String findPolygonByCentroid(@Param("centroid") String centroid);

    @Query(value = GET_OVERLAPPING_PARTS_OF_TWO_POLYGONS, nativeQuery = true)
    List<Object[]> findOverlapsByPolygon(@Param("oldPolygon") String oldPolygon, @Param("newPolygon") String newPolygon);
}
