package org.gradle.backendpostgresqlapi.enums;

import lombok.Getter;

@Getter
public enum ParkingPosition {
    SCHRAEG("Diagonal"),
    LAENGS("Parallel"),
    QUER("Transverse"); // Transverse or across parking

    private final String displayName;

    ParkingPosition(String displayName){
        this.displayName = displayName;
    }

    // get the enum instance from the string value
    public static ParkingPosition fromString(String position) {
        return switch (position.toUpperCase()) {
            case "SCHRÄG" -> SCHRAEG;
            case "LÄNGS", "LÄNGE" -> LAENGS;
            case "QUER" -> QUER;
            default -> throw new IllegalArgumentException("No enum constant for string value: " + position);
        };
    }
}
