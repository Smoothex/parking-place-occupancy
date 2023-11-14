package org.gradle.backendpostgresqlapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.GeometryFactory;

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
    public static String getJsonDataFromFile(String filePath) throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        InputStream inputStream = resourceLoader.getResource(filePath).getInputStream();

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
     * @param geoJson a string, which contains polygons' data
     * @return a list with all the read polygons
     * @throws JsonProcessingException an error when there is a problem processing the GeoJSON string
     */
    public static List<Polygon> getPolygonsFromGeoJson(String geoJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geoJson);
        JsonNode features = rootNode.path("features");
    
        List<Polygon> polygons = new ArrayList<>();
        for (JsonNode feature : features) {
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            Polygon polygon = jsonNodeToPolygon(coordinates);
            polygons.add(polygon);
        }
        return polygons;
    }

    /**
     * Converts a JsonNode containing coordinates to a JTS Polygon.
     *
     * @param coordinatesNode the JsonNode containing the coordinates
     * @return a JTS Polygon object
     */
    private static Polygon jsonNodeToPolygon(JsonNode coordinatesNode) {
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

    public static String polygonToJson(Polygon polygon) {
        if (polygon == null) return null;
    
        StringBuilder json = new StringBuilder();
        json.append("{\"coordinates\": [");
    
        for (Coordinate coord : polygon.getCoordinates()) {
            json.append(String.format(Locale.US, "{\"x\": %.15f, \"y\": %.15f},", coord.x, coord.y));
        }
    
        // Remove the trailing comma
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }
    
        json.append("]}");
        return json.toString();
    }
    
    public static String convertParkingSpaceToJson(ParkingSpace parkingSpace) {
        ObjectMapper mapper = new ObjectMapper();

        // Convert Polygon to JSON
        String polygonJson = JsonHandler.polygonToJson(parkingSpace.getPolygon());

        try {
            // Construct a JSON object
            ObjectNode parkingSpaceJson = mapper.createObjectNode();
            parkingSpaceJson.put("id", parkingSpace.getId());
            parkingSpaceJson.set("polygon", mapper.readTree(polygonJson));
            parkingSpaceJson.put("occupied", parkingSpace.isOccupied());

            // Convert the whole object to a JSON string
            return mapper.writeValueAsString(parkingSpaceJson);
        } catch (Exception e) {
            // Handle exceptions (logging, re-throwing, etc.)
            throw new RuntimeException("Error converting ParkingSpace to JSON", e);
        }
    }
    
}