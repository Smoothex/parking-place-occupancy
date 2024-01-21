package org.gradle.backendpostgresqlapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.dto.ParkingPointDto;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.gradle.backendpostgresqlapi.repository.EditedParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.ParkingPointRepo;
import org.gradle.backendpostgresqlapi.util.DtoConverterUtil;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.getParkingPointsAndTimestampsFromFile;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.*;

@Slf4j
@Service
public class ParkingPointService {

    private final EditedParkingSpaceRepo editedParkingSpaceRepo;
    private final ParkingPointRepo parkingPointRepo;
    private final TimestampService timestampService;

    @Autowired
    public ParkingPointService(EditedParkingSpaceRepo editedParkingSpaceRepo, ParkingPointRepo parkingPointRepo,
        TimestampService timestampService) {
        this.editedParkingSpaceRepo = editedParkingSpaceRepo;
        this.parkingPointRepo = parkingPointRepo;
        this.timestampService = timestampService;
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

    public void loadParkingPoints(String geoJsonData) throws JsonProcessingException {
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
                    parkingPoint.setEditedParkingSpaceId(editedParkingSpaceId.get());
                } else {
                    parkingPoint.setEditedParkingSpaceId(-1L);
                }

                ParkingPoint savedParkingPoint = parkingPointRepo.save(parkingPoint);
                timestamp.setParkingPointId(savedParkingPoint.getId());
            } else {
                duplicatePoints++;
                timestamp.setParkingPointId(duplicateId);
            }

            timestampService.saveTimestamp(timestamp);
        }

        if (duplicatePoints > 0)
            log.warn("{} points from GeoJSON file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePoints, PARKING_POINTS);
        log.info("GeoJSON data loading into tables '{}' and '{}' is completed.", PARKING_POINTS, TIMESTAMPS);
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
