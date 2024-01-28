package org.gradle.backendpostgresqlapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimestampDto {
	private long id;
	private long parkingPointId;
	private String timestamp;
}
