package org.gradle.backendpostgresqlapi.dto;

import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;

@Getter
@Setter
public class TimestampDto {
	private long id;
	private long parkingPointId;
	private String timestamp;
}
