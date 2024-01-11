package org.gradle.backendpostgresqlapi.repository;

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
}
