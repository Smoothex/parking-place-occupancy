package org.gradle.backendpostgresqlapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@SpringBootApplication
public class DatabaseConnection {

	public static void main(String[] args) {
        String dbUser = "myuser";
        String dbPassword = "mypassword";
        String url = "jdbc:postgresql://postgres:5432/mydb";
        try {
            Thread.sleep(10000); // sleep for 10s so that PostgreSQL can initialize properly
            Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
            if (connection != null) {
                System.out.println("Connected to the database!");
                // Perform database operations here

                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
