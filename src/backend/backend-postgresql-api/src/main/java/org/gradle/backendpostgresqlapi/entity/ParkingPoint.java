package org.gradle.backendpostgresqlapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "parking_points", schema = "public")
public class ParkingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pp_id", nullable = false)
    private Long id;

    @Column(name = "pp_edit_id", nullable = false, updatable = false)
    private Long editedParkingSpaceId;

    @Column(columnDefinition = "GEOGRAPHY(POINT, 4326)", name = "pp_coordinates", nullable = false, updatable = false)
    private org.locationtech.jts.geom.Point point;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public ParkingPoint() {}

    @Override
    public String toString() {
        return "TimestampPoint{" +
                    "id=" + id +
                    ", editedParkingSpaceId=" + editedParkingSpaceId +
                    ", coordinates=" + coordinatesToString(point) + "}";
    }

    private String coordinatesToString(org.locationtech.jts.geom.Point point) {
        if (point == null) return "null";

        return String.format(Locale.US,"(%.4f, %.4f)", point.getCoordinate().x, point.getCoordinate().y);
    }
}
