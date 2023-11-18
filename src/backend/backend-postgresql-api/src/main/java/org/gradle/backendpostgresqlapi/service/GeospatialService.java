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
import org.locationtech.jts.geom.util.GeometryTransformer;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.locationtech.proj4j.CoordinateReferenceSystem;

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
        for (Polygon polygon : getPolygonsFromGeoJson(geoJsonData)) {
            geospatialRepo.insertParkingSpace(polygon);
        }

        log.info("GeoJSON data successfully loaded into the database.");
    }

    public List<String> getAllParkingSpacesAsJson() {
        List<ParkingSpace> parkingSpaces = geospatialRepo.findAll();
        return parkingSpaces.stream()
                            .map(JsonHandler::convertParkingSpaceToJson)
                            .collect(Collectors.toList());
    }

    public Optional<String> getParkingSpaceByIdAsJson(int id) {
        return geospatialRepo.findById(id)
                             .map(JsonHandler::convertParkingSpaceToJson);
    }

    public List<String> findParkingSpacesByOccupancy(boolean occupied) {
        List<ParkingSpace> parkingSpaces = geospatialRepo.findByOccupied(occupied);
        return parkingSpaces.stream()
                    .map(JsonHandler::convertParkingSpaceToJson)
                    .collect(Collectors.toList());
    }

    public boolean updateOccupancyStatus(int id, boolean occupied) {
        Optional<ParkingSpace> parkingSpaceOptional = geospatialRepo.findById(id);
        if (parkingSpaceOptional.isPresent()) {
            ParkingSpace parkingSpace = parkingSpaceOptional.get();
            parkingSpace.setOccupied(occupied);
            geospatialRepo.save(parkingSpace);
            return true;
        }
        return false;
    }

    public Optional<String> calculateAreaOfParkingSpace(int id) {
        return geospatialRepo.findById(id)
                             .map(ParkingSpace::getPolygon)
                             .map(this::transformAndCalculateArea);
    }

    /**
     * Transforms the Coordinate Reference System (CRS) of a polygon and calculates its area.
     * Handles the transformation by iterating over each coordinate of the polygon, applying
     * the coordinate conversion, and then computing the area of the resultant geometry.<br>
     *
     * The source CRS is in WGS84 (World Geodetic System 1984) format, which is often used when working
     * with GIS data and is based on a reference ellipsoid (i.e., the earth). Coordinates
     * are represented as latitude and longitude.<br>
     *
     * The target CRS is in UTM (Universal Transverse Mercator) format, which is suited for spatial analysis
     * and uses Cartesian coordinate system with linear units (meters) for its coordinates.
     *
     * @param polygon The Polygon object defined in WGS84 coordinates to be transformed and calculated.
     * @return The area of the polygon in square meters after transformation to UTM coordinates.
     */
    private String transformAndCalculateArea(Polygon polygon) {
        CoordinateTransform coordinateTransform = getCoordinateTransform();

        // Create a custom GeometryTransformer
        GeometryTransformer geomTransformer = new GeometryTransformer() {
            @Override
            protected CoordinateSequence transformCoordinates(CoordinateSequence coordinates, Geometry parent) {
                CoordinateSequence transformedCoordinates = coordinates.copy();
                for (int i = 0; i < coordinates.size(); i++) {
                    // Create a ProjCoordinate from the current coordinate
                    ProjCoordinate srcCoord = new ProjCoordinate(coordinates.getX(i), coordinates.getY(i));
                    // Transform the source coordinate to the target CRS
                    ProjCoordinate dstCoord = coordinateTransform.transform(srcCoord, new ProjCoordinate());
                    // Update the transformed coordinates sequence with the new values
                    transformedCoordinates.setOrdinate(i, 0, dstCoord.x);
                    transformedCoordinates.setOrdinate(i, 1, dstCoord.y);
                }
                return transformedCoordinates;
            }
        };

        // Transform the geometry
        Geometry transformedGeom = geomTransformer.transform(polygon);

        // Return the area of the transformed geometry
        return String.format("%.2f", transformedGeom.getArea());
    }

    private static CoordinateTransform getCoordinateTransform() {
        CRSFactory crsFactory = new CRSFactory();
        // Define the source and target CRS
        CoordinateReferenceSystem sourceCRS = crsFactory.createFromName("EPSG:4326"); // WGS84
        CoordinateReferenceSystem targetCRS = crsFactory.createFromName("EPSG:25833"); // UTM

        // Create a factory for coordinate transformations
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        return ctFactory.createTransform(sourceCRS, targetCRS);
    }
}
