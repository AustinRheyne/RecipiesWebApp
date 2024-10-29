package com.example.databaseswebapp.database;

import java.io.FileInputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.Properties;

public class Database {
    private static final Properties databaseProperties = new Properties();
    private static String connectionString;
    private static boolean isInitialized = false;

    private Database() {} // no constructor, should be referenced statically

    public static void initialize(String propertiesFilePath) throws Exception {
        // Initialize MySQL driver (from mysql-connector-j dependency in build.gradle)
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Build connection string
        databaseProperties.load(new FileInputStream(propertiesFilePath));
        String serverName = databaseProperties.getProperty("database.server");
        String serverPort = databaseProperties.getProperty("database.port");
        String schemaName = databaseProperties.getProperty("database.schema");
        String username = databaseProperties.getProperty("database.username");
        String password = databaseProperties.getProperty("database.password");
        connectionString = "jdbc:mysql://" + serverName + ":" + serverPort + "/" + schemaName
                + "?user=" + username + "&password=" + password;

        isInitialized = true;
    }

    public static Connection newConnection() throws SQLException {
        if (!isInitialized) {
            throw new RuntimeException("Database was not successfully initialized.");
        }

        return DriverManager.getConnection(connectionString);
    }

    public static String getVerison() {
        try (Connection connection = Database.newConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT VERSION() AS version")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { // only expect 1 row, so using if instead of while
                        return rs.getString("version");
                    }
                    else {
                        return "No version returned???";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            return "ERROR: " + e.getMessage();
        }
    }

    public static boolean createNewUser(String username, String password) {
        try (Connection connection = Database.newConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO users (email, password, joinDate) VALUES (?, ?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, LocalDate.now().toString());

                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean login(String username, String password) {
        try (Connection connection = Database.newConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE email = ? AND password = ?")) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { // only expect 1 row, so using if instead of while
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return false;
        }
    }

    public static String getJoinDate(String email) {
        try (Connection connection = Database.newConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { // only expect 1 row, so using if instead of while
                        return rs.getDate("joinDate").toString();
                    }
                    else {
                        return "";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return "";
        }
    }
}
