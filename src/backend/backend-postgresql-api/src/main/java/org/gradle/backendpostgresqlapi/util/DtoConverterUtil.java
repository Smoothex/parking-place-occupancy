package org.gradle.backendpostgresqlapi.util;

import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.dto.ParkingPointDto;
import org.gradle.backendpostgresqlapi.dto.TimestampDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;

public class DtoConverterUtil {

	public static EditedParkingSpaceDto convertToDto(EditedParkingSpace editedParkingSpace) {
		EditedParkingSpaceDto editedParkingSpaceDto = EditedParkingSpaceDto.builder()
			.id(editedParkingSpace.getId())
			.parkingSpaceId(editedParkingSpace.getParkingSpaceId())
			.coordinates(editedParkingSpace.getPolygon().getCoordinates())
			.occupied(editedParkingSpace.isOccupied())
			.area(editedParkingSpace.getArea())
			.capacity(editedParkingSpace.getCapacity())
			.build();

		if (editedParkingSpace.getPosition() != null) {
			editedParkingSpaceDto.setPosition(editedParkingSpace.getPosition().getDisplayName());
		}

		return editedParkingSpaceDto;
	}

	public static ParkingPointDto convertToDto(ParkingPoint parkingPoint) {
		ParkingPointDto parkingPointDto = ParkingPointDto.builder()
			.id(parkingPoint.getId())
			.editedParkingSpaceId(parkingPoint.getEditedParkingSpace().getId())
			.coordinates(parkingPoint.getPoint().getCoordinates())
			.build();

		return parkingPointDto;
	}

	public static TimestampDto convertToDto(Timestamp timestamp) {
		TimestampDto timestampDto = TimestampDto.builder()
			.id(timestamp.getId())
			.parkingPointId(timestamp.getParkingPoint().getId())
			.timestamp(timestamp.getTimestamp())
			.build();

		return timestampDto;
	}
}
