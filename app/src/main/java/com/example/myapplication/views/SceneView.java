package com.example.myapplication.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SceneView extends View {

    private Paint skyPaint;
    private Paint grassPaint;
    
    public SceneView(Context context) {
        super(context);
        init();
    }

    public SceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SceneView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize Sky paint
        skyPaint = new Paint();
        skyPaint.setColor(Color.rgb(135, 206, 235)); // Sky blue
        
        // Initialize Grass paint
        grassPaint = new Paint();
        grassPaint.setColor(Color.rgb(34, 139, 34)); // Forest green
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw sky (top 2/3 of the view)
        canvas.drawRect(0, 0, width, height * 2 / 3, skyPaint);
        
        // Draw grass (bottom 1/3 of the view)
        canvas.drawRect(0, height * 2 / 3, width, height, grassPaint);
    }
} 