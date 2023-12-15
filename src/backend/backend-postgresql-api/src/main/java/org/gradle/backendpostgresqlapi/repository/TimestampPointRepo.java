package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.TimestampPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface TimestampPointRepo extends JpaRepository<TimestampPoint, Long> {

    String CREATE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS tp_coordinates_idx ON timestamp_points USING GIST (tp_coordinates)";
    String GET_MAX_ONE_DUPLICATE = "SELECT COUNT(*) FROM timestamp_points WHERE tp_timestamp = :timestamp" +
        " AND ST_Equals(cast(tp_coordinates as geometry), ST_GeomFromText(:point, 4326)) LIMIT 1";

    @Modifying
    @Query(value = CREATE_INDEX_SQL, nativeQuery = true)
    void createIndex();

    List<TimestampPoint> getTimestampPointsByEditedParkingSpaceId(long editedParkingSpaceId);

    @Query(value = GET_MAX_ONE_DUPLICATE, nativeQuery = true)
    long getMaxOneDuplicate(@Param("timestamp") String timestampToCompareWith, @Param("point") String pointToCompareWith);
}
