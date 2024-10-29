package com.example.databaseswebapp;

import com.example.databaseswebapp.database.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatabasesWebAppApplication {
    private static final String CONFIG_FILE_PATH = "config.properties";

    public static void main(String[] args) {
        // Initialize the database, then run the app
        try {
            Database.initialize(CONFIG_FILE_PATH);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize database", e);
        }

        SpringApplication.run(DatabasesWebAppApplication.class, args);
    }

}
