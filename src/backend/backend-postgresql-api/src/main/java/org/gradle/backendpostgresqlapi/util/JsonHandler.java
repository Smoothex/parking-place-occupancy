package org.gradle.backendpostgresqlapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.gradle.backendpostgresqlapi.util.DateConverterUtil.formatMillisecondsDateToString;

@Slf4j
public class JsonHandler {

    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final String POLYGON_TYPE = "Polygon";
    private static final String MULTI_POLYGON_TYPE = "MultiPolygon";
    private static final String GEOMETRY_COLLECTION_TYPE = "GeometryCollection";
    private static final String COORDINATES_PROPERTY = "coordinates";
    private static final String TYPE_PROPERTY = "type";
    private static final String GEOMETRIES_PROPERTY = "geometries";
    private static final String FEATURES_PROPERTY = "features";
    private static final String GEOMETRY_PROPERTY = "geometry";
    private static final String DATE_TIME_PROPERTY = "DateTime";

    /**
     * This method reads a GeoJSON file from the filesystem and parses it as string.
     *
     * @param filePath the location of the file
     * @return string, which contains the file's data
     * @throws IOException an error when there is a problem reading the GeoJSON file
     */
    public static String getJsonDataFromFile(ResourceLoader resourceLoader, String filePath) throws IOException {
        log.debug("Reading GeoJSON data from file: {}", filePath);

        Resource resource = resourceLoader.getResource("classpath:" + filePath);
        if (!resource.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        InputStream inputStream = resource.getInputStream();
        String geoJsonData;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            geoJsonData = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Error reading GeoJSON data", e);
            throw e;
        }

        return geoJsonData;
    }

    /**
     * This method gets the polygon data from a geoJSON string.
     *
     * @param geoJson a string, which contains polygons and their data
     * @return a list with all processed polygons
     * @throws JsonProcessingException an error when there is a problem processing the GeoJSON string
     */
    public static List<Polygon> getPolygonsFromGeoJson(String geoJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geoJson);
        JsonNode features = rootNode.path(FEATURES_PROPERTY);

        List<Polygon> polygons = new ArrayList<>();
        for (JsonNode feature : features) {
            JsonNode coordinates = feature.path(GEOMETRY_PROPERTY).path(COORDINATES_PROPERTY);
            Polygon polygon = convertJsonNodeToPolygon(coordinates);
            polygons.add(polygon);
        }
        log.info("Successfully read {} parking spaces from JSON file.", polygons.size());
        return polygons;
    }

    /**
     * Converts a JsonNode containing coordinates to a JTS Polygon.
     *
     * @param coordinatesNode the JsonNode containing the coordinates
     * @return a JTS Polygon object
     */
    public static Polygon convertJsonNodeToPolygon(JsonNode coordinatesNode) {
        List<Coordinate> coordinatesList = new ArrayList<>();
        for (JsonNode coordinateArray : coordinatesNode.get(0)) { // Assuming the first array contains the polygon coordinates
            double x = coordinateArray.get(0).asDouble();
            double y = coordinateArray.get(1).asDouble();
            coordinatesList.add(new Coordinate(x, y));
        }

        // Close the linear ring if it's not already closed
        if (!coordinatesList.get(0).equals(coordinatesList.get(coordinatesList.size() - 1))) {
            coordinatesList.add(coordinatesList.get(0));
        }

        Coordinate[] coordinates = coordinatesList.toArray(new Coordinate[0]);
        return geometryFactory.createPolygon(coordinates);
    }

    /**
     * This method gets the parking point data from a geoJSON string.
     *
     * @param geoJson a string, which contains parking points and their data
     * @return a hash map with all processed parking points and their timestamps
     * @throws JsonProcessingException an error when there is a problem processing the GeoJSON string
     */
    public static HashMap<ParkingPoint, Timestamp> getParkingPointsAndTimestampsFromFile(String geoJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geoJson);
        JsonNode features = rootNode.path(FEATURES_PROPERTY);

        HashMap<ParkingPoint, Timestamp> pointTimestampHashMap = new HashMap<>();
        for (JsonNode feature : features) {
            JsonNode timestampNode = feature.path("properties").path(DATE_TIME_PROPERTY);
            JsonNode coordinatesNode = feature.path(GEOMETRY_PROPERTY).path(COORDINATES_PROPERTY);
            ParkingPoint parkingPoint = convertJsonNodeToParkingPoint(coordinatesNode);
            Timestamp timestamp = convertJsonNodeToTimestamp(timestampNode);
            pointTimestampHashMap.put(parkingPoint, timestamp);
        }

        log.info("Successfully read {} parking points and their timestamps from JSON file.", pointTimestampHashMap.size());
        return pointTimestampHashMap;
    }

    private static ParkingPoint convertJsonNodeToParkingPoint(JsonNode coordinatesNode) {
        ParkingPoint parkingPoint = new ParkingPoint();

        Coordinate coordinate = new Coordinate(coordinatesNode.get(0).asDouble(), coordinatesNode.get(1).asDouble());
        parkingPoint.setPoint(geometryFactory.createPoint(coordinate));

        return parkingPoint;
    }

    private static Timestamp convertJsonNodeToTimestamp(JsonNode timestampNode) {
        Timestamp timestamp = new Timestamp();

        long milliseconds = Long.parseLong(timestampNode.asText());
        timestamp.setTimestamp(formatMillisecondsDateToString(milliseconds));

        return timestamp;
    }

    public static Point convertGeoJsonToPoint(String pointGeoJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(pointGeoJson);
        JsonNode coordinatesNode = rootNode.path(COORDINATES_PROPERTY);

        Coordinate coordinate = new Coordinate(coordinatesNode.get(0).asDouble(), coordinatesNode.get(1).asDouble());

		return geometryFactory.createPoint(coordinate);
    }

    public static Polygon convertGeoJsonToPolygon(String polygonGeoJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(polygonGeoJson);
        JsonNode typeNode = rootNode.path(TYPE_PROPERTY);
        JsonNode coordinatesNode = rootNode.path(COORDINATES_PROPERTY);

        // In case of a geometry collection when we aggregate
        // using ST_Union take only a polygon or multipolygon
        if (typeNode.asText().equals(GEOMETRY_COLLECTION_TYPE)) {
            typeNode = rootNode.get(GEOMETRIES_PROPERTY);

			for(JsonNode child : typeNode) {
                String typeName = child.get(TYPE_PROPERTY).asText();
				if (typeName.equals(POLYGON_TYPE) || typeName.equals(MULTI_POLYGON_TYPE)) {
                    typeNode = child.get(TYPE_PROPERTY);
                    coordinatesNode = child.get(COORDINATES_PROPERTY);
                    break;
                }
			}
        }

        // In case of multipolygon due to ST_Difference method get the biggest polygon
        if (typeNode.asText().equals(MULTI_POLYGON_TYPE)) {
            int indexChildWithMaxSize = 0;
            int maxSize = 0;
            for(int i = 0; i < coordinatesNode.size(); i++) {
                int currentSize = coordinatesNode.get(i).get(0).size();
                if(currentSize > maxSize) {
                    maxSize = currentSize;
                    indexChildWithMaxSize = i;
                }
            }

            coordinatesNode = coordinatesNode.get(indexChildWithMaxSize);
        }
        return convertJsonNodeToPolygon(coordinatesNode);
    }
}
