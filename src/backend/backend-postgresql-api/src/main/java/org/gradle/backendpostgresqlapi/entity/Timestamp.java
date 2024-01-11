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

    @Column(name = "t_pp_id", nullable = false, updatable = false)
    private Long parkingPointId;

    @Column(name = "t_timestamp", nullable = false, updatable = false)
    private String timestamp;

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    public Timestamp() {}

    @Override
    public String toString() {
        return "TimestampPoint{" +
                    "id=" + id +
                    ", parkingPointId=" + parkingPointId +
                    ", timestamp=" + timestamp + "}";
    }
}
