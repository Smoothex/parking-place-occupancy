package org.gradle.backendpostgresqlapi;

import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.service.EditedParkingSpaceService;
import org.gradle.backendpostgresqlapi.service.ParkingSpaceService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@Slf4j
@SpringBootApplication
public class DatabaseConnection {

    private static final boolean LOAD_DATA_FIRST_TIME = true;

    public static void main(String[] args) {
        SpringApplication.run(DatabaseConnection.class, args);
    }

    @Bean
    ApplicationRunner initializer(ParkingSpaceService parkingSpaceService, EditedParkingSpaceService editedParkingSpaceService) {
    return args -> {
            // Initialize an index for parking_spaces database
            parkingSpaceService.initializeDatabaseIndex();

            // Load data into parking_spaces database
            if (LOAD_DATA_FIRST_TIME)
                parkingSpaceService.loadDataIntoDatabase();

            // Calculate the area of the inserted park spaces
            parkingSpaceService.calculateAndUpdateAreaColumn();

            // Load data into edited_parking_spaces database
            if (LOAD_DATA_FIRST_TIME)
                editedParkingSpaceService.copyDataIntoDatabase();

            // Now retrieve and print all edited parking spaces.
            List<EditedParkingSpace> editedParkingSpaces = editedParkingSpaceService.getAllEditedParkingSpaces();
            editedParkingSpaces.forEach(eps -> System.out.println(eps.toString()));

            //List<ParkingSpace> parkingSpaces = parkingSpaceService.getAllParkingSpaces();
            //parkingSpaces.forEach(parkingSpace -> System.out.println(parkingSpace.toString()));
        };
    }
}
