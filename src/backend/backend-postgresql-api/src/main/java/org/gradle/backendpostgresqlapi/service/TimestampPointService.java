package org.gradle.backendpostgresqlapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.TimestampPoint;
import org.gradle.backendpostgresqlapi.repository.EditedParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.TimestampPointRepo;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.getTimestampPointsFromGeoJson;

@Slf4j
@Service
public class TimestampPointService {

    private final EditedParkingSpaceRepo editedParkingSpaceRepo;
    private final TimestampPointRepo timestampPointRepo;

    private static final String TABLE_NAME = "timestamp_points";

    @Autowired
    public TimestampPointService(EditedParkingSpaceRepo editedParkingSpaceRepo, TimestampPointRepo timestampPointRepo) {
        this.editedParkingSpaceRepo = editedParkingSpaceRepo;
        this.timestampPointRepo = timestampPointRepo;
    }

    /**
     * Creates a spatial index if it does not already exist.
     */
    public void initializeDbIndex() {
        log.debug("Initializing index for table '{}' ...", TABLE_NAME);
        timestampPointRepo.createIndex();
        log.info("Index 'tp_coordinates_idx' created.");
    }

    public List<TimestampPoint> getAllTimestampPointsByEditedParkingSpaceId(long editedParkingSpaceId) {
        return timestampPointRepo.getTimestampPointsByEditedParkingSpaceId(editedParkingSpaceId);
    }

    public void loadTimestampPoints(String geoJsonData) throws JsonProcessingException {
        int duplicatePoints = 0;
        for (TimestampPoint timestampPoint: getTimestampPointsFromGeoJson(geoJsonData)) {
            if (isPointUnique(timestampPoint.getTimestamp(), timestampPoint.getPoint())) {

                // Convert the new polygon to WKT (Well-Known Text)
                String pointWKT = new WKTWriter().write(timestampPoint.getPoint());
                Optional<Long> id = editedParkingSpaceRepo.getIdByPointWithin(pointWKT);
                if (id.isPresent()) {
                    timestampPoint.setEditedParkingSpaceId(id.get());
                } else {
                    timestampPoint.setEditedParkingSpaceId(-1L);
                }
                timestampPointRepo.save(timestampPoint);
            } else {
                duplicatePoints++;
            }
        }

        if (duplicatePoints > 0)
            log.warn("{} points from GeoJSON file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePoints, TABLE_NAME);
        log.info("GeoJSON data loading into '{}' table is completed.", TABLE_NAME);
    }

    private boolean isPointUnique(String timestamp, Point newPoint) {
        // Convert the new point to WKT (Well-Known Text)
        String newPointWKT = new WKTWriter().write(newPoint);

        // Use a spatial query to check if at least one duplicate exist
        long count = timestampPointRepo.getMaxOneDuplicate(timestamp, newPointWKT);

        return count == 0; // If count is 0, the point is unique
    }

    public String convertTimestampToDateFormat(String timestamp) {
        long milliseconds = Long.parseLong(timestamp);
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
        return dateFormat.format(milliseconds);
    }
}
