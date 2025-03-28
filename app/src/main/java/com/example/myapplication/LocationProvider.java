package com.example.myapplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Helper class to provide game locations
 */
public class LocationProvider {
    
    private static final List<LocationData> locations = new ArrayList<>();
    private static final Random random = new Random();
    
    static {
        // Initialize with a set of interesting locations
        // Using CC0 images from Unsplash and Pixabay that don't require attribution
        locations.add(new LocationData(
                "Eiffel Tower, Paris",
                48.8584, 2.2945,
                "https://images.pexels.com/photos/699466/pexels-photo-699466.jpeg",
                "This iconic iron tower is located in a major European capital"));
                
        locations.add(new LocationData(
                "Statue of Liberty, New York",
                40.6892, -74.0445,
                "https://images.pexels.com/photos/290386/pexels-photo-290386.jpeg",
                "A gift from France to the United States"));
                
        locations.add(new LocationData(
                "Colosseum, Rome",
                41.8902, 12.4922,
                "https://images.pexels.com/photos/1797161/pexels-photo-1797161.jpeg",
                "An ancient amphitheater where gladiators once fought"));
                
        locations.add(new LocationData(
                "Great Wall of China",
                40.4319, 116.5704,
                "https://images.pexels.com/photos/1731041/pexels-photo-1731041.jpeg",
                "One of the world's longest structures, built over centuries"));
                
        locations.add(new LocationData(
                "Taj Mahal, India",
                27.1751, 78.0421,
                "https://images.pexels.com/photos/1603650/pexels-photo-1603650.jpeg",
                "A white marble mausoleum built by a Mughal emperor"));
                
        locations.add(new LocationData(
                "Sydney Opera House, Australia",
                -33.8568, 151.2153,
                "https://images.pexels.com/photos/1878293/pexels-photo-1878293.jpeg",
                "A distinctive performance venue with a shell-like design"));
                
        locations.add(new LocationData(
                "Machu Picchu, Peru",
                -13.1631, -72.5450,
                "https://images.pexels.com/photos/2356045/pexels-photo-2356045.jpeg",
                "An ancient Incan citadel set in the Andes Mountains"));
                
        locations.add(new LocationData(
                "Pyramids of Giza, Egypt",
                29.9773, 31.1325,
                "https://images.pexels.com/photos/3290075/pexels-photo-3290075.jpeg",
                "Ancient structures built as tombs for pharaohs"));
                
        locations.add(new LocationData(
                "Mount Fuji, Japan",
                35.3606, 138.7274,
                "https://images.pexels.com/photos/3408353/pexels-photo-3408353.jpeg",
                "Japan's highest mountain and an active volcano"));
                
        locations.add(new LocationData(
                "Grand Canyon, USA",
                36.0544, -112.2401,
                "https://images.pexels.com/photos/33041/antelope-canyon-lower-canyon-arizona.jpg",
                "A steep-sided canyon carved by the Colorado River"));
    }
    
    /**
     * Get a random location
     */
    public static LocationData getRandomLocation() {
        return locations.get(random.nextInt(locations.size()));
    }
    
    /**
     * Get a specific number of random locations without duplicates
     */
    public static List<LocationData> getRandomLocations(int count) {
        count = Math.min(count, locations.size());
        List<LocationData> shuffled = new ArrayList<>(locations);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, count);
    }
    
    /**
     * Calculate distance between two points in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula to calculate distance between two points on Earth
        final int R = 6371; // Earth radius in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                 
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in kilometers
    }
} 