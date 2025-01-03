package com.example.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StartActivity extends AppCompatActivity {

    private static final int INSTRUCTION_DELAY_MS = 1000;
    private TextView instructionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.startLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View startLayout = findViewById(R.id.startLayout);
        instructionText = findViewById(R.id.instructionText);

        startLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToMain();
            }
        });

        startLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                showInstructionText();
            }
        }, INSTRUCTION_DELAY_MS);
    }

    private void navigateToMain() {
        Intent intent = new Intent(StartActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showInstructionText() {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(500);
        fadeIn.setFillAfter(true);

        instructionText.startAnimation(fadeIn);
        instructionText.setVisibility(View.VISIBLE);
    }
}