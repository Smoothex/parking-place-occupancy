package org.gradle.backendpostgresqlapi.dto;

import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Coordinate;


@Data
@Builder
public class EditedParkingSpaceDto {
	private long id;
	private long parkingSpaceId;
	private Coordinate[] coordinates;
	private boolean occupied;
	private double area;
	private int capacity;
	private String position;
}
