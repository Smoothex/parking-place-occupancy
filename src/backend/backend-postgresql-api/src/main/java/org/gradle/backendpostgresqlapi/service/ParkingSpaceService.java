package org.gradle.backendpostgresqlapi.service;

import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.ParkingSpaceRepo;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.gradle.backendpostgresqlapi.repository.OverlappingParkingSpaceRepo;

import java.io.IOException;
import java.util.List;

import static org.gradle.backendpostgresqlapi.util.CsvHandler.getCsvDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.getJsonDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.getPolygonsFromGeoJson;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.PARKING_SPACES;

@Slf4j
@Service
public class ParkingSpaceService {

    private final ResourceLoader resourceLoader;
    private final ParkingSpaceRepo parkingSpaceRepo;
    private final OverlappingParkingSpaceRepo overlappingParkingSpaceRepo;

    @Autowired
    public ParkingSpaceService(ResourceLoader resourceLoader, ParkingSpaceRepo parkingSpaceRepo, OverlappingParkingSpaceRepo overlappingParkingSpaceRepo) {
        this.resourceLoader = resourceLoader;
        this.parkingSpaceRepo = parkingSpaceRepo;
        this.overlappingParkingSpaceRepo = overlappingParkingSpaceRepo;
    }

    /**
     * Creates a spatial index if it does not already exist.
     */
    public void initializeDbIndex() {
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
            loadPolygonsFromGeoJson(geoJsonData);
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
        loadPolygonsFromCsv(filePath);
        log.info("Successfully loaded file '{}' in '{}'.", filePath, PARKING_SPACES);
    }

    private void loadPolygonsFromCsv(String filePath) throws IOException, CsvValidationException {
        List<ParkingSpace> csvParkingSpaces = getCsvDataFromFile(resourceLoader, filePath);
        for (ParkingSpace parkingSpace : csvParkingSpaces) {
            processParkingSpace(parkingSpace);
        }
    }

    private void loadPolygonsFromGeoJson(String geoJsonData) throws IOException {
        for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
            ParkingSpace parkingSpace = new ParkingSpace();
            parkingSpace.setPolygon(polygon);
            processParkingSpace(parkingSpace);
        }
    }

    private boolean isPolygonUnique(Polygon newPolygon) {
        // Use a spatial query to check if a duplicate exists
        long count = parkingSpaceRepo.findOneDuplicatePolygonByCentroid(newPolygon.getCentroid().toString());

        return count == 0; // If count is 0, the polygon is unique
    }

    private void processParkingSpace(ParkingSpace parkingSpace) {
        Polygon polygon = parkingSpace.getPolygon();
        if (isPolygonUnique(polygon)) {

            // Check if polygon is self-intersecting and if yes, cut the invalid part
            if (!polygon.isValid()) {
                polygon = (Polygon) polygon.buffer(0);
            }

            // get new polygon's centroid
            String newPolygonCentroid = polygon.getCentroid().toString();
    
            // get 3 closest parking spaces by their centroids and loop over them
            List<ParkingSpace> closestParkingSpacesByCentroid = parkingSpaceRepo.findClosestParkingSpacesByCentroid(newPolygonCentroid);
    
            boolean isPolygonOverlapping = false;
            for (ParkingSpace neighboringParkingSpace : closestParkingSpacesByCentroid) {
                double percentageOfOverlappingArea = parkingSpaceRepo
                    .findIntersectionAreaOfTwoPolygons(polygon.toString(), neighboringParkingSpace.getId());
                log.debug("Intersection area: {}", String.format("%.2f", percentageOfOverlappingArea));
    
                if (percentageOfOverlappingArea >= 50) {
                    overlappingParkingSpaceRepo.insertParkingSpace(parkingSpace, neighboringParkingSpace);
                    isPolygonOverlapping = true;
                    break;
                }
            }
    
            if (!isPolygonOverlapping) {
                ParkingSpace savedParkingSpace = parkingSpaceRepo.insertParkingSpace(parkingSpace);
                parkingSpaceRepo.updateAreaColumn(savedParkingSpace.getId());
            }
        } else {
            log.debug("Parking space not loaded due to a duplication in the '{}' table.", PARKING_SPACES);
        }
    }
}
