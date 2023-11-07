package org.gradle.backendpostgresqlapi;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.util.List;

import org.gradle.backendpostgresqlapi.dto.ParkingSpaceDTO;
import org.gradle.backendpostgresqlapi.service.GeospatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootApplication
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    public static void main(String[] args) {
        SpringApplication.run(DatabaseConnection.class, args);
    }

    /**
     * ApplicationRunner bean that is used to run the initialization logic. This method is executed
     * at the start-up of the application. It initializes the database schema and loads GeoJSON data
     * into the database.
     *
     * @param jdbcTemplate the JdbcTemplate bean used for database operations
     * @param geospatialService the service bean that provides geospatial operations
     * @return an ApplicationRunner bean that performs database initialization and data loading
     */
    @Bean
    @Order(1)
    ApplicationRunner initializer(GeospatialService geospatialService) {
        return args -> {
            geospatialService.initializeDatabase();
            geospatialService.loadGeoJsonDataIntoDatabase();
        };
    }

    /**
     * Creates a CommandLineRunner bean that is run after all the Spring Beans are created and registered.
     * It retrieves the GeospatialService bean from the application context, fetches all parking spaces,
     * and then prints them out.
     *
     * @param geospatialService the service bean that provides geospatial operations
     * @return a CommandLineRunner bean that executes the logic defined in the run method
     */
    @Bean
    @Order(2)
    public CommandLineRunner commandLineRunner(GeospatialService geospatialService) {
        return args -> {
            List<ParkingSpaceDTO> parkingSpaces = geospatialService.getParkingSpaces();
            parkingSpaces.forEach(space -> System.out.println(space));
        };
    }
}
