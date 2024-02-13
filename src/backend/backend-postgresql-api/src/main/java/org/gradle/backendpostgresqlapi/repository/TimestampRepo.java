package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.gradle.backendpostgresqlapi.util.TableNameUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface TimestampRepo extends JpaRepository<Timestamp, Long> {

    String CREATE_INDEX_SQL = 
    "CREATE INDEX IF NOT EXISTS t_timestamp_idx ON " + TableNameUtil.TIMESTAMPS + 
    " (t_timestamp)";
    String GET_MAX_ONE_DUPLICATE = 
    "SELECT COUNT(*) " + 
    "FROM " + TableNameUtil.TIMESTAMPS + 
    " WHERE t_pp_id = :id AND t_timestamp = :timestamp LIMIT 1";

    @Modifying
    @Query(value = CREATE_INDEX_SQL, nativeQuery = true)
    void createDbIndex();

    List<Timestamp> getAllTimestampsByParkingPointId(long parkingPointId);

    @Query(value = GET_MAX_ONE_DUPLICATE, nativeQuery = true)
    int getMaxOneDuplicate(@Param("id") long parkingPointId, @Param("timestamp") String timestamp);
}
