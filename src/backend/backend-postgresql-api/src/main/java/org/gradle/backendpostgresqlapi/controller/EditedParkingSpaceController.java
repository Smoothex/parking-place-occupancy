package org.gradle.backendpostgresqlapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.dto.ParkingPointDto;
import org.gradle.backendpostgresqlapi.dto.TimestampDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.service.EditedParkingSpaceService;
import org.gradle.backendpostgresqlapi.service.ParkingSpaceService;
import org.gradle.backendpostgresqlapi.service.ParkingPointService;
import org.gradle.backendpostgresqlapi.service.TimestampService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import static org.gradle.backendpostgresqlapi.util.DtoConverterUtil.convertToDto;

@RestController
@RequestMapping("/api/parking-spaces")
@CrossOrigin(origins = "http://localhost:4200")
public class EditedParkingSpaceController {

    private final EditedParkingSpaceService editedParkingSpaceService;
    private final ParkingSpaceService parkingSpaceService;
    private final ParkingPointService parkingPointService;
    private final TimestampService timestampService;

    @Autowired
    public EditedParkingSpaceController(EditedParkingSpaceService editedParkingSpaceService, ParkingSpaceService parkingSpaceService,
        ParkingPointService parkingPointService, TimestampService timestampService) {
        this.editedParkingSpaceService = editedParkingSpaceService;
        this.parkingSpaceService = parkingSpaceService;
        this.parkingPointService = parkingPointService;
        this.timestampService = timestampService;
    }

    // http://localhost:8080/api/parking-spaces
    @GetMapping
    public ResponseEntity<List<EditedParkingSpaceDto>> getAllEditedParkingSpaces() {
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

    // http://localhost:8080/api/parking-spaces/1/occupancy?occupied=true
    @PatchMapping("/{id}/occupancy")
    public ResponseEntity<EditedParkingSpaceDto> updateOccupancyStatus(@PathVariable long id, @RequestParam boolean occupied) {
        try {
            EditedParkingSpace updatedParkingSpace = editedParkingSpaceService.updateOccupancyStatus(id, occupied);
            if (updatedParkingSpace != null) {
                return ResponseEntity.ok(convertToDto(updatedParkingSpace));
            }
        } catch(IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.notFound().build();
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

    // http://localhost:8080/api/parking-spaces/1/parking_points
    @GetMapping("/{edited_space_id}/parking_points")
    public ResponseEntity<List<ParkingPointDto>> getAllParkingPointsByEditedParkingSpaceId(@PathVariable long edited_space_id) {
        List<ParkingPointDto> parkingPointDtos = parkingPointService.getAllParkingPointsByEditedParkingSpaceIdAsDto(edited_space_id);
        if (parkingPointDtos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(parkingPointDtos);
    }

    // http://localhost:8080/api/parking-spaces/1/timestamps
    @GetMapping("/{parking_point_id}/timestamps")
    public ResponseEntity<List<TimestampDto>> getAllTimestampsByParkingPointId(@PathVariable long parking_point_id) {
        List<TimestampDto> timestampDtos = timestampService.getAllTimestampsByParkingPointIdAsDto(parking_point_id);
        return ResponseEntity.ok(timestampDtos);
    }

    @GetMapping("/{id}/neighbors")
    public ResponseEntity<List<Long>> getNeighborIds(@PathVariable Long id) {
        List<Long> neighborIds = editedParkingSpaceService.getNeighbors(id);
        return ResponseEntity.ok(neighborIds);
    }
}
