package org.gradle.backendpostgresqlapi.dto;

public class ParkingSpaceDTO {

    private int id;
    private String geoJson;
    // private boolean isOccupied; <----- for further use

    // default constructor only for the sake of JPA (See https://spring.io/projects/spring-data-jpa)
    protected ParkingSpaceDTO() {}

    public ParkingSpaceDTO(int id, String geoJson) {
        this.id = id;
        this.geoJson = geoJson;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGeoJson() {
        return this.geoJson;
    }

    public void setGeoJson(String geoJson) {
        this.geoJson = geoJson;
    }
    
    @Override
    public String toString() {
        return "ParkingSpaceDTO{id=" + id + ", geoJson=(" + geoJson + ")}";
    }
}