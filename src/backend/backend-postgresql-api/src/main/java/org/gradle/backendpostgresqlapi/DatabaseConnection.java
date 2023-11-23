package org.gradle.backendpostgresqlapi;

import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.service.GeospatialService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.util.List;

@Slf4j
@SpringBootApplication
public class DatabaseConnection {

    private static String inputDataFormat = "csv";
    public static void main(String[] args) {
        SpringApplication.run(DatabaseConnection.class, args);
    }

    /**
     * ApplicationRunner bean that is used to run the initialization logic. This method is executed
     * at the start-up of the application. It initializes the database schema and loads GeoJSON data
     * into the database. Lastly, area of each polygon is calculated and set.
     *
     * @param geospatialService the service bean that provides geospatial operations
     * @return an ApplicationRunner bean that performs database initialization and data loading
     */
    @Bean
    @Order(1)
    ApplicationRunner initializer(GeospatialService geospatialService) {
        return args -> {
            geospatialService.initializeDatabase();
            if (inputDataFormat.equals("geojson")) {
                geospatialService.loadGeoJsonDataIntoDatabase();
            } else {
              geospatialService.loadCsvDataIntoDatabase();  
            }
        };
    }

    /**
     * Creates a ApplicationRunner bean that is run after all the Spring Beans are created and registered.
     * It retrieves the GeospatialService bean from the application context, fetches all parking spaces,
     * and then prints them out.
     *
     * @param geospatialService the service bean that provides geospatial operations
     * @return a ApplicationRunner bean that executes the logic defined in the run method
     */
    @Bean
    @Order(2)
    public ApplicationRunner printParkingSpaces(GeospatialService geospatialService) {
        return args -> {
            List<ParkingSpace> parkingSpaces = geospatialService.getAllParkingSpaces();
            parkingSpaces.forEach(parkingSpace -> System.out.println(parkingSpace.toString()));
        };
    }
}
