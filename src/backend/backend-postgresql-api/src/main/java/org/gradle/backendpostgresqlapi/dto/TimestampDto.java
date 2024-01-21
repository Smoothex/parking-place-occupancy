package org.gradle.backendpostgresqlapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimestampDto {
	private long id;
	private long parkingPointId;
	private String timestamp;
}
