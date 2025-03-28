package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity implements MapEventsReceiver {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private MapView mapView;
    private LocationData currentLocation;
    private GeoPoint guessedPoint;
    private GeoPoint actualPoint;
    private boolean hasGuessed = false;
    
    private TextView locationNameTextView;
    private TextView hintTextView;
    private ImageView locationImageView;
    private TextView instructionTextView;
    private LinearLayout bottomPanel;
    private TextView resultTextView;
    private Button nextButton;
    
    private List<LocationData> gameLocations;
    private int currentIndex = 0;
    private static final int LOCATIONS_PER_GAME = 5;
    
    // Stats tracking
    private int bestGuess = Integer.MAX_VALUE;
    private String bestGuessLocation = "";
    private int worstGuess = 0;
    private String worstGuessLocation = "";
    private int correctLocations = 0;
    private int totalScore = 0;
    private static final int PERFECT_SCORE_PER_LOCATION = 5000;
    private static final int DISTANCE_THRESHOLD_KM = 100; // Threshold for "correct" guess in km
    private long startTime = 0; // Track time for fastest answer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        
        // Check permissions
        checkPermissions();
        
        // Initialize views
        locationNameTextView = findViewById(R.id.locationNameTextView);
        hintTextView = findViewById(R.id.hintTextView);
        locationImageView = findViewById(R.id.locationImageView);
        instructionTextView = findViewById(R.id.instructionTextView);
        bottomPanel = findViewById(R.id.bottomPanel);
        resultTextView = findViewById(R.id.resultTextView);
        nextButton = findViewById(R.id.nextButton);
        
        // Hide location name initially (will show after guessing)
        locationNameTextView.setVisibility(View.INVISIBLE);
        
        // Setup map
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        
        IMapController mapController = mapView.getController();
        mapController.setZoom(2.0);
        mapController.setCenter(new GeoPoint(0, 0)); // Center on equator
        
        // Add map click listener
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);
        mapView.getOverlays().add(0, mapEventsOverlay);
        
        // Set up next button
        nextButton.setOnClickListener(v -> loadNextLocation());
        
        // Initialize game with random locations
        gameLocations = LocationProvider.getRandomLocations(LOCATIONS_PER_GAME);
        loadLocation(0);
    }
    
    private void loadLocation(int index) {
        if (index >= gameLocations.size()) {
            // Game over, show results screen
            showGameResults();
            return;
        }
        
        // Reset game state
        hasGuessed = false;
        guessedPoint = null;
        actualPoint = null;
        mapView.getOverlays().clear();
        mapView.getOverlays().add(new MapEventsOverlay(this));
        
        // Hide results panel
        bottomPanel.setVisibility(View.GONE);
        
        // Hide location name initially (will show after guessing)
        locationNameTextView.setVisibility(View.INVISIBLE);
        
        // Reset map view
        IMapController mapController = mapView.getController();
        mapController.setZoom(2.0);
        mapController.setCenter(new GeoPoint(0, 0));
        
        // Load location data
        currentLocation = gameLocations.get(index);
        actualPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
        
        // Show hint
        hintTextView.setText(currentLocation.getHint());
        
        // Load image
        Glide.with(this)
                .load(currentLocation.getImageUrl())
                .placeholder(R.color.colorPrimary)
                .into(locationImageView);
                
        // Start timing for this location
        startTime = System.currentTimeMillis();
    }
    
    private void loadNextLocation() {
        currentIndex++;
        loadLocation(currentIndex);
    }
    
    private void showResult() {
        // Make location name visible
        locationNameTextView.setText(currentLocation.getName());
        locationNameTextView.setVisibility(View.VISIBLE);
        
        // Calculate distance
        double distance = LocationProvider.calculateDistance(
                guessedPoint.getLatitude(), guessedPoint.getLongitude(),
                actualPoint.getLatitude(), actualPoint.getLongitude());
        
        // Round distance to whole number
        int roundedDistance = (int) Math.round(distance);
        
        // Update statistics
        updateStats(roundedDistance, currentLocation.getName());
        
        // Set result text
        String resultText = "Ваш ответ был на " + roundedDistance + " км от цели!";
        resultTextView.setText(resultText);
        
        // Set result color based on accuracy
        if (distance < DISTANCE_THRESHOLD_KM) {
            resultTextView.setTextColor(ContextCompat.getColor(this, R.color.colorCorrect));
        } else {
            resultTextView.setTextColor(ContextCompat.getColor(this, R.color.colorIncorrect));
        }
        
        // Show bottom panel
        bottomPanel.setVisibility(View.VISIBLE);
        
        // Add marker for actual location
        Marker actualMarker = new Marker(mapView);
        actualMarker.setPosition(actualPoint);
        actualMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        actualMarker.setTitle(currentLocation.getName());
        actualMarker.setSnippet("Настоящее местоположение");
        actualMarker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_compass));
        mapView.getOverlays().add(actualMarker);
        
        // Draw line between guessed and actual points
        Polyline line = new Polyline();
        List<GeoPoint> points = new ArrayList<>();
        points.add(guessedPoint);
        points.add(actualPoint);
        line.setPoints(points);
        line.setColor(Color.RED);
        line.setWidth(5f);
        mapView.getOverlays().add(line);
        
        // Zoom to show both points
        mapView.zoomToBoundingBox(line.getBounds(), true, 100);
        
        // Update instruction
        instructionTextView.setText("Нажмите 'Следующая локация' для продолжения");
    }
    
    private void updateStats(int distance, String locationName) {
        // Update best and worst guesses
        if (distance < bestGuess) {
            bestGuess = distance;
            bestGuessLocation = locationName;
        }
        
        if (distance > worstGuess) {
            worstGuess = distance;
            worstGuessLocation = locationName;
        }
        
        // Count correct locations (within threshold)
        if (distance < DISTANCE_THRESHOLD_KM) {
            correctLocations++;
        }
        
        // Calculate score for this guess
        // Score decreases as distance increases
        int scoreForGuess = Math.max(0, PERFECT_SCORE_PER_LOCATION - (distance * 2));
        totalScore += scoreForGuess;
    }
    
    private void showGameResults() {
        Intent intent = new Intent(this, GameResultsActivity.class);
        intent.putExtra(GameResultsActivity.EXTRA_BEST_GUESS, bestGuess);
        intent.putExtra(GameResultsActivity.EXTRA_BEST_GUESS_LOCATION, bestGuessLocation);
        intent.putExtra(GameResultsActivity.EXTRA_WORST_GUESS, worstGuess);
        intent.putExtra(GameResultsActivity.EXTRA_WORST_GUESS_LOCATION, worstGuessLocation);
        intent.putExtra(GameResultsActivity.EXTRA_CORRECT_LOCATIONS, correctLocations);
        intent.putExtra(GameResultsActivity.EXTRA_TOTAL_SCORE, totalScore);
        startActivity(intent);
        finish();
    }
    
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        if (!hasGuessed) {
            // Record guess
            guessedPoint = p;
            hasGuessed = true;
            
            // Add marker for guessed location
            Marker guessMarker = new Marker(mapView);
            guessMarker.setPosition(guessedPoint);
            guessMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            guessMarker.setTitle("Ваш ответ");
            mapView.getOverlays().add(guessMarker);
            
            // Show result
            showResult();
        }
        return true;
    }
    
    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }
    
    private void checkPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                    permissionsToRequest.toArray(new String[0]), 
                    PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "Permissions required for map functionality", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
} 