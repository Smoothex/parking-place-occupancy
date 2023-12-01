package org.gradle.backendpostgresqlapi.controller;

import org.gradle.backendpostgresqlapi.service.EditedParkingSpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/parking-spaces")
@CrossOrigin(origins = "http://localhost:4200")
public class ParkingSpaceController {

    private final EditedParkingSpaceService editedParkingSpaceService;

    @Autowired
    public ParkingSpaceController(EditedParkingSpaceService editedParkingSpaceService) {
        this.editedParkingSpaceService = editedParkingSpaceService;
    }

    // http://localhost:8080/api/parking-spaces
    @GetMapping
    public ResponseEntity<List<String>> getAllEditedParkingSpaces() {
        List<String> editedParkingSpaces = editedParkingSpaceService.getAllEditedParkingSpacesAsJson();
        return ResponseEntity.ok(editedParkingSpaces);
    }

    // http://localhost:8080/api/parking-spaces/1
    @GetMapping("/{id}")
    public ResponseEntity<String> getEditedParkingSpaceById(@PathVariable("id") int id) {
        Optional<String> editedParkingSpace = editedParkingSpaceService.getEditedParkingSpaceByIdAsJson(id);
        return editedParkingSpace
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // http://localhost:8080/api/parking-spaces/1/area
    @GetMapping("/{id}/area")
    public ResponseEntity<String> getEditedParkingSpaceArea(@PathVariable("id") int id) {
        Optional<String> area = editedParkingSpaceService.getAreaOfEditedParkingSpaceById(id);
        return area
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // http://localhost:8080/api/parking-spaces/search?occupied=true
    @GetMapping("/search")
    public ResponseEntity<List<String>> findEditedParkingSpacesByOccupancy(@RequestParam("occupied") boolean occupied) {
        List<String> editedParkingSpaces = editedParkingSpaceService.getEditedParkingSpacesByOccupancyAsJson(occupied);
        if (editedParkingSpaces.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(editedParkingSpaces);
    }

    // http://localhost:8080/api/parking-spaces/1/occupancy?occupied=true
    @PatchMapping("/{id}/occupancy")
    public ResponseEntity<?> updateOccupancyStatus(@PathVariable int id, @RequestParam boolean occupied) {
        boolean updateSuccessful = editedParkingSpaceService.updateOccupancyStatus(id, occupied);

        if (updateSuccessful) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }
}
