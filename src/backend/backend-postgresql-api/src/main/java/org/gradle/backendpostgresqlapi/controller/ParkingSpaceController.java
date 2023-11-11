package org.gradle.backendpostgresqlapi.controller;

import org.gradle.backendpostgresqlapi.service.GeospatialService;
import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/parking-spaces")
public class ParkingSpaceController {

    private final GeospatialService geospatialService;

    @Autowired
    public ParkingSpaceController(GeospatialService geospatialService) {
        this.geospatialService = geospatialService;
    }

    // http://localhost:8080/api/parking-spaces
    @GetMapping
    public ResponseEntity<List<ParkingSpace>> getAllParkingSpaces() {
        List<ParkingSpace> parkingSpaces = geospatialService.getParkingSpaces();
        return ResponseEntity.ok(parkingSpaces);
    }

    // http://localhost:8080/api/parking-spaces/1
    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpace> getParkingSpaceById(@PathVariable("id") int id) {
        Optional<ParkingSpace> parkingSpace = geospatialService.getParkingSpaceById(id);
        return parkingSpace
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // http://localhost:8080/api/parking-spaces/search?occupied=true
    @GetMapping("/search")
    public ResponseEntity<List<ParkingSpace>> findParkingSpacesByOccupancy(@RequestParam("occupied") boolean occupied) {
        List<ParkingSpace> parkingSpaces = geospatialService.findParkingSpacesByOccupancy(occupied);
        if (parkingSpaces.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(parkingSpaces);
    }
}
