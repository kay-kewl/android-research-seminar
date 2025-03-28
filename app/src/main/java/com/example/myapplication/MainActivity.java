package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;

import java.io.File;

import com.example.myapplication.activities.FactSettingsActivity;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OpenStreetMap configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));
        
        setContentView(R.layout.activity_main);
        
        // Set up the play button
        Button button = findViewById(R.id.button);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Launch Facts Settings instead of Game
                    Intent intent = new Intent(MainActivity.this, FactSettingsActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
} 