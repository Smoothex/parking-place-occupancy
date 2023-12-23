package org.gradle.backendpostgresqlapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.configuration.GeoDataFile;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.ParkingSpaceRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.util.List;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.*;
import static org.gradle.backendpostgresqlapi.util.CsvHandler.*;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.PARKING_POINTS;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.PARKING_SPACES;

import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.util.CollectionUtils;
import org.apache.commons.io.FilenameUtils;

@Slf4j
@Service
public class ParkingSpaceService {

    private final ResourceLoader resourceLoader;
    private final ParkingSpaceRepo parkingSpaceRepo;
    private final ParkingPointService parkingPointService;
    private final GeoDataFile geoDataFile;

    @Autowired
    public ParkingSpaceService(ResourceLoader resourceLoader, ParkingSpaceRepo parkingSpaceRepo,
        ParkingPointService parkingPointService, GeoDataFile geoDataFile) {
        this.resourceLoader = resourceLoader;
        this.parkingSpaceRepo = parkingSpaceRepo;
        this.parkingPointService = parkingPointService;
        this.geoDataFile = geoDataFile;
    }

    /**
     * Creates a spatial index if it does not already exist.
     */
    public void initializeDatabaseIndex() {
        log.debug("Initializing index for table '{}' ...", PARKING_SPACES);
        parkingSpaceRepo.createMainDataIndex();
        log.info("Index for table '{}' created.", PARKING_SPACES);
    }

    public void loadDataIntoDatabase() throws IOException, CsvValidationException {
        List<String> filePaths = geoDataFile.getPaths();

        if (!CollectionUtils.isEmpty(filePaths)) {
            for (String filePath : filePaths) {
                String extension = FilenameUtils.getExtension(filePath).toLowerCase();

                switch (extension) {
                    case "geojson" -> loadGeoJson(filePath);
                    case "csv" -> loadCsv(filePath);
                    default -> log.warn("Unsupported file format for file: {}", filePath);
                }
            }
        } else {
            log.warn("No data files configured for loading.");
        }
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
    private void loadGeoJson(String filePath) throws IOException {
        String geoJsonData = getJsonDataFromFile(resourceLoader, filePath);

        if (geoJsonData.contains("Polygon")) {
            log.debug("Loading GeoJSON data into '{}' table...", PARKING_SPACES);
            loadPolygons(geoJsonData);
            log.info("Successfully loaded all data in '{}'.", PARKING_SPACES);
        } else {
            log.debug("Loading GeoJSON data into '{}' table...", PARKING_POINTS);
            parkingPointService.loadParkingPoints(geoJsonData);
            log.info("Successfully loaded all data in '{}'.", PARKING_POINTS);
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
    private void loadCsv(String filePath) throws IOException, CsvValidationException {
        log.debug("Loading CSV data into '{}' table...", PARKING_SPACES);
        List<ParkingSpace> csvParkingSpaces = getCsvDataFromFile(resourceLoader, filePath);
        for (ParkingSpace parkingSpace : csvParkingSpaces) {
            if (isPolygonUnique(parkingSpace.getPolygon())) {
                parkingSpaceRepo.save(parkingSpace);
            } else {
                log.warn("Parking space from CSV file not loaded and skipped! A parking space with the same polygon already exists in the '{}' table.",
                    PARKING_SPACES);
            }
        }
        log.info("CSV data loading into '{}' table is completed.", PARKING_SPACES);
    }

    private void loadPolygons(String geoJsonData) throws JsonProcessingException {
        int duplicatePolygons = 0;
        for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
            if (isPolygonUnique(polygon))
                parkingSpaceRepo.insertParkingSpaceFromPolygon(polygon);
            else
                duplicatePolygons++;
        }

        if (duplicatePolygons > 0)
            log.warn("{} parkings space from GeoJSON file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePolygons, PARKING_SPACES);
        log.info("GeoJSON data loading into '{}' table is completed.", PARKING_SPACES);
    }

    public void calculateAndUpdateAreaColumn() {
        log.debug("Calculating and updating area column in '{}' table...", PARKING_SPACES);
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
}
