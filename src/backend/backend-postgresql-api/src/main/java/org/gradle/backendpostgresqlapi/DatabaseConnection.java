package org.gradle.backendpostgresqlapi;

import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.configuration.GeoDataFile;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.service.EditedParkingSpaceService;
import org.gradle.backendpostgresqlapi.service.ParkingSpaceService;
import org.gradle.backendpostgresqlapi.service.ParkingPointService;
import org.gradle.backendpostgresqlapi.service.TimestampService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.gradle.backendpostgresqlapi.util.DataLoaderUtil.loadDataIntoDatabase;

@Slf4j
@SpringBootApplication
public class DatabaseConnection {

    private static final boolean LOADING_DATA_REQUIRED = true;
    private static final boolean PRINT_EDITED_PARKING_SPACES = false;
    private static final boolean PRINT_PARKING_SPACES = false;

    public static void main(String[] args) {
        SpringApplication.run(DatabaseConnection.class, args);
    }

    @Bean
    ApplicationRunner initializer(ParkingSpaceService parkingSpaceService, EditedParkingSpaceService editedParkingSpaceService,
        ParkingPointService parkingPointService, TimestampService timestampService, GeoDataFile geoDataFile) {
        return args -> {
            // Initialize indexes for some tables
            parkingSpaceService.initializeDatabaseIndex();
            parkingPointService.initializeDbIndex();
            timestampService.initializeDbIndex();

            if (LOADING_DATA_REQUIRED) {
                // Data is loaded regarding the content of the file in the db
                List<String> filePaths = geoDataFile.getPaths();
                loadDataIntoDatabase(filePaths, parkingSpaceService, editedParkingSpaceService, parkingPointService);
            }

            if (PRINT_EDITED_PARKING_SPACES) {
                // Now retrieve and print all edited parking spaces
                List<EditedParkingSpace> editedParkingSpaces = editedParkingSpaceService.getAllEditedParkingSpaces();
                editedParkingSpaces.forEach(eps -> System.out.println(eps.toString()));
            }

            if (PRINT_PARKING_SPACES) {
                // Now retrieve and print all parking spaces.
                List<ParkingSpace> parkingSpaces = parkingSpaceService.getAllParkingSpaces();
                parkingSpaces.forEach(parkingSpace -> System.out.println(parkingSpace.toString()));
            }

            log.info("Program is running!");
        };
    }
}
