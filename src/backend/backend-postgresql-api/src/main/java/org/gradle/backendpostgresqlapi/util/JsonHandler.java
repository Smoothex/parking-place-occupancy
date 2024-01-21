package org.gradle.backendpostgresqlapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
public class JsonHandler {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

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
        JsonNode features = rootNode.path("features");

        List<Polygon> polygons = new ArrayList<>();
        for (JsonNode feature : features) {
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            Polygon polygon = convertJsonNodeToPolygon(coordinates);
            polygons.add(polygon);
        }
        log.info("Successfully loaded {} parking spaces from JSON file.", polygons.size());
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
        JsonNode features = rootNode.path("features");

        HashMap<ParkingPoint, Timestamp> pointTimestampHashMap = new HashMap<>();
        for (JsonNode feature : features) {
            JsonNode timestampNode = feature.path("properties").path("DateTime");
            JsonNode coordinatesNode = feature.path("geometry").path("coordinates");
            ParkingPoint parkingPoint = convertJsonNodeToParkingPoint(coordinatesNode);
            Timestamp timestamp = convertJsonNodeToTimestamp(timestampNode);
            pointTimestampHashMap.put(parkingPoint, timestamp);
        }

        log.info("Successfully loaded {} parking points and their timestamps from JSON file.", pointTimestampHashMap.size());
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
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
        timestamp.setTimestamp(dateFormat.format(milliseconds));

        return timestamp;
    }
}
