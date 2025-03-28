package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameResultsActivity extends AppCompatActivity {

    public static final String EXTRA_BEST_GUESS = "best_guess";
    public static final String EXTRA_BEST_GUESS_LOCATION = "best_guess_location";
    public static final String EXTRA_WORST_GUESS = "worst_guess";
    public static final String EXTRA_WORST_GUESS_LOCATION = "worst_guess_location";
    public static final String EXTRA_CORRECT_LOCATIONS = "correct_locations";
    public static final String EXTRA_TOTAL_SCORE = "total_score";
    
    private static final int DISTANCE_THRESHOLD_KM = 100; // Threshold for "correct" guess in km

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_results);
        
        // Set up buttons
        Button tryAgainButton = findViewById(R.id.tryAgainButton);
        Button menuButton = findViewById(R.id.menuButton);
        
        // Get statistics from intent
        Intent intent = getIntent();
        int bestGuess = intent.getIntExtra(EXTRA_BEST_GUESS, 0);
        String bestGuessLocation = intent.getStringExtra(EXTRA_BEST_GUESS_LOCATION);
        int worstGuess = intent.getIntExtra(EXTRA_WORST_GUESS, 0);
        String worstGuessLocation = intent.getStringExtra(EXTRA_WORST_GUESS_LOCATION);
        int correctLocations = intent.getIntExtra(EXTRA_CORRECT_LOCATIONS, 0);
        int totalScore = intent.getIntExtra(EXTRA_TOTAL_SCORE, 0);
        
        // Display statistics
        TextView totalScoreTextView = findViewById(R.id.totalScoreTextView);
        TextView bestGuessTextView = findViewById(R.id.bestGuessTextView);
        TextView worstGuessTextView = findViewById(R.id.worstGuessTextView);
        TextView locationsCorrectTextView = findViewById(R.id.locationsCorrectTextView);
        
        totalScoreTextView.setText("Общий счет: " + totalScore);
        bestGuessTextView.setText("Самый точный ответ: " + bestGuess + " км (" + bestGuessLocation + ")");
        worstGuessTextView.setText("Наименее точный ответ: " + worstGuess + " км (" + worstGuessLocation + ")");
        locationsCorrectTextView.setText("Угадано мест (в пределах " + DISTANCE_THRESHOLD_KM + " км): " 
                + correctLocations + " из 5");
        
        // Set up button click listeners
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start a new game
                Intent gameIntent = new Intent(GameResultsActivity.this, GameActivity.class);
                startActivity(gameIntent);
                finish();
            }
        });
        
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Return to main menu
                finish();
            }
        });
    }
} 