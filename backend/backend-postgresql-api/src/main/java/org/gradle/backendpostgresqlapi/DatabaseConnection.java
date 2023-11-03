package org.gradle.backendpostgresqlapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@SpringBootApplication
public class DatabaseConnection {

	public static void main(String[] args) {
        boolean isExecutedInDocker = true; // change when using it locally for testing
        String dbUser = "parkuser";
        String dbName = "parking_spots_db";
        String hostName = isExecutedInDocker ? "postgres" : "localhost";
        String containerPort = isExecutedInDocker ? "5432" : "32768";
        String url = "jdbc:postgresql://" + hostName + ":" + containerPort + "/" + dbName;
        Properties connectionProp = new Properties();
        connectionProp.setProperty("user", dbUser);
        try {
            if (isExecutedInDocker) {
                Thread.sleep(10000); // sleep for 10s so that PostgreSQL can initialize properly
            }
            Connection connection = DriverManager.getConnection(url, connectionProp);
            if (connection != null) {
                System.out.println("Connected to the database!");
                // Perform database operations here

                connection.close();
            }
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
