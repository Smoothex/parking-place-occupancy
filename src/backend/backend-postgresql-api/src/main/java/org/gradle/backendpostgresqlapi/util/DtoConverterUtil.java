package org.gradle.backendpostgresqlapi.util;

import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.dto.ParkingPointDto;
import org.gradle.backendpostgresqlapi.dto.TimestampDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;

public class DtoConverterUtil {

	public static EditedParkingSpaceDto convertToDto(EditedParkingSpace editedParkingSpace) {
		Integer capacity = editedParkingSpace.getCapacity();
        int convertedCapacity = (capacity != null) ? capacity.intValue() : -1;
		
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

	public static ParkingPointDto convertToDto(ParkingPoint parkingPoint) {
		return ParkingPointDto.builder()
			.id(parkingPoint.getId())
			.editedParkingSpaceId(parkingPoint.getEditedParkingSpace().getId())
			.coordinates(parkingPoint.getPoint().getCoordinates())
			.build();
	}

	public static TimestampDto convertToDto(Timestamp timestamp) {
		return TimestampDto.builder()
			.id(timestamp.getId())
			.parkingPointId(timestamp.getParkingPoint().getId())
			.timestamp(timestamp.getTimestamp())
			.build();
	}
}
