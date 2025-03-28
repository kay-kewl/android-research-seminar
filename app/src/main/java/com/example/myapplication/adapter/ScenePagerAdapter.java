package com.example.myapplication.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.myapplication.NightSceneFragment;
import com.example.myapplication.fragments.WeatherSceneFragment;

public class ScenePagerAdapter extends FragmentStateAdapter {
    
    public ScenePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return the correct fragment based on position
        switch (position) {
            case 0:
                return new WeatherSceneFragment();
            case 1:
                return new NightSceneFragment();
            default:
                return new WeatherSceneFragment();
        }
    }

    @Override
    public int getItemCount() {
        // Now we have two pages - weather scene and night scene
        return 2;
    }
} 