package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.OverlappingParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.OverlappingParkingSpaceRepo;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.gradle.backendpostgresqlapi.util.TableNameUtil.OVERLAPPING_PARKING_SPACES;

@Slf4j
@Service
public class OverlappingParkingSpaceService {

    private final OverlappingParkingSpaceRepo overlappingParkingSpaceRepo;

    @Autowired
    public OverlappingParkingSpaceService(OverlappingParkingSpaceRepo overlappingParkingSpaceRepo) {
        this.overlappingParkingSpaceRepo = overlappingParkingSpaceRepo;
    }

    /**
     * Creates a timestamp index if it does not already exist.
     */
    public void initializeDbIndex() {
        log.debug("Initializing index for table '{}' ...", OVERLAPPING_PARKING_SPACES);
        overlappingParkingSpaceRepo.createDbIndex();
        log.info("Index for table '{}' created.", OVERLAPPING_PARKING_SPACES);
    }

    public void createAndSaveOverlappingParkingSpace(ParkingSpace newParkingSpace, ParkingSpace existingParkingSpace) {
        OverlappingParkingSpace overlappingParkingSpace = new OverlappingParkingSpace(
            newParkingSpace.getPolygon(), newParkingSpace.getCapacity(),
            newParkingSpace.getPosition(), newParkingSpace.getCentroid(), existingParkingSpace);

        overlappingParkingSpaceRepo.insertParkingSpace(overlappingParkingSpace);
    }

    public boolean doesOverlappingSpaceExistByCentroid(Point centroid) {
        return overlappingParkingSpaceRepo.existsByCentroid(centroid);
    }
}
