package org.gradle.backendpostgresqlapi.dto;

import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;

@Getter
@Setter
public class ParkingPointDto {
	private long id;
	private long editedParkingSpaceId;
	private Coordinate[] coordinates;
}
