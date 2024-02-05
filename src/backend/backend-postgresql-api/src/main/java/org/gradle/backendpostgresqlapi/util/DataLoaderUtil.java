package org.gradle.backendpostgresqlapi.util;

import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.gradle.backendpostgresqlapi.service.EditedParkingSpaceService;
import org.gradle.backendpostgresqlapi.service.ParkingPointService;
import org.gradle.backendpostgresqlapi.service.ParkingSpaceService;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
public class DataLoaderUtil {

	public static void loadDataIntoDatabase(List<String> filePaths, ParkingSpaceService parkingSpaceService,
		EditedParkingSpaceService editedParkingSpaceService, ParkingPointService parkingPointService) throws IOException, CsvValidationException {
		if (!CollectionUtils.isEmpty(filePaths)) {
			loadFromParkingSpacesFiles(filePaths, parkingSpaceService, editedParkingSpaceService);
			loadFromTimestampFiles(filePaths, parkingPointService);
		} else {
			log.warn("No data files configured for loading.");
		}
	}

	private static void loadFromParkingSpacesFiles(List<String> filePaths, ParkingSpaceService parkingSpaceService,
		EditedParkingSpaceService editedParkingSpaceService) throws IOException, CsvValidationException {
		for (String filePath : filePaths.stream().filter(fileName -> !fileName.contains("timestamp")).toList()) {
			String extension = FilenameUtils.getExtension(filePath).toLowerCase();

			switch (extension) {
				case "geojson" -> parkingSpaceService.loadGeoJson(filePath);
				case "csv" -> parkingSpaceService.loadCsv(filePath);
				default -> log.warn("Unsupported file format for file: {}", filePath);
			}
		}

		// Copy data to edited_parking_spaces database
		editedParkingSpaceService.copyDataIntoDatabase();
	}

	private static void loadFromTimestampFiles(List<String> filePaths, ParkingPointService parkingPointService) throws IOException {
		for (String filePath : filePaths.stream().filter(fileName -> fileName.contains("timestamp")).toList()) {
			if (!FilenameUtils.getExtension(filePath).equalsIgnoreCase("geojson")) {
				log.warn("Unsupported file format for file: {}", filePath);
			} else {
				parkingPointService.loadGeoJson(filePath);
			}
		}
	}
}
