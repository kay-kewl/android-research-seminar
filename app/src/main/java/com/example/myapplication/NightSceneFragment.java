package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.views.MoonView;
import com.example.myapplication.views.NightSceneView;
import com.example.myapplication.views.SnowdriftView;
import com.example.myapplication.views.StarsView;

public class NightSceneFragment extends Fragment {

    private NightSceneView nightSceneView;
    private MoonView moonView;
    private StarsView starsView;
    private SnowdriftView snowdriftView;
    
    public NightSceneFragment() {
        // Required empty public constructor
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());
        
        // Create and add the night scene background
        nightSceneView = new NightSceneView(requireContext());
        root.addView(nightSceneView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Create and add the stars view
        starsView = new StarsView(requireContext());
        root.addView(starsView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Create and add the moon view
        moonView = new MoonView(requireContext());
        root.addView(moonView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Create and add the snowdrift view
        snowdriftView = new SnowdriftView(requireContext());
        root.addView(snowdriftView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Wait for layout to complete to set the snow line
        view.post(() -> {
            // Set snow line at 80% of the screen height (lower than before - was 2/3)
            int snowLine = (int) (view.getHeight() * 0.8);
            
            // Tell NightSceneView about the snow line
            nightSceneView.setSnowLine(snowLine);
            
            // Tell StarsView about the snow line (stars only appear above it)
            starsView.setSnowLine(snowLine);
            
            // Tell SnowdriftView about the snow line
            snowdriftView.setSnowLine(snowLine);
        });
    }
} 