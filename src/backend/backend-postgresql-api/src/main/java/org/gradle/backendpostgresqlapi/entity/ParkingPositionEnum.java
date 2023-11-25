package org.gradle.backendpostgresqlapi.entity;

import lombok.Getter;

@Getter
public enum ParkingPositionEnum {
    SCHRAEG("Diagonal"),
    LAENGS("Parallel"),
    QUER("Transverse"); // Transverse or across parking

    private final String displayName;

    ParkingPositionEnum (String displayName){
        this.displayName = displayName;
    }

    // get the enum instance from the string value
    public static ParkingPositionEnum fromString(String position) {
        return switch (position.toUpperCase()) {
            case "SCHRÄG" -> SCHRAEG;
            case "LÄNGS", "LÄNGE" -> LAENGS;
            case "QUER" -> QUER;
            default -> throw new IllegalArgumentException("No enum constant for string value: " + position);
        };
    }
}
