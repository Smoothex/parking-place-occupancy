package org.gradle.backendpostgresqlapi.util;

import org.gradle.backendpostgresqlapi.dto.EditedParkingSpaceDto;
import org.gradle.backendpostgresqlapi.dto.ParkingPointDto;
import org.gradle.backendpostgresqlapi.dto.TimestampDto;
import org.gradle.backendpostgresqlapi.entity.EditedParkingSpace;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;

public class DtoConverterUtil {

	public static EditedParkingSpaceDto convertToDto(EditedParkingSpace editedParkingSpace) {
		EditedParkingSpaceDto editedParkingSpaceDto = new EditedParkingSpaceDto();
		editedParkingSpaceDto.setId(editedParkingSpace.getId());
		editedParkingSpaceDto.setParkingSpaceId(editedParkingSpace.getParkingSpaceId());
		editedParkingSpaceDto.setCoordinates(editedParkingSpace.getPolygon().getCoordinates());
		editedParkingSpaceDto.setOccupied(editedParkingSpace.isOccupied());
		editedParkingSpaceDto.setArea(editedParkingSpace.getArea());

		if (editedParkingSpace.getCapacity() != null) {
			editedParkingSpaceDto.setCapacity(editedParkingSpace.getCapacity());
		}
		if (editedParkingSpace.getPosition() != null) {
			editedParkingSpaceDto.setPosition(editedParkingSpace.getPosition().getDisplayName());
		}

		return editedParkingSpaceDto;
	}

	public static ParkingPointDto convertToDto(ParkingPoint parkingPoint) {
		ParkingPointDto parkingPointDto = new ParkingPointDto();
		parkingPointDto.setId(parkingPoint.getId());
		parkingPointDto.setEditedParkingSpaceId(parkingPoint.getEditedParkingSpaceId());
		parkingPointDto.setCoordinates(parkingPoint.getPoint().getCoordinates());

		return parkingPointDto;
	}

	public static TimestampDto convertToDto(Timestamp timestamp) {
		TimestampDto timestampDto = new TimestampDto();
		timestampDto.setId(timestamp.getId());
		timestampDto.setParkingPointId(timestamp.getParkingPointId());
		timestampDto.setTimestamp(timestamp.getTimestamp());

		return timestampDto;
	}
}
