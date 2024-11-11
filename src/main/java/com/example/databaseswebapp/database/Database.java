package com.example.databaseswebapp.database;

import com.example.databaseswebapp.Ingredient;

import java.io.FileInputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
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

    public static boolean insertIngredient(String email, String ingredient) {
        try (Connection connection = Database.newConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM ingredients WHERE name = ?")) {
                ps.setString(1, ingredient);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { // If the ingredient exists, add it to the user's account
                        return addIngredientToAccount(email, ingredient);
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

    private static boolean addIngredientToAccount(String email, String ingredient) {
        try (Connection connection = Database.newConnection()) {
            // Get the user's id, and the ingredients id
            try (PreparedStatement ps = connection.prepareStatement("" +
                    "INSERT INTO hasIngredient (ingredientId, userId, quantity) VALUES (" +
                    "(SELECT id FROM ingredients WHERE name = ?), (SELECT id FROM users WHERE email = ?), 1)"
            )) {
                ps.setString(1, ingredient);
                ps.setString(2, email);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return false;
        }
    }

    public static Ingredient[] getIngredients(String email) {

        try (Connection connection = Database.newConnection()) {
            String sql =
              " SELECT ingredients.name"
            + " FROM ingredients "
            + " JOIN hasIngredient ON ingredients.id = hasIngredient.ingredientId"
            + " JOIN users ON hasIngredient.userId = users.id"
            + " WHERE users.email = ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {
                    ArrayList<Ingredient> ingredients = new ArrayList<>();
                    while (rs.next()) {
                        Ingredient newIngredient = new Ingredient(rs.getString(1), 1);
                        ingredients.add(newIngredient);
                    }
                    return ingredients.toArray(new Ingredient[0]);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return null;
        }
    }

    public static boolean deleteIngredient(String email, String ingredient) {
        try (Connection connection = Database.newConnection()) {
            // Get the user's id, and the ingredients id
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM hasIngredient WHERE ingredientId = " +
                    "(SELECT id FROM ingredients WHERE name = ?) AND userId = (SELECT id FROM users WHERE email = ?)"
            )) {
                ps.setString(1, ingredient);
                ps.setString(2, email);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return false;
        }
    }

    public static boolean changePassword(String email, String currentPassword, String newPassword) {
        try (Connection connection = Database.newConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET password = ? WHERE email = ? AND password = ?")) {
                ps.setString(1, newPassword);
                ps.setString(2, email);
                ps.setString(3, currentPassword);

                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println(e.getMessage());
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
