package com.example.databaseswebapp;

public class Ingredient {
    public String name;
    public int quantity;
    public boolean ownedByUser;
    public Ingredient(String name, int quantity, boolean ownedByUser) {
        this.name = name;
        this.quantity = quantity;
        this.ownedByUser = ownedByUser;
    }
}
