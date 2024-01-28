package org.gradle.backendpostgresqlapi.dto;

import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Coordinate;

@Data
@Builder
public class ParkingPointDto {
	private long id;
	private long editedParkingSpaceId;
	private Coordinate[] coordinates;
}
