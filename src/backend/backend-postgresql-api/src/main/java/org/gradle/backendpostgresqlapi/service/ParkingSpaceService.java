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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.gradle.backendpostgresqlapi.util.CsvHandler.getCsvDataFromFile;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.*;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.PARKING_SPACES;

@Slf4j
@Service
public class ParkingSpaceService {

    private static final double OVERLAPPING_AREA_THRESHOLD = 0.5;
    private static final double DIFFERENCE_IN_SIZES_THRESHOLD = 1.0;
    private static final int DISTANCE_TO_CLOSEST_NEIGHBORS_LIMIT = 20;
    public static final double MIN_THRESHOLD_FOR_BEING_OVERLAPPED = 0.01;

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

    private void processParkingSpace(ParkingSpace newParkingSpace) throws JsonProcessingException {
        Polygon newPolygon = newParkingSpace.getPolygon();
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
            newParkingSpace.setCentroid(newCentroid);

            // Get new polygon's area
            double newPolygonArea = parkingSpaceRepo.calculateAreaForPolygon(newPolygonAsString);

            // Get the closest parking spaces by their centroids and loop over them
            List<ParkingSpace> closestParkingSpacesByCentroid =
                getClosestParkingSpacesByCentroidAndDistance(newCentroid.toString(), DISTANCE_TO_CLOSEST_NEIGHBORS_LIMIT);

            // Initialize help variables
            double maxIntersectionArea = 0.0;
            ParkingSpace maxOverlappingParkingSpace = null;
            List<ParkingSpace> parkingSpacesWithOverlap = new ArrayList<>();

            // Iterate through closest neighbors and find max intersection
            for (ParkingSpace currentNeighbor : closestParkingSpacesByCentroid) {
                double currentIntersectionArea = parkingSpaceRepo.getIntersectionAreaOfTwoPolygons(
                    newPolygonAsString, currentNeighbor.getId());
                log.debug("Intersection area: {}", String.format("%.2f", currentIntersectionArea));

                double overlappedAreaPercentageOfExistingPolygon = currentIntersectionArea / currentNeighbor.getArea() ;

                // Only add polygons to list if overlapped area is more than a minimal threshold
                if(overlappedAreaPercentageOfExistingPolygon > MIN_THRESHOLD_FOR_BEING_OVERLAPPED) {
                    parkingSpacesWithOverlap.add(currentNeighbor);
                    if(currentIntersectionArea > maxIntersectionArea) {
                        maxIntersectionArea = currentIntersectionArea;
                        maxOverlappingParkingSpace = currentNeighbor;
                    }
                }
            }

            if (maxIntersectionArea >= OVERLAPPING_AREA_THRESHOLD) {
                // When the overlapping area exceeds the threshold we differentiate between two cases
                // Case 1: When the new polygon is much bigger assign it to the existing one
                if (newPolygonArea / maxOverlappingParkingSpace.getArea() >= DIFFERENCE_IN_SIZES_THRESHOLD) {
                    if (!overlappingParkingSpaceService.doesOverlappingSpaceExistByCentroid(newParkingSpace.getCentroid())) {
                        overlappingParkingSpaceService.createAndSaveOverlappingParkingSpace(newParkingSpace, maxOverlappingParkingSpace);
                    }
                } else {
                    // Case 2: When new polygon is rather small, aggregate both polygons
                    parkingSpacesWithOverlap.remove(maxOverlappingParkingSpace);
                    aggregatePolygons(newPolygonAsString, maxOverlappingParkingSpace, parkingSpacesWithOverlap);
                }
            } else {
                // No overlaps, therefore parking space is simply saved
                if (parkingSpacesWithOverlap.isEmpty()) {
                    saveParkingSpaceAndUpdateArea(newParkingSpace, newCentroid);
                } else {
                    // Check if the max intersection is more than half of the new polygon's area and aggregate it
                    // with the max overlapping polygon otherwise reshape it and save it
                    if (maxIntersectionArea / newPolygonArea >= OVERLAPPING_AREA_THRESHOLD) {
                        parkingSpacesWithOverlap.remove(maxOverlappingParkingSpace);
                        aggregatePolygons(newPolygonAsString, maxOverlappingParkingSpace, parkingSpacesWithOverlap);
                    } else {
                        reshapePolygonAndSaveParkingSpace(newParkingSpace, parkingSpacesWithOverlap);
                    }
                }
            }

        } else {
            log.debug("Parking space not loaded due to a duplication in the '{}' table.", PARKING_SPACES);
        }
    }

    /**
     * Aggregates existing and new polygon by cutting parts of new polygon,
     * which intersect with other neighboring polygons.
     * @param newPolygon the polygon, which will be aggregated with the existing one
     * @param overlappedExistingParkingSpace the target parking space whose polygon has to be aggregated
     * @param closestParkingSpaces list ot neighbors which intersect the new polygon
     * @throws JsonProcessingException error on parsing a Geo JSON format
     */
    private void aggregatePolygons(String newPolygon, ParkingSpace overlappedExistingParkingSpace,
        List<ParkingSpace> closestParkingSpaces) throws JsonProcessingException {
        String polygonToUnion = newPolygon;
        for (ParkingSpace neighborParkingSpace : closestParkingSpaces) {
            polygonToUnion = parkingSpaceRepo.getDifferenceOfTwoPolygons(polygonToUnion, neighborParkingSpace.getId());
        }

        String aggregatedPolygonAsGeoJson =
            parkingSpaceRepo.getUnionOfTwoPolygons(polygonToUnion, overlappedExistingParkingSpace.getId());

        Polygon aggregatedPolygon = convertGeoJsonToPolygon(aggregatedPolygonAsGeoJson);

        overlappedExistingParkingSpace.setPolygon(aggregatedPolygon);

        String aggregatedPolygonCentroidAsGeoJson = parkingSpaceRepo.calculateCentroidForPolygon(aggregatedPolygon.toString());
        Point aggregatedPolygonCentroid = convertGeoJsonToPoint(aggregatedPolygonCentroidAsGeoJson);

        saveParkingSpaceAndUpdateArea(overlappedExistingParkingSpace, aggregatedPolygonCentroid);
    }

    /**
     * Reshapes the new polygon by cutting parts of new polygon, which intersect
     * with other neighboring polygons, and saves it.
     * @param newParkingSpace the parking space whose polygon has to be reshaped and saved
     * @param closestParkingSpaces list ot neighbors which intersect the new polygon
     * @throws JsonProcessingException error on parsing a Geo JSON format
     */
    private void reshapePolygonAndSaveParkingSpace(ParkingSpace newParkingSpace, List<ParkingSpace> closestParkingSpaces)
        throws JsonProcessingException {
        String reshapedPolygonAsString = newParkingSpace.getPolygon().toString();
        for (ParkingSpace neighborParkingSpace : closestParkingSpaces) {
            reshapedPolygonAsString = parkingSpaceRepo.getDifferenceOfTwoPolygons(reshapedPolygonAsString, neighborParkingSpace.getId());
        }

        String reshapedPolygonAsGeoJson = parkingSpaceRepo.getGeoJsonForPolygon(reshapedPolygonAsString);
        Polygon reshapedPolygon = convertGeoJsonToPolygon(reshapedPolygonAsGeoJson);
        newParkingSpace.setPolygon(reshapedPolygon);

        String reshapedPolygonCentroidAsGeoJson = parkingSpaceRepo.calculateCentroidForPolygon(reshapedPolygon.toString());
        Point reshapedPolygonCentroid = convertGeoJsonToPoint(reshapedPolygonCentroidAsGeoJson);

        saveParkingSpaceAndUpdateArea(newParkingSpace, reshapedPolygonCentroid);
    }

    private void saveParkingSpaceAndUpdateArea(ParkingSpace parkingSpace, Point centroid) {
        parkingSpace.setCentroid(centroid);
        ParkingSpace savedParkingSpace = parkingSpaceRepo.insertParkingSpace(parkingSpace);
        parkingSpaceRepo.updateAreaColumn(savedParkingSpace.getId());
    }

    public List<ParkingSpace> getClosestParkingSpacesByCentroidAndDistance(String centroid, int distance) {
        return parkingSpaceRepo.findClosestParkingSpacesByCentroid(centroid, distance);
    }

    public Optional<Long> getIdOfParkingSpaceByPointWithin(String point) {
        return parkingSpaceRepo.getParkingSpaceIdByPointWithin(point);
    }
}
