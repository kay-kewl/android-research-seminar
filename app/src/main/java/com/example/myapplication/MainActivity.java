package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.adapter.ScenePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    
    private ViewPager2 viewPager;
    private ScenePagerAdapter pagerAdapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewPager2 and adapter
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new ScenePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Set up TabLayout with ViewPager2
        tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // Set descriptive names for each scene
            if (position == 0) {
                tab.setText(R.string.sunny_scene);
            } else if (position == 1) {
                tab.setText(R.string.night_scene);
            }
        }).attach();
    }
} 