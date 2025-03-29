package com.example.myapplication;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_MS = 300;

    // C major scale frequencies (C4 to C5)
    public static final float[] NOTE_FREQUENCIES = {
            261.63f, // C4
            293.66f, // D4
            329.63f, // E4
            349.23f, // F4
            392.00f, // G4
            440.00f, // A4
            493.88f, // B4
            523.25f, // C5
            587.33f, // D5
            659.25f, // E5
            698.46f, // F5
            783.99f, // G5
            880.00f, // A5
            987.77f, // B5
            1046.50f, // C6
            1174.66f  // D6
    };

    /**
     * Generates a simple sine wave for the given frequency
     */
    public static byte[] generateTone(float frequency) {
        int numSamples = (SAMPLE_RATE * DURATION_MS) / 1000;
        double[] sample = new double[numSamples];
        byte[] generatedSound = new byte[2 * numSamples];

        // Generate sine wave
        for (int i = 0; i < numSamples; i++) {
            sample[i] = Math.sin(2 * Math.PI * i * frequency / SAMPLE_RATE);
        }

        // Convert to 16-bit PCM sound array
        int idx = 0;
        for (double dVal : sample) {
            short val = (short) (dVal * 32767);
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        return generatedSound;
    }

    /**
     * Plays the generated tone
     */
    public static void playTone(Context context, float frequency) {
        byte[] generatedSound = generateTone(frequency);
        
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSound.length,
                AudioTrack.MODE_STATIC);

        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.play();
        
        // Auto release after playing
        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                track.release();
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
                // Not used
            }
        });
        
        audioTrack.setNotificationMarkerPosition(generatedSound.length / 4);
    }
} 