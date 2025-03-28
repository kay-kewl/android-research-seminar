package com.example.myapplication.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

public class SunView extends View {

    private Paint sunPaint;
    private Paint rayPaint;
    private float centerX;
    private float centerY;
    private float sunRadius = 120;
    private float rayLength = 90;
    private float rotationAngle = 0;
    private float rotationSpeed = 1;
    private boolean isAnimating = false;
    
    // Initial sun and ray properties
    private static final float DEFAULT_SUN_RADIUS = 120;
    private static final float MAX_SUN_RADIUS = 180;
    private static final float DEFAULT_RAY_LENGTH = 90;
    private static final float MAX_RAY_LENGTH = 130;
    private static final float DEFAULT_ROTATION_SPEED = 1;
    private static final float MAX_ROTATION_SPEED = 5;
    
    // Animation duration
    private static final int ANIMATION_DURATION = 3500; // Longer animation
    
    // Animation properties
    private AnimatorSet animatorSet;

    public SunView(Context context) {
        super(context);
        init();
    }

    public SunView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SunView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize Sun paint
        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setColor(Color.YELLOW);
        sunPaint.setStyle(Paint.Style.FILL);
        
        // Initialize Ray paint
        rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rayPaint.setColor(Color.YELLOW);
        rayPaint.setStyle(Paint.Style.STROKE);
        rayPaint.setStrokeWidth(7);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Position the sun in the upper left corner with some margin
        centerX = sunRadius + rayLength + 50;
        centerY = sunRadius + rayLength + 50;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw the sun
        canvas.drawCircle(centerX, centerY, sunRadius, sunPaint);
        
        // Draw rays
        canvas.save();
        canvas.rotate(rotationAngle, centerX, centerY);
        
        // Draw 8 rays around the sun
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 4);
            float startX = centerX + sunRadius * (float) Math.cos(angle);
            float startY = centerY + sunRadius * (float) Math.sin(angle);
            float endX = centerX + (sunRadius + rayLength) * (float) Math.cos(angle);
            float endY = centerY + (sunRadius + rayLength) * (float) Math.sin(angle);
            
            canvas.drawLine(startX, startY, endX, endY, rayPaint);
        }
        
        canvas.restore();
        
        // Rotate the sun rays
        rotationAngle += rotationSpeed;
        if (rotationAngle >= 360) {
            rotationAngle = 0;
        }
        
        // Request next frame for animation
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if touch is inside the sun circle
            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= sunRadius) {
                animateSun();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
    
    private void animateSun() {
        // Prevent multiple animations from running simultaneously
        if (isAnimating) {
            return;
        }
        
        isAnimating = true;
        
        // Cancel any existing animations
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        
        // Create animator for sun radius
        ValueAnimator sunRadiusAnimator = ValueAnimator.ofFloat(sunRadius, MAX_SUN_RADIUS, DEFAULT_SUN_RADIUS);
        sunRadiusAnimator.setDuration(ANIMATION_DURATION);
        sunRadiusAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        sunRadiusAnimator.addUpdateListener(animation -> {
            sunRadius = (float) animation.getAnimatedValue();
        });
        
        // Create animator for ray length
        ValueAnimator rayLengthAnimator = ValueAnimator.ofFloat(rayLength, MAX_RAY_LENGTH, DEFAULT_RAY_LENGTH);
        rayLengthAnimator.setDuration(ANIMATION_DURATION);
        rayLengthAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rayLengthAnimator.addUpdateListener(animation -> {
            rayLength = (float) animation.getAnimatedValue();
        });
        
        // Create animator for rotation speed
        ValueAnimator rotationSpeedAnimator = ValueAnimator.ofFloat(rotationSpeed, MAX_ROTATION_SPEED, DEFAULT_ROTATION_SPEED);
        rotationSpeedAnimator.setDuration(ANIMATION_DURATION);
        rotationSpeedAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotationSpeedAnimator.addUpdateListener(animation -> {
            rotationSpeed = (float) animation.getAnimatedValue();
        });
        
        // Create an animator set to play animations together
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(sunRadiusAnimator, rayLengthAnimator, rotationSpeedAnimator);
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isAnimating = false;
            }
        });
        
        animatorSet.start();
    }
} 