package com.example.myapplication.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class NightSceneView extends View {

    private Paint skyPaint;
    private Paint snowPaint;
    private int snowLine; // Position where snow starts (ground level)
    
    public NightSceneView(Context context) {
        super(context);
        init();
    }

    public NightSceneView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NightSceneView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize Sky paint - dark blue for night sky
        skyPaint = new Paint();
        skyPaint.setColor(Color.rgb(25, 25, 112)); // Midnight blue
        
        // Initialize Snow paint - white for snow
        snowPaint = new Paint();
        snowPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Position where snow starts (ground level at 2/3 of screen height)
        snowLine = (int)(h * 2.0f / 3.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        // Calculate where to draw snow rectangle
        // Move it 50px higher so snowdrifts appear connected to it
        int visualSnowLine = snowLine - 30;
        
        // Draw night sky (top portion of the view)
        canvas.drawRect(0, 0, width, visualSnowLine, skyPaint);
        
        // Draw snow ground (bottom portion of the view)
        canvas.drawRect(0, visualSnowLine, width, height, snowPaint);
    }
    
    // This method can be used from outside to get snow line position
    public int getSnowLine() {
        return snowLine;
    }
    
    // Add setter method for snowLine
    public void setSnowLine(int snowLine) {
        this.snowLine = snowLine;
        invalidate(); // Redraw with new snow line
    }
} 