package com.example.myapplication.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

public class MoonView extends View {

    private Paint moonPaint;
    private float centerX;
    private float centerY;
    private float moonRadius = 120;
    private float rotation = 0;
    private boolean isAnimating = false;
    private ObjectAnimator rotationAnimator;
    
    // Animation duration
    private static final int SWING_DURATION = 3000;
    
    public MoonView(Context context) {
        super(context);
        init();
    }

    public MoonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize Moon paint
        moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonPaint.setColor(Color.rgb(255, 253, 208)); // Light yellow for moon
        moonPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Position the moon in the top left area of the sky
        centerX = w * 0.25f;
        centerY = h * 0.25f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Save the canvas state before rotating
        canvas.save();
        
        // Rotate the canvas around the moon center
        canvas.rotate(rotation, centerX, centerY);
        
        // Draw the main full moon circle
        canvas.drawCircle(centerX, centerY, moonRadius, moonPaint);
        
        // Draw a slightly offset circle with sky color to create crescent
        Paint skyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        skyPaint.setColor(Color.rgb(25, 25, 112)); // Midnight blue
        skyPaint.setStyle(Paint.Style.FILL);
        
        // Offset to create crescent (shift to the right)
        float offsetX = moonRadius * 0.35f;
        canvas.drawCircle(centerX + offsetX, centerY, moonRadius * 0.95f, skyPaint);
        
        // Restore the canvas to its original state
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if touch is inside the moon
            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= moonRadius) {
                animateMoon();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
    
    private void animateMoon() {
        // Prevent multiple animations from running simultaneously
        if (isAnimating) {
            return;
        }
        
        isAnimating = true;
        
        // Cancel any existing animations
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
        
        // Create a simpler, smoother pendulum animation
        // Use fewer keyframes for smoother motion
        rotationAnimator = ObjectAnimator.ofFloat(this, "rotation", 0, 25, 0, -20, 0, 15, 0, -10, 0, 5, 0);
        rotationAnimator.setDuration(SWING_DURATION);
        
        // Use AccelerateDecelerateInterpolator for smoother swinging
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Add a listener to reset the animating flag
        rotationAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isAnimating = false;
            }
        });
        
        rotationAnimator.start();
    }
    
    // Getter and setter for the rotation property (needed for the ObjectAnimator)
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float rotation) {
        this.rotation = rotation;
        invalidate(); // Redraw the view with the new rotation
    }
} 