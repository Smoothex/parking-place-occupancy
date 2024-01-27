package org.gradle.backendpostgresqlapi.repository;

import java.math.BigDecimal;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.OverlappingParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.util.TableNameUtil;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface OverlappingParkingSpaceRepo extends JpaRepository<OverlappingParkingSpace, Long> {
        
    @Modifying
    default void insertOverlappingParkingSpaceFromPolygon(Polygon polygon) {
        OverlappingParkingSpace overlappingParkingSpace = new OverlappingParkingSpace();
        overlappingParkingSpace.setPolygon(polygon);
        this.save(overlappingParkingSpace);
    }
}
