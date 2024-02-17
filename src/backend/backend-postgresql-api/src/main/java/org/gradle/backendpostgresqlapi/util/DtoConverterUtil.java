package org.gradle.backendpostgresqlapi.util;

import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;

public class DtoConverterUtil {

	public static EditedParkingSpaceDto convertToDto(EditedParkingSpace editedParkingSpace) {

		return EditedParkingSpaceDto.builder()
			.id(editedParkingSpace.getId())
			.parkingSpaceId(editedParkingSpace.getParkingSpaceId())
			.coordinates(editedParkingSpace.getPolygon().getCoordinates())
			.occupied(editedParkingSpace.isOccupied())
			.area(editedParkingSpace.getArea())
			.capacity(editedParkingSpace.getCapacity())
			.position(editedParkingSpace.getPosition().getDisplayName())
			.build();
	}
}
