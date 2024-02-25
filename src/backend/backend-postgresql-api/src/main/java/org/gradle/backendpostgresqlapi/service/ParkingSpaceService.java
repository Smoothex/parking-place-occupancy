package org.gradle.backendpostgresqlapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.ParkingSpaceRepo;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.gradle.backendpostgresqlapi.util.CsvHandler.getCsvDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.*;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.PARKING_SPACES;

@Slf4j
@Service
public class ParkingSpaceService {

    private static final int OVERLAPPING_AREA_THRESHOLD = 50;
    private static final double DIFFERENCE_IN_SIZES_THRESHOLD = 1;
    private static final int DISTANCE_TO_CLOSEST_NEIGHBORS_LIMIT = 20;

    private final ResourceLoader resourceLoader;
    private final ParkingSpaceRepo parkingSpaceRepo;
    private final OverlappingParkingSpaceService overlappingParkingSpaceService;

    @Autowired
    public ParkingSpaceService(ResourceLoader resourceLoader, ParkingSpaceRepo parkingSpaceRepo,
        OverlappingParkingSpaceService overlappingParkingSpaceService) {
        this.resourceLoader = resourceLoader;
        this.parkingSpaceRepo = parkingSpaceRepo;
        this.overlappingParkingSpaceService = overlappingParkingSpaceService;
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

            for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
                ParkingSpace parkingSpace = new ParkingSpace();
                parkingSpace.setPolygon(polygon);
                processParkingSpace(parkingSpace);
            }

            log.info("Successfully loaded file '{}' in '{}' table.", filePath, PARKING_SPACES);
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
            processParkingSpace(parkingSpace);
        }

        log.info("Successfully loaded file '{}' in '{}' table.", filePath, PARKING_SPACES);
    }

    private boolean isPolygonUnique(Polygon newPolygon) {
        // Use a spatial query to check if a duplicate exists. If count is 0, the polygon is unique
        return parkingSpaceRepo.findOneDuplicatePolygonByCentroid(newPolygon.getCentroid().toString()) == 0;
    }

    private void processParkingSpace(ParkingSpace parkingSpace) throws JsonProcessingException {
        Polygon newPolygon = parkingSpace.getPolygon();
        if (isPolygonUnique(newPolygon)) {

            // Check if polygon is self-intersecting and if yes, cut the invalid part
            if (!newPolygon.isValid()) {
                newPolygon = (Polygon) newPolygon.buffer(0);
            }

            // Get new polygon's centroid as GeoJson
            String newPolygonAsString = newPolygon.toString();
            String newPolygonCentroidAsGeoJson = parkingSpaceRepo.calculateCentroidForPolygon(newPolygonAsString);

            // Convert it to a point and set it to the new parking space
            Point newCentroid = convertGeoJsonToPoint(newPolygonCentroidAsGeoJson);
            parkingSpace.setCentroid(newCentroid);

            // Get new polygon's area
            double newPolygonArea = parkingSpaceRepo.calculateAreaForPolygon(newPolygonAsString);

            // Get the closest parking spaces by their centroids and loop over them
            List<ParkingSpace> closestParkingSpacesByCentroid =
                getClosestParkingSpacesByCentroidAndDistance(newCentroid.toString(), DISTANCE_TO_CLOSEST_NEIGHBORS_LIMIT);

            boolean isPolygonOverlapping = false;
            for (ParkingSpace neighboringParkingSpace : closestParkingSpacesByCentroid) {
                double percentageOfOverlappingArea = parkingSpaceRepo
                    .getIntersectionAreaOfTwoPolygons(newPolygonAsString, neighboringParkingSpace.getId());
                log.debug("Intersection area: {}", String.format("%.2f", percentageOfOverlappingArea));

                // When the overlapping area exceeds the threshold we differentiate between two cases
                if (percentageOfOverlappingArea >= OVERLAPPING_AREA_THRESHOLD) {

                    // Case 1: When the new polygon is much bigger assign it to the existing one
                    if (newPolygonArea / neighboringParkingSpace.getArea() >= DIFFERENCE_IN_SIZES_THRESHOLD) {
                        if (!overlappingParkingSpaceService.doesOverlappingSpaceExistByCentroid(parkingSpace.getCentroid())) {
                            overlappingParkingSpaceService.createAndSaveOverlappingParkingSpace(parkingSpace, neighboringParkingSpace);
                        }
                    } else {
                        // Case 2: When new polygon is rather small, aggregate both polygons
                        aggregatePolygons(newPolygonAsString, neighboringParkingSpace, closestParkingSpacesByCentroid);
                    }
                    isPolygonOverlapping = true;
                    break;
                }
            }

            if (!isPolygonOverlapping) {
                saveParkingSpaceAndUpdateArea(parkingSpace, newCentroid);
            }
        } else {
            log.debug("Parking space not loaded due to a duplication in the '{}' table.", PARKING_SPACES);
        }
    }

    private void aggregatePolygons(String newPolygon, ParkingSpace overlappedExistingParkingSpace,
        List<ParkingSpace> closestParkingSpaces) throws JsonProcessingException {
        String polygonToUnion = newPolygon;
        for (ParkingSpace neighborParkingSpace : closestParkingSpaces) {
            if (!Objects.equals(neighborParkingSpace.getId(), overlappedExistingParkingSpace.getId())) {
                polygonToUnion = parkingSpaceRepo.getDifferenceOfTwoPolygons(polygonToUnion, neighborParkingSpace.getId());
            }
        }

        String aggrPolygonAsGeoJson = parkingSpaceRepo.getUnionOfTwoPolygons(polygonToUnion, overlappedExistingParkingSpace.getId());

        Polygon aggregatedPolygon = convertGeoJsonToPolygon(aggrPolygonAsGeoJson);
        overlappedExistingParkingSpace.setPolygon(aggregatedPolygon);

        String aggrPolygonCentroidAsGeoJson = parkingSpaceRepo.calculateCentroidForPolygon(aggregatedPolygon.toString());
        Point aggrPolygonCentroid = convertGeoJsonToPoint(aggrPolygonCentroidAsGeoJson);

        saveParkingSpaceAndUpdateArea(overlappedExistingParkingSpace, aggrPolygonCentroid);
    }

    private void saveParkingSpaceAndUpdateArea(ParkingSpace parkingSpace, Point centroid) {
        ParkingSpace savedParkingSpace = parkingSpaceRepo.insertParkingSpace(parkingSpace, centroid);
        parkingSpaceRepo.updateAreaColumn(savedParkingSpace.getId());
    }

    public List<ParkingSpace> getClosestParkingSpacesByCentroidAndDistance(String centroid, int distance) {
        return parkingSpaceRepo.findClosestParkingSpacesByCentroid(centroid, distance);
    }

    public Optional<Long> getIdOfParkingSpaceByPointWithin(String point) {
        return parkingSpaceRepo.getParkingSpaceIdByPointWithin(point);
    }
}
