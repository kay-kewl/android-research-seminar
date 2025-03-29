package com.example.myapplication;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

public class SoundPlayer {
    private MediaPlayer mediaPlayer;
    private boolean isLooping = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable loopRunnable;
    private final Context context;
    private final int soundResId;
    private final int loopInterval = 500; // 500ms between sound repetitions

    public SoundPlayer(Context context, int soundResId) {
        this.context = context;
        this.soundResId = soundResId;
    }

    public void playOnce() {
        // Stop any existing playback
        stop();
        
        // Create and start new MediaPlayer
        mediaPlayer = MediaPlayer.create(context, soundResId);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
            mediaPlayer.start();
        }
    }

    public void startLooping() {
        // If already looping, do nothing
        if (isLooping) return;
        
        isLooping = true;
        playOnce(); // Play immediately for the first time
        
        // Setup looping using handler
        loopRunnable = new Runnable() {
            @Override
            public void run() {
                if (isLooping) {
                    playOnce();
                    handler.postDelayed(this, loopInterval);
                }
            }
        };
        
        // Schedule next playback
        handler.postDelayed(loopRunnable, loopInterval);
    }

    public void stopLooping() {
        isLooping = false;
        if (loopRunnable != null) {
            handler.removeCallbacks(loopRunnable);
        }
        // Let current playback complete normally
    }
    
    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    public void release() {
        isLooping = false;
        handler.removeCallbacksAndMessages(null);
        stop();
    }
} 