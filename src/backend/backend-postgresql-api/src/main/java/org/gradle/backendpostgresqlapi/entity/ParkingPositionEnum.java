package org.gradle.backendpostgresqlapi.entity;

public enum ParkingPositionEnum {
    SCHRÄG {
        public String toString() {
            return "Diagonal";
        }
    },
    LÄNGS {
        public String toString() {
            return "Parallel";
        }
    },
    QUER {
        public String toString() {
            return "Transverse"; // Transverse or across parking
        }
    };

    // get the enum instance from the string value
    public static ParkingPositionEnum fromString(String position) {
        switch (position.toUpperCase()) {
            case "SCHRÄG":
                return SCHRÄG;
            case "LÄNGS":
            case "LÄNGE":
                return LÄNGS;
            case "QUER":
                return QUER;
            default:
                throw new IllegalArgumentException("No enum constant for string value: " + position);
        }
    }
}
