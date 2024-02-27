package org.gradle.backendpostgresqlapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.gradle.backendpostgresqlapi.enums.ParkingPosition;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "overlapping_parking_spaces", schema = "public")
public class OverlappingParkingSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ops_id", nullable = false)
    private Long id;

    @Column(columnDefinition = "GEOGRAPHY(POLYGON, 4326)", name = "ops_coordinates", nullable = false, updatable = false)
    private Polygon polygon;

    @Column(name = "ops_capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "ops_position")
    private ParkingPosition position;

    @Column(columnDefinition = "GEOGRAPHY(POINT, 4326)", name = "ops_centroid", nullable = false, updatable = false)
    private Point centroid;

    @ManyToOne
    @JoinColumn(name = "ops_ps_id", nullable = false)
    private ParkingSpace assignedParkingSpace;

    public OverlappingParkingSpace() {}

    public OverlappingParkingSpace(Polygon polygon, Integer capacity, ParkingPosition position, Point centroid,
        ParkingSpace assignedParkingSpace) {
        setPolygon(polygon);
        setCapacity(capacity);
        setPosition(position);
        setCentroid(centroid);
        setAssignedParkingSpace(assignedParkingSpace);
    }

    @Override
    public String toString() {
        return "OverlappingParkingSpace{" +
                    "id=" + id +
                    ", coordinates=" + coordinatesToString(polygon) +
                    ", capacity=" + capacity +
                    ", position=" + (position != null ? position.getDisplayName() : "null") +
                    ", parkingSpaceId=" + assignedParkingSpace.getId() + "}";
    }

    private String coordinatesToString(Polygon polygon) {
        if (polygon == null) return "null";

        return Stream.of(polygon.getCoordinates())
                     .map(coordinate -> String.format(Locale.US,"(%.4f, %.4f)", coordinate.x, coordinate.y))
                     .collect(Collectors.joining(", "));
    }
}
