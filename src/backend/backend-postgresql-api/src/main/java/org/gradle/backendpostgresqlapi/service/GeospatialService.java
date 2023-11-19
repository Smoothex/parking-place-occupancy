package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.GeospatialRepo;
import org.gradle.backendpostgresqlapi.util.JsonHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.getJsonDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.getPolygonsFromGeoJson;

import org.locationtech.jts.geom.Polygon;

@Slf4j
@Service
public class GeospatialService {

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
     * @param filePath the location of the file
     * @throws IOException an error when there is a problem reading the GeoJSON file
     */
    public void loadGeoJsonDataIntoDatabase(String filePath) throws IOException {
        log.debug("Loading GeoJSON data into the database...");

        String geoJsonData = getJsonDataFromFile(filePath);
        for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
            geospatialRepo.insertParkingSpace(polygon);
        }

        log.info("GeoJSON data successfully loaded into the database.");
    }

    public void calculateAndUpdateAreaColumn() {
        log.debug("Calculating and updating area column in the database...");
        geospatialRepo.updateAreaColumn();
        log.info("Area column values were calculated and set accordingly.");
    }

    public List<String> getAllParkingSpacesAsJson() {
        List<ParkingSpace> parkingSpaces = geospatialRepo.findAll();
        return parkingSpaces.stream()
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

    public List<String> findParkingSpacesByOccupancy(boolean occupied) {
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
