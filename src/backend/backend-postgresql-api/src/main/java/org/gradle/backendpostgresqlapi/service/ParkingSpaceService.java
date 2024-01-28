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
import org.locationtech.jts.geom.Point;

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
        int duplicatePolygons = 0;
        for (ParkingSpace parkingSpace : csvParkingSpaces) {
            if (isPolygonUnique(parkingSpace.getPolygon())) {
                parkingSpaceRepo.insertParkingSpaceWithCentroid(parkingSpace);
            } else {
                duplicatePolygons++;
            }
        }

        if (duplicatePolygons > 0)
            log.warn("{} parking spaces from CSV file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePolygons, PARKING_SPACES);
    }

    private void loadPolygonsFromGeoJson(String geoJsonData) throws IOException {
        int duplicatePolygons = 0;
        for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
            if (isPolygonUnique(polygon)) {

                // get new polygon's centroid
                Point newPolygonCentroid = polygon.getCentroid();

                // get 3 closest parking spaces by their centroids and loop over them
                List<ParkingSpace> closestParkingSpacesByCentroid = parkingSpaceRepo.findClosestParkingSpacesByCentroid(newPolygonCentroid.toString());

                boolean isPolygonOverlapping = false;
                for (ParkingSpace neighboringParkingSpace : closestParkingSpacesByCentroid) {
                    double intersectionArea = neighboringParkingSpace.getPolygon().intersection(polygon).getArea();
                    log.debug("Intersection area: {}", intersectionArea);

                    if (intersectionArea >= 70) {
                        overlappingParkingSpaceRepo.insertOverlappingParkingSpaceFromPolygon(polygon);
                        isPolygonOverlapping = true;
                        break;
                    }
                }

                if (!isPolygonOverlapping) {
                    parkingSpaceRepo.insertParkingSpaceFromPolygon(polygon);
                }
            } else {
                duplicatePolygons++;
            }
        }

        if (duplicatePolygons > 0)
            log.warn("{} parking spaces from GeoJSON file were not loaded and skipped due to a duplication in the '{}' table.",
                duplicatePolygons, PARKING_SPACES);
    }

    public void calculateAndUpdateAreaColumn() {
        log.info("Calculating and updating area column in '{}' table...", PARKING_SPACES);
        parkingSpaceRepo.updateAreaColumn();
        log.info("Area column values were calculated and set accordingly for '{}'.", PARKING_SPACES);
    }

    private boolean isPolygonUnique(Polygon newPolygon) {
        // Use a spatial query to check if a duplicate exists
        long count = parkingSpaceRepo.findOneDuplicatePolygonByCentroid(newPolygon.getCentroid().toString());

        return count == 0; // If count is 0, the polygon is unique
    }
}
