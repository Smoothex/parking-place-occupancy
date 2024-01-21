package org.gradle.backendpostgresqlapi.dto;

import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;

@Getter
@Setter
public class EditedParkingSpaceDto {
	private long id;
	private long parkingSpaceId;
	private Coordinate[] coordinates;
	private boolean occupied;
	private double area;
	private int capacity;
	private String position;
}
