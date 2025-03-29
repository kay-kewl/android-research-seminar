package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "sound_grid_channel";
    private static final int GRID_SIZE = 4; // 4x4 grid
    private NotificationManager notificationManager;
    private int notificationId = 1;

    // Sound resources for different instruments
    // Download sound samples from sites like:
    // - freesound.org
    // - mixkit.co/free-sound-effects
    // - soundbible.com
    // - zapsplat.com
    
    // Temporarily use dummy resource IDs until actual sound files are added
    private final int[] soundResources = {
            R.raw.drum1,     // Bass drum
            R.raw.drum2,     // Snare drum
            R.raw.drum3,     // Hi-hat
            R.raw.drum4,     // Cymbal
            R.raw.piano1,    // Piano C chord
            R.raw.piano2,    // Piano G chord
            R.raw.piano3,    // Piano F chord
            R.raw.piano4,    // Piano Am chord
            R.raw.synth1,    // Synth pad
            R.raw.synth2,    // Synth arpeggio
            R.raw.synth3,    // Synth bass
            R.raw.synth4,    // Synth lead
            R.raw.fx1,       // Sound effect 1
            R.raw.fx2,       // Sound effect 2
            R.raw.fx3,       // Sound effect 3
            R.raw.fx4        // Sound effect 4
    };
    
    // Sound player map to track active players
    private Map<Integer, SoundPlayer> soundPlayers = new HashMap<>();
    
    // Colors for the sound buttons
    private final int[] buttonColors = {
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#3F51B5"), // Indigo
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#03A9F4"), // Light Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#8BC34A"), // Light Green
            Color.parseColor("#CDDC39"), // Lime
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FF5722")  // Deep Orange
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createSoundGrid();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Sound Grid Channel";
            String description = "Channel for Sound Grid Composer notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createSoundGrid() {
        GridLayout gridLayout = findViewById(R.id.sound_grid);
        gridLayout.setColumnCount(GRID_SIZE);
        gridLayout.setRowCount(GRID_SIZE);
        
        // Get the available width and height for the grid
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int titleHeight = findViewById(R.id.title_text).getHeight() + 
                          (int) getResources().getDimension(R.dimen.activity_vertical_margin) * 2;
        
        // Adjust for margins
        int horizontalMargin = (int) (16 * getResources().getDisplayMetrics().density);
        int availableWidth = screenWidth - (horizontalMargin * 2);
        
        // Calculate the cell size to fit the screen width
        // We divide by GRID_SIZE to get the number of cells per row
        int cellSize = (availableWidth - (GRID_SIZE - 1) * horizontalMargin / GRID_SIZE) / GRID_SIZE;
        
        // Create grid buttons
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            final int soundIndex = i;
            
            // Create a custom view for each grid square
            View buttonView = new View(this);
            
            // Apply background drawable for ripple effect and rounded corners
            buttonView.setBackground(ContextCompat.getDrawable(this, R.drawable.sound_button_background));
            
            // Set the background tint for the color
            buttonView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(buttonColors[i]));
            
            // Set layout parameters for a square grid
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.columnSpec = GridLayout.spec(i % GRID_SIZE);
            params.rowSpec = GridLayout.spec(i / GRID_SIZE);
            params.setMargins(4, 4, 4, 4);
            buttonView.setLayoutParams(params);

            // Set click and touch listener for different behaviors
            buttonView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Start playing sound
                            playSound(soundIndex);
                            v.setPressed(true);
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            // If released quickly, just play once
                            if (event.getEventTime() - event.getDownTime() < 500) {
                                // Short press - already played once, do nothing more
                            } else {
                                // Long press ended, stop looping
                                stopSoundLoop(soundIndex);
                            }
                            v.setPressed(false);
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            // If pressed for a while, start looping
                            if (event.getEventTime() - event.getDownTime() >= 500) {
                                startSoundLoop(soundIndex);
                            }
                            return true;
                            
                        case MotionEvent.ACTION_CANCEL:
                            stopSoundLoop(soundIndex);
                            v.setPressed(false);
                            return true;
                    }
                    return false;
                }
            });

            gridLayout.addView(buttonView);
        }
    }
    
    private void playSound(int soundIndex) {
        // Get or create a sound player for this button
        SoundPlayer player = getSoundPlayer(soundIndex);
        player.playOnce();
        
        // Show notification
        String soundName = getSoundNameFromIndex(soundIndex);
        showNotification("Sound Grid", soundName + " played", soundIndex);
    }
    
    private void startSoundLoop(int soundIndex) {
        SoundPlayer player = getSoundPlayer(soundIndex);
        player.startLooping();
    }
    
    private void stopSoundLoop(int soundIndex) {
        SoundPlayer player = getSoundPlayer(soundIndex);
        player.stopLooping();
    }
    
    private SoundPlayer getSoundPlayer(int soundIndex) {
        if (!soundPlayers.containsKey(soundIndex)) {
            soundPlayers.put(soundIndex, new SoundPlayer(this, soundResources[soundIndex]));
        }
        return soundPlayers.get(soundIndex);
    }
    
    private String getSoundNameFromIndex(int index) {
        String[] soundNames = {
            "Bass Drum", "Snare Drum", "Hi-Hat", "Cymbal",
            "Piano C", "Piano G", "Piano F", "Piano Am",
            "Synth Pad", "Synth Arpeggio", "Synth Bass", "Synth Lead",
            "Effect 1", "Effect 2", "Effect 3", "Effect 4"
        };
        return soundNames[index];
    }

    private void showNotification(String title, String message, int soundIndex) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(buttonColors[soundIndex])
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        
        notificationManager.notify(notificationId++, builder.build());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all sound players
        for (SoundPlayer player : soundPlayers.values()) {
            player.release();
        }
        soundPlayers.clear();
    }
} 