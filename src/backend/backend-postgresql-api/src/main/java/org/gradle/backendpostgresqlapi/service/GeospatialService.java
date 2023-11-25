package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.configuration.GeoDataFile;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.GeospatialRepo;
import org.gradle.backendpostgresqlapi.util.JsonHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.*;
import static org.gradle.backendpostgresqlapi.util.CsvHandler.*;


import org.locationtech.jts.geom.Polygon;
import org.springframework.util.CollectionUtils;
import org.apache.commons.io.FilenameUtils;

@Slf4j
@Service
public class GeospatialService {

    private final ResourceLoader resourceLoader;

    @Autowired
    public GeospatialService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Autowired
    private GeospatialRepo geospatialRepo;

    @Autowired
    private GeoDataFile geoDataFile;


    /**
     * Initializes the database schema for storing parking spaces.
     * Creates a table and a spatial index if they do not already exist.
     */
    public void initializeDatabase() {
        log.debug("Initializing the database...");
        geospatialRepo.createTable();
        geospatialRepo.createIndex();
        log.info("Database initialized with table parking_spaces and index PS_COORDINATES_IDX.");
    }

    public void loadDataIntoDatabase() throws IOException, CsvValidationException {
        List<String> filePaths = geoDataFile.getPaths();
        log.debug("Loading data into the database...");

        if (!CollectionUtils.isEmpty(filePaths)) {
            for (String filePath : filePaths) {
                String extension = FilenameUtils.getExtension(filePath).toLowerCase();

                switch (extension) {
                    case "geojson" -> loadGeoJson(filePath);
                    case "csv" -> loadCsv(filePath);
                    default -> log.warn("Unsupported file format for file: {}", filePath);
                }
            }
            log.info("Data loading into the database is completed.");
        } else {
            log.warn("No data files configured for loading.");
        }
    }

    /**
     * Loads data from a GeoJSON file into the database. The method
     * reads a GeoJSON file from the filesystem and inserts the data into the `parking_spaces` table.
     *
     * @param filePath the GeoJSON file from the filesystem to read from
     * @throws IOException an error when there is a problem reading the GeoJSON file
     */
    private void loadGeoJson(String filePath) throws IOException {
        log.debug("Loading GeoJSON data into the database...");
        String geoJsonData = getJsonDataFromFile(resourceLoader, filePath);
        for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
            geospatialRepo.insertParkingSpaceFromPolygon(polygon);
        }
        log.info("GeoJSON data loading into the database is completed.");
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
        log.debug("Loading CSV data into the database...");
        List<ParkingSpace> csvParkingSpaces = getCsvDataFromFile(resourceLoader, filePath);
        for (ParkingSpace parkingSpace : csvParkingSpaces) {
            geospatialRepo.saveParkingSpace(parkingSpace);
        }
        log.info("CSV data loading into the database is completed.");
    }

    public void calculateAndUpdateAreaColumn() {
        log.debug("Calculating and updating area column in the database...");
        geospatialRepo.updateAreaColumn();
        log.info("Area column values were calculated and set accordingly.");
    }

    public List<ParkingSpace> getAllParkingSpaces() {
        return geospatialRepo.findAll();
    }    

    public List<String> getAllParkingSpacesAsJson() {
        return getAllParkingSpaces().stream()
                            .map(JsonHandler::convertParkingSpaceToJson)
                            .collect(Collectors.toList());
    }

    private Optional<ParkingSpace> getParkingSpaceById(int id) {
        return geospatialRepo.findById(id);
    }

    public Optional<String> getParkingSpaceByIdAsJson(int id) {
        return getParkingSpaceById(id)
                             .map(JsonHandler::convertParkingSpaceToJson);
    }

    public List<String> getParkingSpacesByOccupancyAsJson(boolean occupied) {
        List<ParkingSpace> parkingSpaces = geospatialRepo.findByOccupied(occupied);
        return parkingSpaces.stream()
                    .map(JsonHandler::convertParkingSpaceToJson)
                    .collect(Collectors.toList());
    }

    public boolean updateOccupancyStatus(int id, boolean occupied) {
        Optional<ParkingSpace> parkingSpaceOptional = getParkingSpaceById(id);
        if (parkingSpaceOptional.isPresent()) {
            ParkingSpace parkingSpace = parkingSpaceOptional.get();
            parkingSpace.setOccupied(occupied);
            geospatialRepo.save(parkingSpace);
            return true;
        }
        return false;
    }

    public Optional<String> getAreaOfParkingSpaceById(int id) {
        return getParkingSpaceById(id).map(ParkingSpace::getArea).map(Object::toString);
    }
}
