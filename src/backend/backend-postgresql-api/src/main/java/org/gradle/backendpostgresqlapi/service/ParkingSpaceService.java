package org.gradle.backendpostgresqlapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.ParkingSpaceRepo;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.gradle.backendpostgresqlapi.dto.TimestampDto;
import org.gradle.backendpostgresqlapi.repository.OverlappingParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.ParkingPointRepo;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.gradle.backendpostgresqlapi.util.CsvHandler.getCsvDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.getJsonDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.getPolygonsFromGeoJson;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.PARKING_SPACES;

@Slf4j
@Service
public class ParkingSpaceService {
    private final static WKTReader wktReader = new WKTReader();

    private final ResourceLoader resourceLoader;
    private final ParkingSpaceRepo parkingSpaceRepo;
    private final OverlappingParkingSpaceRepo overlappingParkingSpaceRepo;
    private final TimestampService timestampService;

    @Autowired
    public ParkingSpaceService(ResourceLoader resourceLoader, ParkingSpaceRepo parkingSpaceRepo, TimestampService timestampService, OverlappingParkingSpaceRepo overlappingParkingSpaceRepo) {
        this.resourceLoader = resourceLoader;
        this.parkingSpaceRepo = parkingSpaceRepo;
        this.timestampService = timestampService;
        this.overlappingParkingSpaceRepo = overlappingParkingSpaceRepo;
    }

    /**
     * Creates a spatial index if it does not already exist.
     */
    public void initializeDatabaseIndex() {
        log.debug("Initializing index for table '{}' ...", PARKING_SPACES);
        parkingSpaceRepo.createMainDataIndex();
        log.info("Index for table '{}' created.", PARKING_SPACES);
    }

    public List<ParkingSpace> getAllParkingSpaces() {
        return parkingSpaceRepo.findAll();
    }   

    /**
     * Loads data from a GeoJSON file into the database. The method
     * reads a GeoJSON file from the filesystem and inserts the data into the `parking_spaces` table.
     *
     * @param filePath the GeoJSON file from the filesystem to read from
     * @throws IOException an error when there is a problem reading the GeoJSON file
     */
    public void loadGeoJson(String filePath) throws IOException {
        String geoJsonData = getJsonDataFromFile(resourceLoader, filePath);

        if (geoJsonData.contains("Polygon")) {
            log.info("Loading file '{}' into '{}' table...", filePath, PARKING_SPACES);
            loadPolygons(geoJsonData);
            log.info("Successfully loaded file '{}' in '{}'.", filePath, PARKING_SPACES);
        } else {
            log.warn("File '{}' does not contain parking spaces data.",filePath);
        }
    }

    /**
     * Loads data from a CSV file into the database. The method
     * reads a CSV file from the filesystem and inserts the data into the `parking_spaces` table.
     * 
     * @param filePath the CSV file from the filesystem to read from
     * @throws IOException an error when there is a problem reading the CSV file
     * @throws CsvValidationException an error when there is a problem validating the CSV file
     */
    public void loadCsv(String filePath) throws IOException, CsvValidationException {
        log.info("Loading file '{}' into '{}' table...", filePath, PARKING_SPACES);
        List<ParkingSpace> csvParkingSpaces = getCsvDataFromFile(resourceLoader, filePath);
        for (ParkingSpace parkingSpace : csvParkingSpaces) {
            if (isPolygonUnique(parkingSpace.getPolygon())) {
                parkingSpaceRepo.save(parkingSpace);
            } else {
                log.warn("Parking space from CSV file not loaded and skipped! A parking space with the same polygon already exists in the '{}' table.",
                    PARKING_SPACES);
            }
        }
        log.info("Successfully loaded file '{}' in '{}'.", filePath, PARKING_SPACES);
    }

    private void loadPolygons(String geoJsonData) throws JsonProcessingException {
        int duplicatePolygons = 0;
        for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
            if (isPolygonUnique(polygon)) {

                // get centroid
                Point newPolygonCentroid = findCentroid(polygon);

                // get 3 closest other centroids and loop over them
                List<String> closestCentroids = parkingSpaceRepo.findClosestCentroids(newPolygonCentroid.toString());
                for (String neighboring_centroid : closestCentroids) {
                    // get polygon from centroid
                    Optional<String> polygonByCentroid = parkingSpaceRepo.findPolygonByCentroid(neighboring_centroid);
                    Polygon neighboringPolygon;
                        
                    try {
                        log.debug("Polygon by centroid: {}", polygonByCentroid.get());
                        neighboringPolygon = (Polygon) wktReader.read(polygonByCentroid.get());

                        if (neighboringPolygon.intersection(polygon).getArea() > 0) {
                            log.info("Polygon intersects with neighboring polygon.");
                            double originalArea = polygon.getArea();
                            Double overlapArea = neighboringPolygon.getArea();
                            double percentageOverlap = (overlapArea / originalArea) * 100;
                            log.info("Overlap area: {}", overlapArea);
                            log.info("Percentage overlap: {}", percentageOverlap);

                            if (percentageOverlap > 70) {
                                overlappingParkingSpaceRepo.insertOverlappingParkingSpaceFromPolygon(polygon);
                            } else {
                                parkingSpaceRepo.insertParkingSpaceFromPolygon(polygon);
                            }
                        } else {
                            log.info("Polygon does not intersect with neighboring polygon.");
                            parkingSpaceRepo.insertParkingSpaceFromPolygon(polygon);
                            continue;
                        }
                    } catch (org.locationtech.jts.io.ParseException e) {
                        e.printStackTrace();
                    }  
                }
            } else {
                duplicatePolygons++;
            }
        }

        if (duplicatePolygons > 0)
            log.warn("{} parkings space from GeoJSON file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePolygons, PARKING_SPACES);
    }

    public void calculateAndUpdateAreaColumn() {
        log.info("Calculating and updating area column in '{}' table...", PARKING_SPACES);
        parkingSpaceRepo.updateAreaColumn();
        log.info("Area column values were calculated and set accordingly for '{}'.", PARKING_SPACES);
    }

    private boolean isPolygonUnique(Polygon newPolygon) {
        // Convert the new polygon to WKT (Well-Known Text)
        String newPolygonWKT = new WKTWriter().write(newPolygon);

        // Use a spatial query to check if at least one duplicate exist
        long count = parkingSpaceRepo.countSamePolygons(newPolygonWKT);

        return count == 0; // If count is 0, the polygon is unique
    }
    
    private TimestampDto getMostRecentTimestamp(List<TimestampDto> firstTimestamps, List<TimestampDto> originalTimestamps) throws ParseException {
        if (firstTimestamps.isEmpty() && originalTimestamps.isEmpty()) {
            return null;
        }
    
        if (firstTimestamps.isEmpty()) {
            return Collections.max(originalTimestamps, Comparator.comparing(TimestampDto::getTimestamp));
        }
    
        if (originalTimestamps.isEmpty()) {
            return Collections.max(firstTimestamps, Comparator.comparing(TimestampDto::getTimestamp));
        }
    
        TimestampDto mostRecentFirstTimestamp = Collections.max(firstTimestamps, Comparator.comparing(TimestampDto::getTimestamp));
        TimestampDto mostRecentOriginalTimestamp = Collections.max(originalTimestamps, Comparator.comparing(TimestampDto::getTimestamp));
    
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        
        Date firstTimestamp = dateFormat.parse(mostRecentFirstTimestamp.getTimestamp());
        Date originalTimestamp = dateFormat.parse(mostRecentOriginalTimestamp.getTimestamp());
        
        return firstTimestamp.after(originalTimestamp) ? mostRecentFirstTimestamp : mostRecentOriginalTimestamp;
    }

    public Point findCentroid(Polygon polygon) {
        Geometry geometry = (Geometry) polygon;
        return geometry.getCentroid();
    }
}
