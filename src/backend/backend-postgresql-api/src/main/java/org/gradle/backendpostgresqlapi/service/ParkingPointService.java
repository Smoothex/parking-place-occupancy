package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.dto.ParkingPointDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.gradle.backendpostgresqlapi.repository.EditedParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.ParkingPointRepo;
import org.gradle.backendpostgresqlapi.util.DtoConverterUtil;
import org.locationtech.jts.io.WKTWriter;
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

    private final EditedParkingSpaceRepo editedParkingSpaceRepo;
    private final ParkingPointRepo parkingPointRepo;
    private final TimestampService timestampService;
    private final ResourceLoader resourceLoader;

    @Autowired
    public ParkingPointService(EditedParkingSpaceRepo editedParkingSpaceRepo, ParkingPointRepo parkingPointRepo,
        TimestampService timestampService, ResourceLoader resourceLoader) {
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

    public List<ParkingPointDto> getAllParkingPointsByEditedParkingSpaceIdAsDto(long editedParkingSpaceId) {
        return parkingPointRepo.getParkingPointsByEditedParkingSpaceId(editedParkingSpaceId).
            stream().map(DtoConverterUtil::convertToDto).toList();
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
            long duplicateId = isPointUnique(parkingPoint);

            if (duplicateId == -1L) {

                // Convert the new polygon to WKT (Well-Known Text)
                String pointWKT = new WKTWriter().write(parkingPoint.getPoint());
                Optional<Long> editedParkingSpaceId = editedParkingSpaceRepo.getIdByPointWithin(pointWKT);
                if (editedParkingSpaceId.isPresent()) {
                    EditedParkingSpace editedParkingSpace = editedParkingSpaceRepo.findById(editedParkingSpaceId.get()).orElse(null);
                    log.warn("Edited parking space with id '{}' found.", editedParkingSpaceId.get());
                    parkingPoint.setEditedParkingSpace(editedParkingSpace);
                } else {
                    log.warn("No edited parking space found for point '{}'.", pointWKT);
                    parkingPoint.setEditedParkingSpace(null);
                }

                ParkingPoint savedParkingPoint = parkingPointRepo.save(parkingPoint);
                timestamp.setParkingPoint(savedParkingPoint);
            } else {
                duplicatePoints++;
            }
            timestampService.saveTimestamp(timestamp);
        }

        if (duplicatePoints > 0) {
            log.warn("{} points from GeoJSON file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePoints, PARKING_POINTS);
        }
    }

    private long isPointUnique(ParkingPoint newPoint) {
        // Convert the new point to WKT (Well-Known Text)
        String newPointWKT = new WKTWriter().write(newPoint.getPoint());

        // Use a spatial query to check if at least one duplicate exist
        Optional<Long> duplicateId = parkingPointRepo.getIdOfDuplicateByCoordinates(newPointWKT);
        if (duplicateId.isPresent()) {
            return duplicateId.get();
        }

        return -1; // If id is -1, the point is unique
    }
}
