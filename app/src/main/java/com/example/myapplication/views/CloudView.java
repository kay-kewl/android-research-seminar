package com.example.myapplication.views;

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
import java.util.List;
import java.util.Random;

public class CloudView extends View {

    private Paint cloudPaint;
    private Paint rainPaint;
    private List<Cloud> clouds;
    private List<RainDrop> raindrops;
    private Random random;
    private int grassLine; // Position where grass starts
    
    // Cloud properties
    private static final int DEFAULT_CLOUD_COLOR = Color.WHITE;
    private static final int RAIN_CLOUD_COLOR = Color.rgb(100, 100, 100); // Darker gray
    private static final float DEFAULT_CLOUD_SCALE = 2.0f;  // Even larger default scale
    private static final float MAX_CLOUD_SCALE = 2.8f;      // Even larger max scale
    
    // Animation durations
    private static final int GROW_DARKEN_DURATION = 1500; // Longer duration for growing and darkening
    private static final int SHRINK_DURATION = 3000; // Longer duration for shrinking
    private static final int LIGHTEN_DURATION = 4000; // Longer duration for lightening
    
    public CloudView(Context context) {
        super(context);
        init();
    }

    public CloudView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CloudView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize Cloud paint
        cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudPaint.setColor(DEFAULT_CLOUD_COLOR);
        cloudPaint.setStyle(Paint.Style.FILL);
        
        // Initialize Rain paint
        rainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rainPaint.setColor(Color.BLUE);
        rainPaint.setStyle(Paint.Style.STROKE);
        rainPaint.setStrokeWidth(3);
        
        // Initialize clouds list
        clouds = new ArrayList<>();
        raindrops = new ArrayList<>();
        random = new Random();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Calculate where grass starts (2/3 of the screen height)
        grassLine = (int)(h * 2.0f / 3.0f);
        
        // Create some clouds when the view size is determined
        if (clouds.isEmpty()) {
            // Create 3 larger clouds spread across and positioned lower
            clouds.add(new Cloud(w * 0.3f, h * 0.3f, DEFAULT_CLOUD_SCALE));  // Larger cloud and lower
            clouds.add(new Cloud(w * 0.6f, h * 0.25f, DEFAULT_CLOUD_SCALE * 1.1f)); // Larger cloud and lower
            clouds.add(new Cloud(w * 0.85f, h * 0.35f, DEFAULT_CLOUD_SCALE * 0.95f)); // Larger cloud and lower
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw all clouds
        for (Cloud cloud : clouds) {
            cloud.draw(canvas);
        }
        
        // Draw raindrops for each cloud
        for (int i = raindrops.size() - 1; i >= 0; i--) {
            RainDrop raindrop = raindrops.get(i);
            raindrop.update();
            raindrop.draw(canvas);
            
            // Remove raindrops that have reached their target position
            if (raindrop.reachedTarget) {
                raindrops.remove(i);
            }
        }
        
        // Generate new raindrops for actively raining clouds
        for (Cloud cloud : clouds) {
            if (cloud.isRaining && random.nextInt(3) == 0) {
                createRaindrop(cloud);
            }
        }
        
        // Request next frame for animation
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if touch is on a cloud
            for (Cloud cloud : clouds) {
                if (cloud.containsPoint(event.getX(), event.getY())) {
                    animateCloud(cloud);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
    
    private void animateCloud(Cloud cloud) {
        // Allow multiple animations to run simultaneously
        // No need to check isAnimating anymore
        
        // Cancel any existing animations for this specific cloud
        if (cloud.animatorSet != null) {
            cloud.animatorSet.cancel();
        }
        
        // Animation sequence:
        // 1. Cloud grows and darkens
        // 2. Rain starts
        // 3. Cloud shrinks back to normal and returns to original color
        
        // First, animate growing and darkening
        ValueAnimator growAnimator = ValueAnimator.ofFloat(cloud.scale, MAX_CLOUD_SCALE);
        growAnimator.setDuration(GROW_DARKEN_DURATION);
        growAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        growAnimator.addUpdateListener(animation -> {
            cloud.scale = (float) animation.getAnimatedValue();
        });
        
        ValueAnimator darkenAnimator = ValueAnimator.ofArgb(DEFAULT_CLOUD_COLOR, RAIN_CLOUD_COLOR);
        darkenAnimator.setDuration(GROW_DARKEN_DURATION);
        darkenAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        darkenAnimator.addUpdateListener(animation -> {
            cloud.color = (int) animation.getAnimatedValue();
        });
        
        // Then, animate shrinking and lightening
        ValueAnimator shrinkAnimator = ValueAnimator.ofFloat(MAX_CLOUD_SCALE, DEFAULT_CLOUD_SCALE);
        shrinkAnimator.setDuration(SHRINK_DURATION);
        shrinkAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        shrinkAnimator.addUpdateListener(animation -> {
            cloud.scale = (float) animation.getAnimatedValue();
            
            // Stop rain at 90% through shrinking (near the end of animation)
            // This ensures the rain continues almost until the cloud returns to its normal size
            if (animation.getAnimatedFraction() > 0.9f && cloud.isRaining) {
                cloud.isRaining = false;
            }
        });
        
        ValueAnimator lightenAnimator = ValueAnimator.ofArgb(RAIN_CLOUD_COLOR, DEFAULT_CLOUD_COLOR);
        lightenAnimator.setDuration(LIGHTEN_DURATION);
        lightenAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        lightenAnimator.addUpdateListener(animation -> {
            cloud.color = (int) animation.getAnimatedValue();
        });
        
        // Create an animator set to sequence the animations
        AnimatorSet growAndDarken = new AnimatorSet();
        growAndDarken.playTogether(growAnimator, darkenAnimator);
        
        AnimatorSet shrinkAndLighten = new AnimatorSet();
        shrinkAndLighten.playTogether(shrinkAnimator, lightenAnimator);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(growAndDarken, shrinkAndLighten);
        cloud.animatorSet = animatorSet;
        
        // Start raining after the cloud darkens and stop before it's fully back to normal
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                cloud.isAnimating = true;
                
                // Rain starts after the cloud has grown and darkened
                growAndDarken.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        cloud.isRaining = true;
                    }
                });
            }
            
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Reset state when everything is finished
                cloud.isAnimating = false;
                // Keep existing raindrops until they reach their targets
            }
        });
        
        animatorSet.start();
    }
    
    private void createRaindrop(Cloud cloud) {
        float x = cloud.x + (random.nextFloat() * cloud.getWidth() - cloud.getWidth() / 2);
        float y = cloud.y + cloud.getHeight() / 2;
        
        // Create a target y position that is somewhere on the grass
        float targetY = grassLine + random.nextFloat() * (getHeight() - grassLine) * 0.3f;
        
        raindrops.add(new RainDrop(x, y, targetY));
    }
    
    // Cloud class to represent a cloud
    private class Cloud {
        float x, y;
        float scale;
        int color;
        boolean isAnimating;
        boolean isRaining;
        AnimatorSet animatorSet;
        
        Cloud(float x, float y, float scale) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.color = DEFAULT_CLOUD_COLOR;
            this.isAnimating = false;
            this.isRaining = false;
            this.animatorSet = null;
        }
        
        void draw(Canvas canvas) {
            cloudPaint.setColor(color);
            
            // Scale and draw the cloud (made of multiple circles)
            float baseRadius = 45 * scale; // Even larger base radius for bigger clouds
            
            // Draw the main body of the cloud (several overlapping circles)
            canvas.drawCircle(x, y, baseRadius, cloudPaint);
            canvas.drawCircle(x - baseRadius, y, baseRadius * 0.7f, cloudPaint);
            canvas.drawCircle(x + baseRadius, y, baseRadius * 0.7f, cloudPaint);
            canvas.drawCircle(x - baseRadius / 2, y - baseRadius / 2, baseRadius * 0.7f, cloudPaint);
            canvas.drawCircle(x + baseRadius / 2, y - baseRadius / 2, baseRadius * 0.7f, cloudPaint);
        }
        
        boolean containsPoint(float px, float py) {
            // Simple circular hit detection
            float radius = getWidth() / 2;
            return Math.sqrt(Math.pow(px - x, 2) + Math.pow(py - y, 2)) <= radius;
        }
        
        float getWidth() {
            return 140 * scale; // Increase width for larger clouds
        }
        
        float getHeight() {
            return 90 * scale; // Increase height for larger clouds
        }
    }
    
    // RainDrop class for rain animation
    private class RainDrop {
        float x, y;
        float length;
        float speed;
        float targetY;
        boolean reachedTarget;
        
        RainDrop(float x, float y, float targetY) {
            this.x = x;
            this.y = y;
            this.length = 15 + random.nextFloat() * 10;
            this.speed = 5 + random.nextFloat() * 5; // Slightly slower rain
            this.targetY = targetY;
            this.reachedTarget = false;
        }
        
        void update() {
            y += speed;
            
            // Check if raindrop has reached target
            if (y + length >= targetY) {
                reachedTarget = true;
            }
        }
        
        void draw(Canvas canvas) {
            canvas.drawLine(x, y, x, Math.min(y + length, targetY), rainPaint);
        }
    }
} 