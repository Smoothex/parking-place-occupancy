package org.gradle.backendpostgresqlapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class DatabaseConnection {

    private static final String GEOJSON_FILE = "/tmp/data.geojson";
    private static Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    public static void main(String[] args) {
        SpringApplication.run(DatabaseConnection.class, args);
        try {
            setupDatabaseConnection();
            initializeDatabase();
            loadGeoJsonDataIntoDatabase();
            printDatabaseContents();
            closeDatabaseConnection();
        } catch (IOException | SQLException | InterruptedException e) {
            logger.error("An exception occurred: {}", e.getMessage(), e);
        }
    }

    /**
     * Initializes a connection to the PostgreSQL database.
     * Configures the connection for either a Docker environment or a local setup.
     * A delay is included when running in Docker to allow for the database service to start.
     * 
     * @throws SQLException if unable to establish a database connection.
     * @throws InterruptedException if the thread sleep is interrupted.
     */
    private static void setupDatabaseConnection() throws SQLException, InterruptedException {
        boolean isExecutedInDocker = true; // change when using it locally for testing
        String dbUser = "parkuser";
        String dbName = "parking_spots_db";
        String hostName = isExecutedInDocker ? "postgres" : "localhost";
        String containerPort = isExecutedInDocker ? "5432" : "32768";
        String url = "jdbc:postgresql://" + hostName + ":" + containerPort + "/" + dbName;
        Properties connectionProp = new Properties();
        connectionProp.setProperty("user", dbUser);

        logger.debug("Attempting to establish a database connection...");
        if (isExecutedInDocker) {
            logger.debug("Application running in Docker. Waiting for the database service to start...");
            Thread.sleep(10000); // sleep for 10s so that PostgreSQL can initialize properly
        }
        connection = DriverManager.getConnection(url, connectionProp);
        if (connection != null) {
            logger.info("Connected to database {}", dbName);
        } else {
            logger.error("Failed to connect to database {}", dbName);
        }
    }

    /**
     * Creates the {@code parking_spaces} table and its associated GIST index if they do not exist.
     * Designed to store geolocation data with the GEOGRAPHY data type.
     * 
     * @throws SQLException if there is an error executing the SQL commands to create the table or index.
     */
    private static void initializeDatabase() throws SQLException {
        logger.debug("Initializing the database...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS parking_spaces (" +
                "id SERIAL PRIMARY KEY," +
                "geolocation GEOGRAPHY(POLYGON, 4326)" + // SRID 4326 for WGS 84
                ");"
            );
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS parking_spaces_geolocation_idx " +
                "ON parking_spaces USING GIST (geolocation);"
            );
            logger.info("Database initialized with table parking_spaces and associated index.");
        } catch (SQLException e) {
            logger.error("Error initializing the database: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Loads GeoJSON data from a predefined file path, parses it, and inserts
     * the geometry data into the {@code parking_spaces} table using a prepared statement.
     *
     * @throws IOException if there is an issue reading the GeoJSON file.
     * @throws SQLException if there is an error executing the insert statement.
     */
    private static void loadGeoJsonDataIntoDatabase() throws IOException, SQLException {
        logger.debug("Loading GeoJSON data into the database...");
        String geoJsonData = new String(Files.readAllBytes(Paths.get(GEOJSON_FILE)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(geoJsonData);
        JsonNode features = rootNode.path("features");

        try (PreparedStatement pstmt = connection.prepareStatement("INSERT INTO parking_spaces (geolocation) VALUES (ST_GeomFromGeoJSON(?))")) {
            for (JsonNode feature : features) {
                String geometry = feature.path("geometry").toString();
                pstmt.setString(1, geometry);
                pstmt.executeUpdate();
            }
            logger.info("GeoJSON data successfully loaded into the database.");
        } catch (SQLException e) {
            logger.error("Error loading GeoJSON data into the database: {}", e.getMessage(), e);
            throw e;
        }
    }

    private static void printDatabaseContents() throws SQLException {
        logger.debug("Printing database contents...");
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, ST_AsGeoJSON(geolocation) AS geolocation FROM parking_spaces");
            while (rs.next()) {
                int id = rs.getInt("id");
                String geolocation = rs.getString("geolocation");
                logger.info("ID: {}, Geolocation: {}", id, geolocation);
            }
        } catch (SQLException e) {
            logger.error("Error printing database contents: {}", e.getMessage(), e);
            throw e;
        }
    }

    private static void closeDatabaseConnection() throws SQLException {
        logger.debug("Closing database connection...");
        if (connection != null && !connection.isClosed()) {
            connection.close();
            logger.info("Database connection closed.");
        } else {
            logger.error("Database connection was already closed or it was never opened.");
        }
    }
}
