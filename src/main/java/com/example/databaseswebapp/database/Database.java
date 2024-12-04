package com.example.databaseswebapp.database;

import com.example.databaseswebapp.Ingredient;
import com.example.databaseswebapp.Recipe;

import javax.xml.crypto.Data;
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

    public static Ingredient[] getUserIngredients(String email) {

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
                        Ingredient newIngredient = new Ingredient(rs.getString(1), 1, true);
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

    public static Ingredient[] getRecipeIngredients(String id, String email) {
        // Get all ingredients, check which ones the user has
        try (Connection connection = Database.newConnection()) {
            String sql =
                    " SELECT ingredients.name, ingredients.id"
                            + " FROM ingredients "
                            + " JOIN usesingredients ON ingredients.id = usesIngredients.ingredientId"
                            + " JOIN recipes ON usesIngredients.recipeId = recipes.id"
                            + " WHERE recipes.id = ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    ArrayList<Ingredient> ingredients = new ArrayList<>();
                    while (rs.next()) {
                        String name = rs.getString(1);
                        boolean hasIngredient = false;

                        // Check if the user has this ingredient
                        if(email != null) {
                            String ingredientSQL = "SELECT ingredientId FROM hasIngredient WHERE ingredientId = ?";
                            try (PreparedStatement is = connection.prepareStatement(ingredientSQL)) {
                                is.setString(1, rs.getString(2));
                                try (ResultSet iRs = is.executeQuery()) {
                                    if (iRs.next()) {
                                        hasIngredient = true;
                                    }
                                }
                            }
                        }

                        Ingredient newIngredient = new Ingredient(name, 1, hasIngredient);
                        System.out.println(name);
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

    public static boolean createRecipe(String name, String directions, String[] ingredients, String imagePath) {
        try (Connection connection = Database.newConnection()) {
            // Insert the recipe
            int recipeId = 0;
            try (PreparedStatement ps = connection.prepareStatement("" +
                    "INSERT INTO recipes (name, recipe) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ps.setString(1, name);
                ps.setString(2, directions);
                ps.executeUpdate();

                try(ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        recipeId = generatedKeys.getInt(1);
                    }
                }
            }

            // Link ingredients
            for (String ingredient: ingredients) {
                // Get ingredient id or create it if it does not exist
                int ingredientId = 0;
                String sql = "SELECT id FROM ingredients WHERE name = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql))
                {
                    ps.setString(1, ingredient);
                    try (ResultSet rs = ps.executeQuery()) {
                        // The ingredient exists!
                        if (rs.next()) {
                            ingredientId = rs.getInt(1);
                        } else {
                        // The ingredient doesn't exist, we need to create it
                            try (PreparedStatement createIng = connection.prepareStatement("INSERT INTO ingredients (name) VALUES (?)",
                                    Statement.RETURN_GENERATED_KEYS)) {
                                createIng.setString(1, ingredient);
                                createIng.executeUpdate();

                                // Retrive the id
                                try (ResultSet generatedKeys = createIng.getGeneratedKeys()) {
                                    if (generatedKeys.next()) {
                                        int generateId = generatedKeys.getInt(1);
                                        System.out.println("Generated Ingredient ID: " + generateId);
                                        ingredientId = generateId;
                                    }
                                }
                            }
                        }

                    }
                }

                // Link the ingredient
                try (PreparedStatement ps = connection.prepareStatement("" +
                        "INSERT INTO usesIngredients (recipeId, ingredientId, quantity) VALUES " +
                        "(?, ?, 1)"
                )) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, ingredientId);
                    ps.executeUpdate();
                }
            }

            // Link the image
            try (PreparedStatement ps = connection.prepareStatement("" +
                    "INSERT INTO usesImage (recipeId, imagePath) VALUES " +
                    "(?, ?)"
            )) {
                ps.setInt(1, recipeId);
                ps.setString(2, imagePath);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return false;
        }
        return true;
    }
    public static Recipe getRecipe(String id) {
        try (Connection connection = Database.newConnection()) {
            String sql = "SELECT name, recipe, imagePath, recipes.id FROM recipes JOIN usesImage ON usesImage.recipeId = recipes.id WHERE recipes.id = ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // We don't care about how many ingredients we're missing on this page, as it's displayed via color coordination later on. We can say 0.
                        Recipe newRecipe = new Recipe(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), 0);
                        return newRecipe;
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return null;
        }
    }

    public static Recipe[] getRecipes(String email) {
        try (Connection connection = Database.newConnection()) {
            String sql = "SELECT name, recipe, imagePath, recipes.id FROM recipes JOIN usesImage ON usesImage.recipeId = recipes.id";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    ArrayList<Recipe> recipes = new ArrayList<>();
                    while (rs.next()) {
                        Recipe newRecipe = new Recipe(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), getMissingIngredients(rs.getString(4), email));
                        recipes.add(newRecipe);
                    }
                    return recipes.toArray(new Recipe[0]);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return null;
        }
    }

    private static int getMissingIngredients(String id, String email) {
        int missing = 0;
        // Get the total number of ingredients
        try(Connection connection = Database.newConnection()) {
            String sql = "SELECT COUNT(*) FROM usesIngredients WHERE recipeId = ?";
            try(PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, id);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) {
                        missing = rs.getInt(1);
                    }
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return 0;
        }

        int userHas = 0;
        try(Connection connection = Database.newConnection()) {
            String sql = "SELECT COUNT(*) FROM usesIngredients JOIN ingredients ON usesIngredients.ingredientId = ingredients.id JOIN hasIngredient ON hasIngredient.ingredientId = ingredients.id JOIN users ON users.id = hasIngredient.userId WHERE recipeId = ? AND users.email = ?";
            try(PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, email);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) {
                        userHas = rs.getInt(1);
                        System.out.println(userHas);
                    }
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace(System.err);
            System.out.println("ERROR: " + e.getMessage());
            return 0;
        }

        return missing - userHas;
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
