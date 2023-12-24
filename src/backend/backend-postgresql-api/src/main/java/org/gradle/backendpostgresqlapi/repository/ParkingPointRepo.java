package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.util.TableNameUtil;
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
public interface ParkingPointRepo extends JpaRepository<ParkingPoint, Long> {

    String CREATE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS pp_coordinates_idx ON " + TableNameUtil.PARKING_POINTS
        + " USING GIST (pp_coordinates)";
    String GET_DUPLICATE_ID = "SELECT pp_id FROM " + TableNameUtil.PARKING_POINTS +
        " WHERE ST_Equals(cast(pp_coordinates as geometry), ST_GeomFromText(:point, 4326)) LIMIT 1";

    @Modifying
    @Query(value = CREATE_INDEX_SQL, nativeQuery = true)
    void createIndex();

    List<ParkingPoint> getParkingPointsByEditedParkingSpaceId(long editedParkingSpaceId);

    @Query(value = GET_DUPLICATE_ID, nativeQuery = true)
    Optional<Long> getIdOfDuplicateByCoordinates(@Param("point") String pointToCompareWith);
}
