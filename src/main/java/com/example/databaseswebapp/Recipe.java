package com.example.databaseswebapp;

public class Recipe {
    private String name;
    private String recipe;
    private int missing;
    private int id;
    private String imagePath;

    public Recipe(String name, String recipe, String path, int id, int missing) {
        this.name = name;
        this.recipe = recipe;
        this.imagePath = path;
        this.id = id;
        this.missing = missing;
    }

    public String getRecipe() {
        return recipe;
    }

    public String getName() {
        return name;
    }

    public String getImagePath() {
        System.out.println(imagePath);
        return imagePath;
    }

    public int getMissing() {
        return missing;
    }

    public int getId() {
        return id;
    }
}
