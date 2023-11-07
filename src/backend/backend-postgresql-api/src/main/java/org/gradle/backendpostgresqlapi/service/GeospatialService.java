package org.gradle.backendpostgresqlapi.service;

import org.gradle.backendpostgresqlapi.dto.ParkingSpaceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@Service
public class GeospatialService {

    private static final Logger logger = LoggerFactory.getLogger(GeospatialService.class);
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    private static final String GEOJSON_FILE = "classpath:data.geojson";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS parking_spaces (id SERIAL PRIMARY KEY, geolocation GEOGRAPHY(POLYGON, 4326));";
    private static final String CREATE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS parking_spaces_geolocation_idx ON parking_spaces USING GIST (geolocation);";
    private static final String INSERT_GEOJSON_SQL = "INSERT INTO parking_spaces (geolocation) VALUES (ST_GeomFromGeoJSON(?))";
    private static final String SELECT_PARKING_SPACES_SQL = "SELECT id, ST_AsGeoJSON(geolocation)::json AS geolocation FROM parking_spaces";


    public GeospatialService(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Initializes the database schema for storing parking spaces.
     * Creates a table and a spatial index if they do not already exist.
     *
     */
    @Transactional
    public void initializeDatabase() {
        logger.debug("Initializing the database...");
        jdbcTemplate.execute(CREATE_TABLE_SQL);
        jdbcTemplate.execute(CREATE_INDEX_SQL);
        logger.info("Database initialized with table parking_spaces and index parking_spaces_geolocation_idx.");
    }

    /**
     * Loads data from a GeoJSON file into the database.
     * The method reads a GeoJSON file from the filesystem, parses it, and then inserts the data
     * into the `parking_spaces` table using a batch update operation.
     *
     * @throws IOException If there is a problem reading the GeoJSON file.
     */
    @Transactional
    public void loadGeoJsonDataIntoDatabase() throws IOException {
        logger.debug("Loading GeoJSON data into the database...");

        // Get the resource as an InputStream (and not as a File, since it is embedded in the .jar under /src/main/resources and it's not on the container's file system)
        InputStream inputStream = resourceLoader.getResource(GEOJSON_FILE).getInputStream();
        
        // Read from the InputStream using BufferedReader
        String geoJsonData;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            geoJsonData = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            logger.error("Error reading GeoJSON data", e);
            throw e;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geoJsonData);
        JsonNode features = rootNode.path("features");

        List<JsonNode> featuresList = new ArrayList<>();
        for (JsonNode feature : features) {
            featuresList.add(feature);
        }

        // Execute the batch update as before
        jdbcTemplate.batchUpdate(
            INSERT_GEOJSON_SQL,
            featuresList,
            featuresList.size(),
            (PreparedStatement ps, JsonNode feature) -> {
                // Convert the JsonNode to a String representation of the geometry
                String geometryJson = feature.path("geometry").toString();
                // Set the string in the prepared statement
                ps.setString(1, geometryJson);
            }
        );

        logger.info("GeoJSON data successfully loaded into the database.");
    }


    /**
     * Fetches all parking space records from the database and maps them to a list of ParkingSpaceDTO objects.
     * This method executes a SQL query to retrieve parking space data, which is then transformed to DTOs.
     *
     * @return A List of ParkingSpaceDTO objects representing the parking spaces in the database.
     * @throws SQLException If there is a problem accessing the database or mapping the result set to DTOs.
     */
    public List<ParkingSpaceDTO> getParkingSpaces() {
        return jdbcTemplate.query(SELECT_PARKING_SPACES_SQL, (rs, rowNum) -> {
            int parkingSpaceId = rs.getInt("id");
            String parkingSpaceGeojson = rs.getString("geolocation");
            ParkingSpaceDTO parkingSpace = new ParkingSpaceDTO(parkingSpaceId, parkingSpaceGeojson);
            
            return parkingSpace;
        });
    }
}
