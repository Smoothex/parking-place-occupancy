package org.gradle.backendpostgresqlapi.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "parking_spaces", schema = "public")
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ps_id")
    private int id;

    @Column(name = "ps_coordinates", nullable = false)
    private Polygon polygon;

    @Column(name = "ps_occupied")
    private boolean occupied;

    @Column(name = "ps_area")
    private double area;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public ParkingSpace() {}

    @Override
    public String toString() {
        return "ParkingSpace{" +
                    "id=" + id +
                    ", coordinates=" + coordinatesToString(polygon) +
                    ", isOccupied=" + occupied +
                    ", area=" + area + "}";
    }

    private String coordinatesToString(Polygon polygon) {
        if (polygon == null) return "null";
    
        return Stream.of(polygon.getCoordinates())
                     .map(coordinate -> String.format("(%f, %f)", coordinate.x, coordinate.y))
                     .collect(Collectors.joining(", "));
    }
}
