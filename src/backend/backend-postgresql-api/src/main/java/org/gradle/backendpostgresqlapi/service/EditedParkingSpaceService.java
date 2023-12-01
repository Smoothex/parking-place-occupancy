package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.repository.EditedParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.ParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.util.JsonHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EditedParkingSpaceService {

    private final EditedParkingSpaceRepo editedParkingSpaceRepo;
    private final ParkingSpaceRepo parkingSpaceRepo;

    @Autowired
    public EditedParkingSpaceService(ParkingSpaceRepo parkingSpaceRepo, EditedParkingSpaceRepo editedParkingSpaceRepo) {
        this.parkingSpaceRepo = parkingSpaceRepo;
        this.editedParkingSpaceRepo = editedParkingSpaceRepo;
    }

    /**
     * Initializes the database schema for storing edited parking spaces.
     */
    public void initializeDatabase() {
        log.debug("Initializing table 'edited_parking_spaces' ...");
        editedParkingSpaceRepo.createEditedDataTable();
        log.info("Table 'edited_parking_spaces' initialized.");
    }

    public void copyDataIntoDatabase() throws IOException{
        for (int i = 1; i <= parkingSpaceRepo.count(); i++) {
            Optional<ParkingSpace> parkingSpace = parkingSpaceRepo.findById(i);
            if (parkingSpace.isPresent()) {
                EditedParkingSpace editedParkingSpace = convertToEditedParkingSpace(parkingSpace.get());
                editedParkingSpaceRepo.save(editedParkingSpace);
            } else {
                throw new IOException("Error on copying data.");
            }
        }
    }

    public void calculateAndUpdateAreaColumnById(int id) {
        log.debug("Calculating and updating area column in the database...");
        editedParkingSpaceRepo.updateAreaColumnById(id);
        log.info("Area column values were calculated and set accordingly.");
    }

    public List<EditedParkingSpace> getAllEditedParkingSpaces() {
        return editedParkingSpaceRepo.findAll();
    }    

    public List<String> getAllEditedParkingSpacesAsJson() {
        return getAllEditedParkingSpaces().stream()
                            .map(JsonHandler::convertEditedParkingSpaceToJson)
                            .collect(Collectors.toList());
    }

    private Optional<EditedParkingSpace> getEditedParkingSpaceById(int id) {
        return editedParkingSpaceRepo.findById(id);
    }

    public Optional<String> getEditedParkingSpaceByIdAsJson(int id) {
        return getEditedParkingSpaceById(id)
                             .map(JsonHandler::convertEditedParkingSpaceToJson);
    }

    public List<String> getEditedParkingSpacesByOccupancyAsJson(boolean occupied) {
        List<EditedParkingSpace> editedParkingSpaces = editedParkingSpaceRepo.findByOccupied(occupied);
        return editedParkingSpaces.stream()
                    .map(JsonHandler::convertEditedParkingSpaceToJson)
                    .collect(Collectors.toList());
    }

    public boolean updateOccupancyStatus(int id, boolean occupied) {
        Optional<EditedParkingSpace> editedParkingSpaceOptional = getEditedParkingSpaceById(id);
        if (editedParkingSpaceOptional.isPresent()) {
            EditedParkingSpace editedParkingSpace = editedParkingSpaceOptional.get();
            editedParkingSpace.setOccupied(occupied);
            editedParkingSpaceRepo.save(editedParkingSpace);
            return true;
        }
        return false;
    }

    public Optional<String> getAreaOfEditedParkingSpaceById(int id) {
        return getEditedParkingSpaceById(id).map(EditedParkingSpace::getArea).map(Object::toString);
    }

    private EditedParkingSpace convertToEditedParkingSpace(ParkingSpace parkingSpace) {
        EditedParkingSpace editedParkingSpace = new EditedParkingSpace();
        editedParkingSpace.setParkingSpaceId(parkingSpace.getId());
        editedParkingSpace.setPolygon(parkingSpace.getPolygon());
        editedParkingSpace.setCapacity(parkingSpace.getCapacity());
        editedParkingSpace.setArea(parkingSpace.getArea());
        editedParkingSpace.setPosition(parkingSpace.getPosition());
        return editedParkingSpace;
    }
}
