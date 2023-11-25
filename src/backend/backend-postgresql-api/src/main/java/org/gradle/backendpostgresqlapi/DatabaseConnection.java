package org.gradle.backendpostgresqlapi;

import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.service.GeospatialService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@Slf4j
@SpringBootApplication
public class DatabaseConnection {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseConnection.class, args);
    }

    @Bean
    ApplicationRunner initializer(GeospatialService geospatialService) {
    return args -> {
            geospatialService.initializeDatabase();

            // Load data into the database
            geospatialService.loadDataIntoDatabase();

            // Calculate the area of the newly inserted park spaces
            geospatialService.calculateAndUpdateAreaColumn();

            // Now retrieve and print all parking spaces.
            List<ParkingSpace> parkingSpaces = geospatialService.getAllParkingSpaces();
            parkingSpaces.forEach(parkingSpace -> System.out.println(parkingSpace.toString()));
        };
    }

}
