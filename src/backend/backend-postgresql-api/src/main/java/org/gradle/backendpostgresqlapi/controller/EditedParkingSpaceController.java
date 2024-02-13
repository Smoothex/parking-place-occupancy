package org.gradle.backendpostgresqlapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.service.EditedParkingSpaceService;
import org.gradle.backendpostgresqlapi.service.ParkingPointService;
import org.gradle.backendpostgresqlapi.service.TimestampService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.*;

import static org.gradle.backendpostgresqlapi.util.DateConverterUtil.formatDateToString;
import static org.gradle.backendpostgresqlapi.util.DateConverterUtil.parseStringToDate;

@RestController
@RequestMapping("/api/parking-spaces")
@CrossOrigin(origins = "http://localhost:4200")
public class EditedParkingSpaceController {

    private final EditedParkingSpaceService editedParkingSpaceService;
    private final ParkingPointService parkingPointService;
    private final TimestampService timestampService;

    @Autowired
    public EditedParkingSpaceController(EditedParkingSpaceService editedParkingSpaceService,
        ParkingPointService parkingPointService, TimestampService timestampService) {
        this.editedParkingSpaceService = editedParkingSpaceService;
        this.parkingPointService = parkingPointService;
        this.timestampService = timestampService;
    }

    // http://localhost:8080/api/parking-spaces
    @GetMapping
    public ResponseEntity<List<EditedParkingSpaceDto>> getAllEditedParkingSpaces() {
        editedParkingSpaceService.updateOccupancyStatusForAllSpaces();
        List<EditedParkingSpaceDto> editedParkingSpaceDtos = editedParkingSpaceService.getAllEditedParkingSpacesAsDto();
        if (editedParkingSpaceDtos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(editedParkingSpaceDtos);
    }

    // http://localhost:8080/api/parking-spaces/1
    @GetMapping("/{id}")
    public ResponseEntity<EditedParkingSpaceDto> getEditedParkingSpaceById(@PathVariable("id") long id) {
        Optional<EditedParkingSpaceDto> editedParkingSpaceDto = editedParkingSpaceService.getEditedParkingSpaceByIdAsDto(id);
        return editedParkingSpaceDto
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // http://localhost:8080/api/parking-spaces/1/area
    @GetMapping("/{id}/area")
    public ResponseEntity<String> getEditedParkingSpaceArea(@PathVariable("id") long id) {
        Optional<String> area = editedParkingSpaceService.getAreaOfEditedParkingSpaceById(id);
        return area
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // http://localhost:8080/api/parking-spaces/search?occupied=true
    @GetMapping("/search")
    public ResponseEntity<List<EditedParkingSpaceDto>> findEditedParkingSpacesByOccupancy(@RequestParam("occupied") boolean occupied) {
        List<EditedParkingSpaceDto> editedParkingSpaceDtos = editedParkingSpaceService.getEditedParkingSpacesByOccupancyAsDto(occupied);
        if (editedParkingSpaceDtos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(editedParkingSpaceDtos);
    }

    // http://localhost:8080/api/parking-spaces/1/polygon
    @PatchMapping("/{id}/polygon")
    public ResponseEntity<EditedParkingSpaceDto> updatePolygonCoordinates(@PathVariable long id,
        @RequestBody JsonNode polygonWithChangedCoordinates) {

        try {
            // update coordinates and re-calculate area
            if (!editedParkingSpaceService.updatePolygonCoordinates(id, polygonWithChangedCoordinates)) {
                return ResponseEntity.notFound().build();
            }

            editedParkingSpaceService.calculateAndUpdateAreaColumnById(id);

            Optional<EditedParkingSpaceDto> updatedParkingSpaceDto = editedParkingSpaceService.getEditedParkingSpaceByIdAsDto(id);
			return updatedParkingSpaceDto.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());

        } catch(IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // http://localhost:8080/api/parking-spaces/1/neighbors
    @GetMapping("/{id}/neighbors")
    public ResponseEntity<List<Long>> getNeighborIds(@PathVariable Long id) {
        List<Long> neighborIds = editedParkingSpaceService.getNeighbors(id);
        return ResponseEntity.ok(neighborIds);
    }

    // http://localhost:8080/api/parking-spaces/1/history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<String>> getAllTimestampsByEditedParkingSpaceId(@PathVariable long id) {

        List<String> timestampsAsStrings = new ArrayList<>();
        List<Date> timestampsAsDates = new ArrayList<>();

        try {
            for (ParkingPoint parkingPoint : parkingPointService.getAllParkingPointsByEditedParkingSpaceId(id)) {
                timestampsAsStrings.addAll(timestampService.getAllTimestampsByParkingPointId(parkingPoint.getId()));
            }

            for (String timestampStr : timestampsAsStrings) {
                timestampsAsDates.add(parseStringToDate(timestampStr));
            }
        } catch(ParseException exception) {
            return ResponseEntity.internalServerError().build();
        }

        // Sort dates, format them back to strings and remove duplicates
        Collections.sort(timestampsAsDates);
        timestampsAsStrings.clear();
        timestampsAsDates.forEach(t -> timestampsAsStrings.add(formatDateToString(t)));

        return ResponseEntity.ok( new ArrayList<>(new LinkedHashSet<>(timestampsAsStrings)));
    }
}
