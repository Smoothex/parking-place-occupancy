package org.gradle.backendpostgresqlapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "timestamps", schema = "public")
public class Timestamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "t_id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parking_point_id", nullable = false)
    private ParkingPoint parkingPoint;

    @Column(name = "t_timestamp", nullable = false, updatable = false)
    private String timestamp;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public Timestamp() {}

    @Override
    public String toString() {
        return "TimestampPoint{" +
                    "id=" + id +
                    ", parkingPointId=" + parkingPoint.getId() +
                    ", timestamp=" + timestamp + "}";
    }
}
