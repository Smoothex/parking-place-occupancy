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

    String CREATE_MAIN_DATA_INDEX_SQL = "CREATE INDEX IF NOT EXISTS ps_coordinates_idx ON " + TableNameUtil.PARKING_SPACES
        + " USING GIST (ps_coordinates)";
    String UPDATE_AREA_SQL = "UPDATE " + TableNameUtil.PARKING_SPACES +
        " SET ps_area = ROUND(CAST(ST_AREA(ps_coordinates) AS NUMERIC),2) WHERE ps_area = 0.0";
    String COUNT_NUM_OF_POLYGONS = "SELECT COUNT(*) FROM " + TableNameUtil.PARKING_SPACES
        + " WHERE ST_Equals(cast(ps_coordinates as geometry), ST_GeomFromText(:polygonToCompareWith, 4326)) LIMIT 1";
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

    String FIND_CLOSEST_CENTROIDS = "SELECT ST_AsText(ST_Centroid(CAST(ps_coordinates AS GEOMETRY))) AS centroid " +
                                    "FROM " + TableNameUtil.PARKING_SPACES +
                                    " WHERE ST_Distance(ST_GeomFromText(:point, 4326), ST_Centroid(CAST(ps_coordinates AS GEOMETRY))) <= 10 " +
                                    "ORDER BY ST_Distance(ST_GeomFromText(:point, 4326), ST_Centroid(CAST(ps_coordinates AS GEOMETRY))) ASC";

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

    @Query(value = GET_CENTROID_BY_POLYGON, nativeQuery = true)
    Point getCentroidByPolygon(@Param("polygon") String polygon);

    @Query(value = FIND_CLOSEST_CENTROIDS, nativeQuery = true)
    List<String> findClosestCentroids(@Param("point") String point);

    @Query(value = GET_POLYGON_BY_CENTROID, nativeQuery = true)
    String findPolygonByCentroid(@Param("centroid") String centroid);

    @Query(value = GET_OVERLAPPING_PARTS_OF_TWO_POLYGONS, nativeQuery = true)
    List<Object[]> findOverlapsByPolygon(@Param("oldPolygon") String oldPolygon, @Param("newPolygon") String newPolygon);
}
