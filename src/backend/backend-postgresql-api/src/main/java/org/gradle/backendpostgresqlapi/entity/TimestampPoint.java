package org.gradle.backendpostgresqlapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "timestamp_points", schema = "public")
public class TimestampPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tp_id", nullable = false)
    private Long id;

    @Column(name = "tp_edit_id", nullable = false, updatable = false)
    private Long editedParkingSpaceId;

    @Column(columnDefinition = "GEOGRAPHY(POINT, 4326)", name = "tp_coordinates", nullable = false, updatable = false)
    private Point point;

    @Column(name = "tp_timestamp", nullable = false, updatable = false)
    private String timestamp;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public TimestampPoint() {}

    @Override
    public String toString() {
        return "TimestampPoint{" +
                    "id=" + id +
                    ", editedParkingSpaceId=" + editedParkingSpaceId +
                    ", coordinates=" + coordinatesToString(point) +
                    ", timestamp=" + timestamp + "}";
    }

    private String coordinatesToString(Point point) {
        if (point == null) return "null";

        return String.format(Locale.US,"(%.4f, %.4f)", point.getCoordinate().x, point.getCoordinate().y);
    }
}
