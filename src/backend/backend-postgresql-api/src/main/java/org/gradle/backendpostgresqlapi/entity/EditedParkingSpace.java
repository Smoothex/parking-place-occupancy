package org.gradle.backendpostgresqlapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.gradle.backendpostgresqlapi.enums.ParkingPosition;
import org.locationtech.jts.geom.Polygon;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "edited_parking_spaces", schema = "public")
public class EditedParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edit_id", nullable = false)
    private Long id;

    @Column(name = "edit_ps_id", nullable = false, updatable = false)
    private Long parkingSpaceId;

    @Column(columnDefinition = "GEOGRAPHY(POLYGON, 4326)", name = "edit_coordinates", nullable = false)
    private Polygon polygon;

    @Column(columnDefinition = "boolean default false", name = "edit_occupied")
    private boolean occupied;

    @Column(name = "edit_area")
    private double area;

    @Column(name = "edit_capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_position")
    private ParkingPosition position;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public EditedParkingSpace() {}

    @Override
    public String toString() {
        return "EditedParkingSpace{" +
                    "id=" + id +
                    ", parkingSpaceId=" + parkingSpaceId +
                    ", coordinates=" + coordinatesToString(polygon) +
                    ", isOccupied=" + occupied +
                    ", area=" + area +
                    ", capacity=" + capacity +
                    ", position=" + (position != null ? position.getDisplayName() : "null") + "}";
    }

    private String coordinatesToString(Polygon polygon) {
        if (polygon == null) return "null";

        return Stream.of(polygon.getCoordinates())
                     .map(coordinate -> String.format(Locale.US,"(%.4f, %.4f)", coordinate.x, coordinate.y))
                     .collect(Collectors.joining(", "));
    }
}
