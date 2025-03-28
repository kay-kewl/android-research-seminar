package com.example.myapplication;

/**
 * Data class to store information about a location in the game
 */
public class LocationData {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String imageUrl;
    private final String hint;

    public LocationData(String name, double latitude, double longitude, String imageUrl, String hint) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.hint = hint;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getHint() {
        return hint;
    }
} 