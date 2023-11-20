package org.gradle.backendpostgresqlapi.controller;

import org.gradle.backendpostgresqlapi.service.GeospatialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/parking-spaces")
@CrossOrigin(origins = "http://localhost:4200")
public class ParkingSpaceController {

    private final GeospatialService geospatialService;

    @Autowired
    public ParkingSpaceController(GeospatialService geospatialService) {
        this.geospatialService = geospatialService;
    }

    // http://localhost:8080/api/parking-spaces
    @GetMapping
    public ResponseEntity<List<String>> getAllParkingSpaces() {
        List<String> parkingSpaces = geospatialService.getAllParkingSpacesAsJson();
        return ResponseEntity.ok(parkingSpaces);
    }

    // http://localhost:8080/api/parking-spaces/1
    @GetMapping("/{id}")
    public ResponseEntity<String> getParkingSpaceById(@PathVariable("id") int id) {
        Optional<String> parkingSpace = geospatialService.getParkingSpaceByIdAsJson(id);
        return parkingSpace
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // http://localhost:8080/api/parking-spaces/1/area
    @GetMapping("/{id}/area")
    public ResponseEntity<String> getParkingSpaceArea(@PathVariable("id") int id) {
        Optional<String> area = geospatialService.getAreaOfParkingSpaceById(id);
        return area
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // http://localhost:8080/api/parking-spaces/search?occupied=true
    @GetMapping("/search")
    public ResponseEntity<List<String>> findParkingSpacesByOccupancy(@RequestParam("occupied") boolean occupied) {
        List<String> parkingSpaces = geospatialService.findParkingSpacesByOccupancy(occupied);
        if (parkingSpaces.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(parkingSpaces);
    }

    // http://localhost:8080/api/parking-spaces/1/occupancy?occupied=true
    @PatchMapping("/{id}/occupancy")
    public ResponseEntity<?> updateOccupancyStatus(@PathVariable int id, @RequestParam boolean occupied) {
        boolean updateSuccessful = geospatialService.updateOccupancyStatus(id, occupied);

        if (updateSuccessful) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }
}
