package org.gradle.backendpostgresqlapi.util;

import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;

public class DtoConverterUtil {

	public static EditedParkingSpaceDto convertToDto(EditedParkingSpace editedParkingSpace) {
		Integer capacity = editedParkingSpace.getCapacity();
        int convertedCapacity = (capacity != null) ? capacity : -1;
		
		EditedParkingSpaceDto editedParkingSpaceDto = EditedParkingSpaceDto.builder()
			.id(editedParkingSpace.getId())
			.parkingSpaceId(editedParkingSpace.getParkingSpaceId())
			.coordinates(editedParkingSpace.getPolygon().getCoordinates())
			.occupied(editedParkingSpace.isOccupied())
			.area(editedParkingSpace.getArea())
			.capacity(convertedCapacity)
			.build();

		if (editedParkingSpace.getPosition() != null) {
			editedParkingSpaceDto.setPosition(editedParkingSpace.getPosition().getDisplayName());
		}

		return editedParkingSpaceDto;
	}
}
