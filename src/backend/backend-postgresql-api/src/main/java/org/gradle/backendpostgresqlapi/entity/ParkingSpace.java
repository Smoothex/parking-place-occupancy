package org.gradle.backendpostgresqlapi.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import static org.gradle.backendpostgresqlapi.util.JsonHandler.*;

@Getter
@Setter
@Entity
@Table(name = "parking_spaces", schema = "public")
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ps_id")
    private int id;

    @Column(name = "ps_coordinates", nullable = false)
    private String geoJson;

    @Column(name = "ps_occupied")
    private boolean occupied;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    protected ParkingSpace() {

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