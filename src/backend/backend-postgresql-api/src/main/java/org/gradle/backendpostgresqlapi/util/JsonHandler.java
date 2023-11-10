package org.gradle.backendpostgresqlapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

@Slf4j
public class JsonHandler {

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
    public static List<String> getPolygonsFromGeoJson(String geoJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geoJson);
        JsonNode features = rootNode.path("features");

        List<String> polygons = new ArrayList<>();
        for (JsonNode feature: features) {
            polygons.add(feature.path("geometry").toString());
        }
        return polygons;
    }

    /**
     * This method gets the coordinates of a polygon from a geoJSON string.
     *
     * @param geoJson a string, which contains polygons' coordinates
     * @return a string, which represents the coordinates of a polygon
     * @throws JsonProcessingException an error when there is a problem processing the GeoJSON string
     */
    public static String getCoordinatesFromGeoJson(String geoJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geoJson);
        return rootNode.at("/coordinates").toString();
    }
}
