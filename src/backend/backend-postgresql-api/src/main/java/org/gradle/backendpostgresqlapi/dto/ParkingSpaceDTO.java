package org.gradle.backendpostgresqlapi.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.getCoordinatesFromGeoJson;

@Getter
@Setter
public class ParkingSpaceDTO {

    private int id;
    private String geoJson;
    private boolean occupied;

    public ParkingSpaceDTO(int id, String geoJson) {
        this.id = id;
        this.geoJson = geoJson;
    }

    @Override
    public String toString() {
        try {
            return "ParkingSpace{id=" + id +
                    ", coordinates=(" + getCoordinatesFromGeoJson(geoJson) + ")}" +
                    ", isOccupied=" + occupied + "}";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}