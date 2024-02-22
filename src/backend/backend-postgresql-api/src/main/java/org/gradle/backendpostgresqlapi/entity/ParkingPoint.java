package org.gradle.backendpostgresqlapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.Set;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@Entity
@Table(name = "parking_points", schema = "public")
public class ParkingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pp_id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pp_eps_id")
    private EditedParkingSpace editedParkingSpace;

    @Column(columnDefinition = "GEOGRAPHY(POINT, 4326)", name = "pp_coordinates", nullable = false, updatable = false)
    private Point point;

    @OneToMany(mappedBy = "parkingPoint", fetch = FetchType.EAGER)
    private Set<Timestamp> timestamps;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public ParkingPoint() {}

    @Override
    public String toString() {
        return "TimestampPoint{" +
                    "id=" + id +
                    ", editedParkingSpaceId=" + editedParkingSpace.getId() +
                    ", coordinates=" + coordinatesToString(point) + "}";
    }

    private String coordinatesToString(Point point) {
        if (point == null) return "null";

        return String.format(Locale.US,"(%.4f, %.4f)", point.getCoordinate().x, point.getCoordinate().y);
    }
}
