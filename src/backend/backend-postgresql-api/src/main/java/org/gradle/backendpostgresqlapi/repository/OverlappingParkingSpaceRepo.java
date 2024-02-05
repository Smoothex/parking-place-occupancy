package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.OverlappingParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.util.TableNameUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public interface OverlappingParkingSpaceRepo extends JpaRepository<OverlappingParkingSpace, Long> {

    String CREATE_DB_INDEX_SQL = 
    "CREATE INDEX IF NOT EXISTS ops_coordinates_idx " + 
    "ON " + TableNameUtil.OVERLAPPING_PARKING_SPACES + 
    " USING GIST (ops_coordinates)";

    String GET_OVERLAPPING_PARKING_SPACE_BY_POINT_WITHIN = 
    "SELECT * " + 
    "FROM " + TableNameUtil.OVERLAPPING_PARKING_SPACES + 
    " WHERE ST_Contains(CAST(ops_coordinates AS GEOMETRY), ST_GeomFromText(:pointWithin, 4326)) " + 
    "LIMIT 1";

    @Modifying
    @Query(value = CREATE_DB_INDEX_SQL, nativeQuery = true)
    void initializeDbIndex();

    @Query(value = GET_OVERLAPPING_PARKING_SPACE_BY_POINT_WITHIN, nativeQuery = true)
    Optional<OverlappingParkingSpace> getOverlappingParkingSpaceByPointWithin(@Param("pointWithin") String pointWithin);

    @Modifying
    default void insertParkingSpace(ParkingSpace newParkingSpace, ParkingSpace existingParkingSpace) {
        OverlappingParkingSpace overlappingParkingSpace = new OverlappingParkingSpace();
        overlappingParkingSpace.setPolygon(newParkingSpace.getPolygon());
        overlappingParkingSpace.setAssignedParkingSpace(existingParkingSpace);

        if (newParkingSpace.getCapacity() != null) {
            overlappingParkingSpace.setCapacity(newParkingSpace.getCapacity());
        }
        if (newParkingSpace.getPosition() != null) {
            overlappingParkingSpace.setPosition(newParkingSpace.getPosition());
        }

        this.save(overlappingParkingSpace);
    }
}
