package org.gradle.backendpostgresqlapi.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import lombok.extern.slf4j.Slf4j;

import org.gradle.backendpostgresqlapi.entity.ParkingSpace;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.Reader;
import java.io.InputStreamReader;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CsvHandler {

    private final static WKTReader wktReader = new WKTReader();

    public static List<ParkingSpace> getCsvDataFromFile(ResourceLoader resourceLoader, String filePath) throws IOException, CsvValidationException {
        List<ParkingSpace> parkingSpaces = new ArrayList<>();
        log.debug("Reading CSV data from file: {}", filePath);

        Resource resource = resourceLoader.getResource(filePath);
        if (!resource.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        try (Reader reader = new InputStreamReader(resource.getInputStream());
            CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
                ParkingSpace parkingSpace = new ParkingSpace();
                Geometry geometry = parseWktToGeometry(nextRecord[0]);

                if (geometry instanceof Polygon) {
                    parkingSpace.setPolygon((Polygon) geometry);
                } else if (geometry instanceof MultiPolygon) {
                    // Handle the case where the geometry is a MultiPolygon.
                    // You might want to convert it to a Polygon, take the first Polygon, or handle it according to your application logic.
                    MultiPolygon multiPolygon = (MultiPolygon) geometry;
                    // For example, just take the first polygon (if that makes sense for your application)
                    if (multiPolygon.getNumGeometries() > 0) {
                        parkingSpace.setPolygon((Polygon) multiPolygon.getGeometryN(0));

                    }
                } else {
                    log.error("Geometry is not a Polygon or MultiPolygon: {}", geometry.toText());
                }

                parkingSpace.setOccupied(false); // Assuming default is always false since it's not in CSV
                parkingSpace.setNumberOfParkingSpaces(Integer.parseInt(nextRecord[2]));
                parkingSpace.setPosition(nextRecord[4]);
                parkingSpaces.add(parkingSpace);
                log.debug("Added parking space with ID: {}", nextRecord[1]); // Assuming nextRecord[1] contains the ID
            }
        } catch (IOException e) {
            log.error("Error reading CSV file at path: {}", filePath, e);
            throw e;
        }
        log.info("Successfully loaded {} parking spaces from CSV file.", parkingSpaces.size());
        return parkingSpaces;
    }

    private static Geometry parseWktToGeometry(String wktString) {
        try {
            return wktReader.read(wktString);
        } catch (ParseException e) {
            log.error("Error parsing WKT to Geometry. WKT string: {}", wktString, e);
            throw new IllegalArgumentException("Error parsing WKT to Geometry", e);
        }
    }

}
