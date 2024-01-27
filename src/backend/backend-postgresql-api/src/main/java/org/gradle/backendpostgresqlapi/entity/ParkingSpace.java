package org.gradle.backendpostgresqlapi.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.gradle.backendpostgresqlapi.enums.ParkingPosition;
import org.locationtech.jts.geom.Polygon;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "parking_spaces", schema = "public")
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ps_id", nullable = false)
    private Long id;

    @Column(columnDefinition = "GEOGRAPHY(POLYGON, 4326)", name = "ps_coordinates", nullable = false, updatable = false)
    private Polygon polygon;

    @Column(name = "ps_area")
    private double area;

    @Column(name = "ps_capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "ps_position")
    private ParkingPosition position;

    @OneToMany(mappedBy = "assignedParkingSpace")
    private Set<OverlappingParkingSpace> overlappingParkingSpaces;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public ParkingSpace() {}

    @Override
    public String toString() {
        return "ParkingSpace{" +
                    "id=" + id +
                    ", coordinates=" + coordinatesToString(polygon) +
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
