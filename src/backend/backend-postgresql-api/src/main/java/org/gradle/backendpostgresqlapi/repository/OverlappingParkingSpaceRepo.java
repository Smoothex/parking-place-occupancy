package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.OverlappingParkingSpace;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
