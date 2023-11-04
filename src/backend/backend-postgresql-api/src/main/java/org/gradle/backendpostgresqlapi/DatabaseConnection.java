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

@SpringBootApplication
public class DatabaseConnection {

    private static final String GEOJSON_FILE = "/tmp/data.geojson";
    private static Connection connection;

    public static void main(String[] args) {
        SpringApplication.run(DatabaseConnection.class, args);
        try {
            setupDatabaseConnection();
            initializeDatabase();
            loadGeoJsonDataIntoDatabase();
            printDatabaseContents();
            closeDatabaseConnection();
        } catch (IOException | SQLException | InterruptedException e) {
            e.printStackTrace();
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

        if (isExecutedInDocker) {
            Thread.sleep(10000); // sleep for 10s so that PostgreSQL can initialize properly
        }
        connection = DriverManager.getConnection(url, connectionProp);
        if (connection != null) {
            System.out.println("Connected to the database!");
        }
    }

    /**
     * Creates the {@code parking_spaces} table and its associated GIST index if they do not exist.
     * Designed to store geolocation data with the GEOGRAPHY data type.
     * 
     * @throws SQLException if there is an error executing the SQL commands to create the table or index.
     */
    private static void initializeDatabase() throws SQLException {
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
            System.out.println("parking_spaces table created.");
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
            System.out.println("GeoJSON data loaded into the database.");
        }
    }

    private static void printDatabaseContents() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, ST_AsGeoJSON(geolocation) AS geolocation FROM parking_spaces");
            while (rs.next()) {
                int id = rs.getInt("id");
                String geolocation = rs.getString("geolocation"); // This will be a GeoJSON string
                System.out.println("ID: " + id + ", Geolocation: " + geolocation);
            }
        }
    }

    private static void closeDatabaseConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Disconnected from the database.");
        }
    }
}
