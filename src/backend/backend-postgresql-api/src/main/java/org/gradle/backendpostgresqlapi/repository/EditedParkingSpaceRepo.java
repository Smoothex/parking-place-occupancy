package org.gradle.backendpostgresqlapi.repository;

import java.math.BigDecimal;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
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
public interface EditedParkingSpaceRepo extends JpaRepository<EditedParkingSpace, Long> {

        String UPDATE_AREA_SQL = "UPDATE " + TableNameUtil.EDITED_PARKING_SPACES
                        + " SET edit_area = ROUND(CAST(ST_AREA(edit_coordinates) AS NUMERIC),2) WHERE edit_id = :id";
        String GET_ID_BY_POINT = "SELECT edit_id FROM " + TableNameUtil.EDITED_PARKING_SPACES
                        + " WHERE ST_Contains(cast(edit_coordinates as GEOMETRY), ST_GeomFromText(:pointWithin, 4326)) LIMIT 1";
        String GET_NEIGHBORS = "SELECT p2.edit_id FROM " + TableNameUtil.EDITED_PARKING_SPACES + " p1 " +
                                "JOIN " + TableNameUtil.EDITED_PARKING_SPACES + " p2 " +
                                "ON p1.edit_id = :id AND p1.edit_id <> p2.edit_id " +
                                "AND ST_Touches(CAST(p1.edit_coordinates AS GEOMETRY), CAST(p2.edit_coordinates AS GEOMETRY))";

        // GET POINTS ON THE EDGE OF THE POLYGON
        // SELECT edit_id, edit_area, edit_capacity, edit_occupied, edit_ps_id,
        // ST_AsText(edit_coordinates) AS wkt_coordinates
        // FROM public.edited_parking_spaces
        // WHERE ST_Intersects(ST_ExteriorRing(edit_coordinates::geometry),
        // ST_GeomFromText('POINT(13.360991062697591 52.545194996101614)', 4326));

        @Modifying
        @Query(value = UPDATE_AREA_SQL, nativeQuery = true)
        void updateAreaColumnById(@Param("id") long id);

        List<EditedParkingSpace> findByOccupied(boolean occupied);

        boolean existsByParkingSpaceId(long id);

        @Query(value = GET_ID_BY_POINT, nativeQuery = true)
        Optional<Long> getIdByPointWithin(@Param("pointWithin") String pointWithin);

        @Query(value = GET_NEIGHBORS, nativeQuery = true)
        List<Long> findNeighborIds(@Param("id") Long id);


}
