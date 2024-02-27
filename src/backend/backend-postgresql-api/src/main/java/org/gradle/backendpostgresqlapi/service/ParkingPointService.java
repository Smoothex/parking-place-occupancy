package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.*;
import org.gradle.backendpostgresqlapi.repository.EditedParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.OverlappingParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.ParkingPointRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.getJsonDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.getParkingPointsAndTimestampsFromFile;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.PARKING_POINTS;

@Slf4j
@Service
public class ParkingPointService {

    private static final int DISTANCE_TO_CLOSEST_NEIGHBORS_LIMIT = 100;

    private final ParkingSpaceService parkingSpaceService;
    private final OverlappingParkingSpaceRepo overlappingParkingSpaceRepo;
    private final EditedParkingSpaceRepo editedParkingSpaceRepo;
    private final ParkingPointRepo parkingPointRepo;
    private final TimestampService timestampService;
    private final ResourceLoader resourceLoader;

    @Autowired
    public ParkingPointService(ParkingSpaceService parkingSpaceService, OverlappingParkingSpaceRepo overlappingParkingSpaceRepo,
        EditedParkingSpaceRepo editedParkingSpaceRepo, ParkingPointRepo parkingPointRepo,
        TimestampService timestampService, ResourceLoader resourceLoader) {
        this.parkingSpaceService = parkingSpaceService;
        this.overlappingParkingSpaceRepo = overlappingParkingSpaceRepo;
        this.editedParkingSpaceRepo = editedParkingSpaceRepo;
        this.parkingPointRepo = parkingPointRepo;
        this.timestampService = timestampService;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Creates a spatial index if it does not already exist.
     */
    public void initializeDbIndex() {
        log.debug("Initializing index for table '{}' ...", PARKING_POINTS);
        parkingPointRepo.createIndex();
        log.info("Index for table '{}' created.", PARKING_POINTS);
    }

    public List<ParkingPoint> getAllParkingPointsByEditedParkingSpaceId(long editedParkingSpaceId) {
        return parkingPointRepo.getParkingPointsByEditedParkingSpaceId(editedParkingSpaceId);
    }

    /**
     * Loads data from a GeoJSON file into the database. The method
     * reads a GeoJSON file from the filesystem and inserts the data into the `parking_points` table.
     *
     * @param filePath the GeoJSON file from the filesystem to read from
     * @throws IOException an error when there is a problem reading the GeoJSON file
     */
    public void loadGeoJson(String filePath) throws IOException {
        String geoJsonData = getJsonDataFromFile(resourceLoader, filePath);

        if (geoJsonData.contains("DateTime")) {
            log.info("Loading file '{}' into '{}' table...", filePath, PARKING_POINTS);
            loadParkingPoints(geoJsonData);
            log.info("Successfully loaded file '{}' in '{}'.", filePath, PARKING_POINTS);
        } else {
            log.warn("File '{}' does not contain parking points and timestamps data.",filePath);
        }
    }

    private void loadParkingPoints(String geoJsonData) throws IOException {
        int duplicatePoints = 0;

        for (Map.Entry<ParkingPoint, Timestamp> entry : getParkingPointsAndTimestampsFromFile(geoJsonData).entrySet()) {
            ParkingPoint parkingPoint = entry.getKey();
            Timestamp timestamp = entry.getValue();
            long duplicateId = getIdOfDuplicateParkingPoint(parkingPoint);

            if (duplicateId == -1L) {
                // Search for a polygon in the 'parking_spaces' table
                Optional<Long> parkingSpaceId = parkingSpaceService.getIdOfParkingSpaceByPointWithin(parkingPoint.getPoint().toString());

                EditedParkingSpace editedParkingSpace = null;
                if (parkingSpaceId.isPresent()) {
                    editedParkingSpace = editedParkingSpaceRepo.getEditedParkingSpaceByParkingSpaceId(parkingSpaceId.get());
                    log.debug("Edited parking space with id '{}' found.", editedParkingSpace.getId());
                } else {
                    // Search for a polygon in the 'overlapping_parking_places' table
                    Optional<OverlappingParkingSpace> overlappingParkingSpace = overlappingParkingSpaceRepo
                        .getOverlappingParkingSpaceByPointWithin(parkingPoint.getPoint().toString());

                    if (overlappingParkingSpace.isPresent()) {
                        editedParkingSpace = editedParkingSpaceRepo
                            .getEditedParkingSpaceByParkingSpaceId(overlappingParkingSpace.get().getAssignedParkingSpace().getId());
                        log.debug("Edited parking space with id '{}' found.", editedParkingSpace.getId());
                    } else {
                        log.debug("No edited parking space found for point '{}'.", parkingPoint.getPoint().toString());
                    }
                }

                // In case no parking space or overlapping parking space was found until now
                if (editedParkingSpace == null) {
                    List<ParkingSpace> closestParkingSpaces = parkingSpaceService.getClosestParkingSpacesByCentroidAndDistance(
                        parkingPoint.getPoint().toString(), DISTANCE_TO_CLOSEST_NEIGHBORS_LIMIT);

                    if (!closestParkingSpaces.isEmpty()) {
                        long closestParkingSpaceId = closestParkingSpaces.get(0).getId();
                        editedParkingSpace = editedParkingSpaceRepo.getEditedParkingSpaceByParkingSpaceId(
                            closestParkingSpaceId);
                    }
                }

                parkingPoint.setEditedParkingSpace(editedParkingSpace);
                ParkingPoint savedParkingPoint = parkingPointRepo.save(parkingPoint);
                timestamp.setParkingPoint(savedParkingPoint);
            } else {
                duplicatePoints++;
                ParkingPoint existingParkingPoint = parkingPointRepo.getParkingPointById(duplicateId);
                timestamp.setParkingPoint(existingParkingPoint);
            }
            timestampService.saveTimestamp(timestamp);
        }

        if (duplicatePoints > 0) {
            log.warn("{} points from GeoJSON file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePoints, PARKING_POINTS);
        }
    }

    /**
     * Uses a spatial query to check a duplicate exists and get its id.
     * @param newPoint a ParkingPoint object containing the point which is checked in the database
     * @return the id of the duplicate, otherwise -1
     */
    private long getIdOfDuplicateParkingPoint(ParkingPoint newPoint) {
        // Use a spatial query to check a duplicate exists
        Optional<Long> duplicateId = parkingPointRepo.getIdOfDuplicateByCoordinates(newPoint.getPoint().toString());
        if (duplicateId.isPresent()) {
            return duplicateId.get();
        }

        return -1; // If id is -1, the point is unique
    }
}
