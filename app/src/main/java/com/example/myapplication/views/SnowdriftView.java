package com.example.myapplication.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SnowdriftView extends View {

    private Paint snowdriftPaint;
    private Paint snowflakePaint;
    private List<Snowdrift> snowdrifts;
    private List<Snowflake> snowflakes;
    private Random random;
    private int snowLine; // Position where snow starts (ground level)
    private boolean isSnowing = false;
    private boolean hasCreatedDrifts = false; // Track if drifts have been created
    private boolean isCreatingNewSnow = false; // Track if we're still creating new snowflakes
    private boolean hasLandedSnow = false; // Track if any snow has landed on drifts
    private boolean isAnimating = false; // Track if we're in the middle of animation
    
    // Animation durations and delays
    private static final int INITIAL_SNOW_DURATION = 2000; // Time for snow to fall before drifts grow
    private static final int GROW_DURATION = 2500;
    private static final int SNOW_FALL_DURATION = 4000; // Increased time for snow to fall
    private static final int WAIT_FOR_LAST_SNOW_DURATION = 2000; // Increased to wait for more snow to land
    private static final int SHRINK_DURATION = 2500;
    
    // Snowdrift sizes - keeping the larger drifts
    private static final float DRIFT_WIDTH_SMALL = 120f;
    private static final float DRIFT_WIDTH_MEDIUM = 150f;
    private static final float DRIFT_WIDTH_LARGE = 180f;
    private static final float DRIFT_HEIGHT_SMALL = 60f;
    private static final float DRIFT_HEIGHT_MEDIUM = 80f;
    private static final float DRIFT_HEIGHT_LARGE = 100f;
    
    public SnowdriftView(Context context) {
        super(context);
        init();
    }

    public SnowdriftView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SnowdriftView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize Snowdrift paint (white with slight transparency)
        snowdriftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        snowdriftPaint.setColor(Color.WHITE);
        snowdriftPaint.setStyle(Paint.Style.FILL);
        
        // Initialize Snowflake paint
        snowflakePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        snowflakePaint.setColor(Color.WHITE);
        snowflakePaint.setStyle(Paint.Style.FILL);
        
        // Initialize snowdrifts and snowflakes lists
        snowdrifts = new ArrayList<>();
        snowflakes = new ArrayList<>();
        random = new Random();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Force snowdrift creation regardless of condition
        createSnowdrifts();
    }

    public void setSnowLine(int snowLine) {
        this.snowLine = snowLine;
        
        // Always recreate snowdrifts when snow line changes
        createSnowdrifts();
    }

    private void createSnowdrifts() {
        // Only create snowdrifts if we have valid dimensions
        if (getWidth() <= 0) {
            // If width is not available yet, post for later
            post(this::createSnowdrifts);
            return;
        }
        
        // Clear any existing snowdrifts
        snowdrifts.clear();
        
        int width = getWidth();
        
        // Position snowdrifts ABOVE the snow line to make them visible
        int driftY = snowLine - 20; // Position them slightly above the snow line
        
        // Create 3 larger snowdrifts spaced across the bottom
        Snowdrift drift1 = new Snowdrift(width * 0.2f, driftY, DRIFT_WIDTH_SMALL, DRIFT_HEIGHT_SMALL);
        Snowdrift drift2 = new Snowdrift(width * 0.5f, driftY, DRIFT_WIDTH_MEDIUM, DRIFT_HEIGHT_MEDIUM);
        Snowdrift drift3 = new Snowdrift(width * 0.8f, driftY, DRIFT_WIDTH_LARGE, DRIFT_HEIGHT_LARGE);
        
        snowdrifts.add(drift1);
        snowdrifts.add(drift2);
        snowdrifts.add(drift3);
        
        // Mark that we've created the drifts to avoid recreation
        hasCreatedDrifts = true;
        
        // Store original sizes
        for (Snowdrift drift : snowdrifts) {
            drift.originalWidth = drift.width;
            drift.originalHeight = drift.height;
        }
        
        // Force redraw
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw all snowdrifts
        for (Snowdrift drift : snowdrifts) {
            drawSnowdrift(canvas, drift);
        }
        
        // Update and draw snowflakes
        if (isSnowing) {
            // Create new snowflakes only if we're in the creation phase
            if (isCreatingNewSnow && random.nextInt(2) == 0) {
                createSnowflake();
            }
            
            // Update and draw existing snowflakes
            Iterator<Snowflake> iterator = snowflakes.iterator();
            while (iterator.hasNext()) {
                Snowflake snowflake = iterator.next();
                snowflake.update();
                drawSnowflake(canvas, snowflake);
                
                // Check if any snow has landed on drifts
                if (snowflake.hasLanded && !hasLandedSnow) {
                    hasLandedSnow = true;
                }
                
                // Remove snowflakes that have landed or gone off-screen
                if (snowflake.y > getHeight() + 50 || snowflake.hasLanded) { // Allow snow to fall off-screen before removal
                    iterator.remove();
                }
            }
        }
        
        // Continue animation
        if (isSnowing || !snowflakes.isEmpty() || isAnimating) {
            invalidate();
        }
    }
    
    private void drawSnowdrift(Canvas canvas, Snowdrift drift) {
        float left = drift.x - drift.width;
        float right = drift.x + drift.width;
        float top = drift.y - drift.height;
        float bottom = drift.y + drift.height / 3;
        
        // Draw a semi-ellipse shape for the snowdrift
        Path path = new Path();
        path.moveTo(left, drift.y);
        path.quadTo(drift.x, top, right, drift.y);
        path.quadTo(drift.x, bottom, left, drift.y);
        path.close();
        
        canvas.drawPath(path, snowdriftPaint);
    }
    
    private void createSnowflake() {
        float x = random.nextFloat() * getWidth();
        float y = 0; // Start at the top of the screen
        float size = 3 + random.nextFloat() * 4; // Random size 3-7
        float speed = 2 + random.nextFloat() * 3; // Random speed 2-5
        
        Snowflake snowflake = new Snowflake(x, y, size, speed);
        snowflakes.add(snowflake);
    }
    
    private void drawSnowflake(Canvas canvas, Snowflake snowflake) {
        canvas.drawCircle(snowflake.x, snowflake.y, snowflake.size, snowflakePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Only process touches in the snow area (below snow line)
            if (event.getY() >= snowLine) {
                // Start snow animation
                startSnowSequence();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
    
    private void startSnowSequence() {
        // If currently animating, don't start a new animation
        if (isSnowing || isAnimating) {
            return;
        }
        
        // Reset animation state
        isAnimating = true;
        hasLandedSnow = false;
        snowflakes.clear(); // Clear any old snowflakes to prevent them from falling from below
        
        // Start the snow animation sequence
        // 1. Start snow falling
        startSnow();
        
        // 2. After initial delay (when first snow lands), grow the drifts
        postDelayed(() -> {
            if (hasLandedSnow) {
                growDrifts();
            } else {
                // If no snow has landed yet, check again shortly
                postDelayed(this::growDrifts, 500);
            }
        }, INITIAL_SNOW_DURATION);
        
        // 3. After more time, stop creating new snowflakes
        postDelayed(() -> {
            isCreatingNewSnow = false;
        }, INITIAL_SNOW_DURATION + SNOW_FALL_DURATION);
        
        // 4. After waiting for last snowflakes to land, shrink drifts
        postDelayed(this::shrinkDrifts, 
                   INITIAL_SNOW_DURATION + SNOW_FALL_DURATION + WAIT_FOR_LAST_SNOW_DURATION);
    }
    
    private void startSnow() {
        isSnowing = true;
        isCreatingNewSnow = true;
        invalidate();
    }
    
    private void growDrifts() {
        // Animate snowdrifts growing
        List<Animator> growAnimators = new ArrayList<>();
        
        for (Snowdrift drift : snowdrifts) {
            ValueAnimator widthAnimator = ValueAnimator.ofFloat(drift.width, drift.width * 2.0f);
            widthAnimator.setDuration(GROW_DURATION);
            widthAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            widthAnimator.addUpdateListener(animation -> {
                drift.width = (float) animation.getAnimatedValue();
                invalidate();
            });
            
            ValueAnimator heightAnimator = ValueAnimator.ofFloat(drift.height, drift.height * 2.2f);
            heightAnimator.setDuration(GROW_DURATION);
            heightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            heightAnimator.addUpdateListener(animation -> {
                drift.height = (float) animation.getAnimatedValue();
                invalidate();
            });
            
            growAnimators.add(widthAnimator);
            growAnimators.add(heightAnimator);
        }
        
        // Create animator set for growing
        AnimatorSet growSet = new AnimatorSet();
        growSet.playTogether(growAnimators);
        growSet.start();
    }
    
    private void shrinkDrifts() {
        // Make sure most snowflakes have landed before shrinking
        // Count how many snowflakes are still visible on screen
        int visibleSnowflakes = 0;
        for (Snowflake flake : snowflakes) {
            if (flake.y < getHeight() / 2) { // Only count flakes in upper half of screen
                visibleSnowflakes++;
            }
        }
        
        // If more than 5 snowflakes are still in the upper half, wait longer
        if (visibleSnowflakes > 5) {
            postDelayed(this::shrinkDrifts, 500);
            return;
        }
        
        // Animate snowdrifts shrinking back to normal (original) size
        List<Animator> shrinkAnimators = new ArrayList<>();
        
        for (Snowdrift drift : snowdrifts) {
            ValueAnimator widthAnimator = ValueAnimator.ofFloat(drift.width, drift.originalWidth);
            widthAnimator.setDuration(SHRINK_DURATION);
            widthAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            widthAnimator.addUpdateListener(animation -> {
                drift.width = (float) animation.getAnimatedValue();
                invalidate();
            });
            
            ValueAnimator heightAnimator = ValueAnimator.ofFloat(drift.height, drift.originalHeight);
            heightAnimator.setDuration(SHRINK_DURATION);
            heightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            heightAnimator.addUpdateListener(animation -> {
                drift.height = (float) animation.getAnimatedValue();
                invalidate();
            });
            
            shrinkAnimators.add(widthAnimator);
            shrinkAnimators.add(heightAnimator);
        }
        
        // Create animator set for shrinking
        AnimatorSet shrinkSet = new AnimatorSet();
        shrinkSet.playTogether(shrinkAnimators);
        
        // When shrinking is complete, reset the animation state
        shrinkSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isSnowing = false;
                isAnimating = false;
            }
        });
        
        shrinkSet.start();
    }
    
    // Snowdrift class to represent a pile of snow
    private class Snowdrift {
        float x, y;
        float width, height;
        float originalWidth, originalHeight; // Store original size
        
        Snowdrift(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.originalWidth = width;
            this.originalHeight = height;
        }
    }
    
    // Snowflake class for the falling snow animation
    private class Snowflake {
        float x, y;
        float size;
        float speed;
        float drift; // Horizontal drift factor
        boolean hasLanded;
        
        Snowflake(float x, float y, float size, float speed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.drift = (random.nextFloat() - 0.5f) * 2; // Random drift between -1 and 1
            this.hasLanded = false;
        }
        
        void update() {
            y += speed;
            x += drift;
            
            // Check if the snowflake has reached a snowdrift
            for (Snowdrift drift : snowdrifts) {
                // Simple collision check - if snowflake is within the width of a drift
                // and is at or below the top of the drift
                if (Math.abs(x - drift.x) < drift.width && y >= drift.y - drift.height * 0.7f) {
                    hasLanded = true;
                    return;
                }
            }
            
            // Check if the snowflake has reached the snow line (ground)
            if (y >= snowLine) {
                hasLanded = true;
            }
        }
    }
} 