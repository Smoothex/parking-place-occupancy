package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.GeospatialRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.getJsonDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.getPolygonsFromGeoJson;

@Slf4j
@Service
public class GeospatialService {

    private static final String GEOJSON_FILE = "classpath:data.geojson";

    @Autowired
    private GeospatialRepo geospatialRepo;

    public GeospatialService() {
    }

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

    /**
     * Loads data from a GeoJSON file into the database. The method
     * reads a GeoJSON file from the filesystem, parses it, and then
     * inserts the data into the `parking_spaces` table.
     *
     * @throws IOException an error when there is a problem reading the GeoJSON file
     */
    public void loadGeoJsonDataIntoDatabase() throws IOException {
        log.debug("Loading GeoJSON data into the database...");

        String geoJsonData = getJsonDataFromFile(GEOJSON_FILE);

        for (String polygon : getPolygonsFromGeoJson(geoJsonData)) {
            geospatialRepo.insertParkingSpace(polygon);
        }

        log.info("GeoJSON data successfully loaded into the database.");
    }

    /**
     * Fetches all parking space records from the database.
     *
     * @return a list of ParkingSpace objects representing the parking spaces in
     * the database
     */
    public List<ParkingSpace> getParkingSpaces() {
        return geospatialRepo.getAllParkingSpaces();
    }

    public Optional<ParkingSpace> getParkingSpaceById(int id) {
        return geospatialRepo.findById(id);
    }

    public List<ParkingSpace> findParkingSpacesByOccupancy(boolean occupied) {
        return geospatialRepo.findByOccupied(occupied);
    }
    
}
