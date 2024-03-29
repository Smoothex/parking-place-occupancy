package org.gradle.backendpostgresqlapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.gradle.backendpostgresqlapi.repository.EditedParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.repository.ParkingSpaceRepo;
import org.gradle.backendpostgresqlapi.util.DtoConverterUtil;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gradle.backendpostgresqlapi.util.DateConverterUtil.calculateDifferenceInDays;
import static org.gradle.backendpostgresqlapi.util.JsonHandler.convertJsonNodeToPolygon;
import static org.gradle.backendpostgresqlapi.util.TableNameUtil.EDITED_PARKING_SPACES;

@Slf4j
@Service
public class EditedParkingSpaceService {

    private final EditedParkingSpaceRepo editedParkingSpaceRepo;
    private final ParkingSpaceRepo parkingSpaceRepo;
    private static final int DAYS_FOR_VALID_OCCUPANCY = 180;

    @Autowired
    public EditedParkingSpaceService(ParkingSpaceRepo parkingSpaceRepo, EditedParkingSpaceRepo editedParkingSpaceRepo) {
        this.parkingSpaceRepo = parkingSpaceRepo;
        this.editedParkingSpaceRepo = editedParkingSpaceRepo;
    }

    public void copyDataIntoDatabase() throws IOException {
        log.info("Copying data to '{}' table...", EDITED_PARKING_SPACES);
        long duplicates = 0;

        for (long i = 1; i <= parkingSpaceRepo.count(); i++) {
            Optional<ParkingSpace> parkingSpace = parkingSpaceRepo.findById(i);
            if (parkingSpace.isPresent()) {
                ParkingSpace existingParkingSpace = parkingSpace.get();

                // Check if a parking space with reference to the current id already exists in the second database
                boolean isDuplicate = editedParkingSpaceRepo.existsByParkingSpaceId(existingParkingSpace.getId());

                if (!isDuplicate) {
                    EditedParkingSpace editedParkingSpace = convertToEditedParkingSpace(existingParkingSpace);
                    editedParkingSpaceRepo.save(editedParkingSpace);
                } else {
                    duplicates++;
                }
            } else {
                throw new IOException(String.format("Error on copying data into '%s'.", EDITED_PARKING_SPACES));
            }
        }

        if (duplicates > 0) {
            log.warn("{} duplicates were not copied, because data already exists in the '{}' table.", duplicates, EDITED_PARKING_SPACES);
        }
        log.info("Successfully copied data to '{}'", EDITED_PARKING_SPACES);
    }

    public void calculateAndUpdateAreaColumnById(long id) {
        log.debug("Calculating and updating area column in '{}' table...", EDITED_PARKING_SPACES);
        editedParkingSpaceRepo.updateAreaColumnById(id);
        log.info("Area column values were calculated and set accordingly for '{}'.", EDITED_PARKING_SPACES);
    }

    public List<EditedParkingSpace> getAllEditedParkingSpaces() {
        return editedParkingSpaceRepo.findAll();
    }    

    public List<EditedParkingSpaceDto> getAllEditedParkingSpacesAsDto() {
        return getAllEditedParkingSpaces().stream()
                            .map(DtoConverterUtil::convertToDto)
                            .collect(Collectors.toList());
    }

    private Optional<EditedParkingSpace> getEditedParkingSpaceById(long id) {
        return editedParkingSpaceRepo.findById(id);
    }

    public Optional<EditedParkingSpaceDto> getEditedParkingSpaceByIdAsDto(long id) {
        return getEditedParkingSpaceById(id)
                             .map(DtoConverterUtil::convertToDto);
    }

    public List<EditedParkingSpaceDto> getEditedParkingSpacesByOccupancyAsDto(boolean occupied) {
        List<EditedParkingSpace> editedParkingSpaces = editedParkingSpaceRepo.findByOccupied(occupied);
        return editedParkingSpaces.stream()
                    .map(DtoConverterUtil::convertToDto)
                    .collect(Collectors.toList());
    }

    /**
     * Checks for each edited parking space if there is a timestamp related to it, which is sooner than
     * a predefined threshold, and updates the occupancy status respectively.
     */
    public void updateOccupancyStatusForAllSpaces() {
        log.info("Updating occupancy status for all parking spaces...");
        for (EditedParkingSpace editedParkingSpace : getAllEditedParkingSpaces()) {
            boolean isOccupied = false;
            for (ParkingPoint parkingPoint : editedParkingSpace.getParkingPoints()) {
                for (Timestamp  timestamp : parkingPoint.getTimestamps()) {
                    long daysPassedTimestamp = calculateDifferenceInDays(timestamp.getTimestamp());

                    if (daysPassedTimestamp < DAYS_FOR_VALID_OCCUPANCY) {
                        isOccupied = true;
                        break;
                    }
                }

                if (isOccupied) {
                    break;
                }
            }

            // Check if the occupancy status have to be updated
            if (editedParkingSpace.isOccupied() != isOccupied) {
                editedParkingSpace.setOccupied(isOccupied);
                editedParkingSpaceRepo.save(editedParkingSpace);
            }
        }
        log.info("Occupancy status updated!");
    }

    public Optional<String> getAreaOfEditedParkingSpaceById(long id) {
        return getEditedParkingSpaceById(id).map(EditedParkingSpace::getArea).map(Object::toString);
    }

    public boolean updatePolygonCoordinates(long id, JsonNode polygonWithChangedCoordinates) {
        Optional<EditedParkingSpace> editedParkingSpaceOptional = getEditedParkingSpaceById(id);

        if (editedParkingSpaceOptional.isPresent()) {
            EditedParkingSpace editedParkingSpace = editedParkingSpaceOptional.get();

            // convert new coordinates to a polygon
            Polygon editedPolygon = convertJsonNodeToPolygon(polygonWithChangedCoordinates);

            editedParkingSpace.setPolygon(editedPolygon);
            editedParkingSpaceRepo.save(editedParkingSpace);
            return true;
        }
        return false;
    }

    private EditedParkingSpace convertToEditedParkingSpace(ParkingSpace parkingSpace) {
		return new EditedParkingSpace(parkingSpace.getId(), parkingSpace.getPolygon(),
            parkingSpace.getArea(), parkingSpace.getCapacity(), parkingSpace.getPosition());
    }

    public List<Long> getNeighbors(Long id) {
        editedParkingSpaceRepo.findById(id)
                              .orElseThrow(() -> new ResourceAccessException("Parking space with id: " + id + " not found."));

		return editedParkingSpaceRepo.findNeighborIds(id);
    }
}
