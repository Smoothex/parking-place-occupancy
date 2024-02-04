package org.gradle.backendpostgresqlapi.repository;

import org.gradle.backendpostgresqlapi.entity.OverlappingParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface OverlappingParkingSpaceRepo extends JpaRepository<OverlappingParkingSpace, Long> {
        
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
