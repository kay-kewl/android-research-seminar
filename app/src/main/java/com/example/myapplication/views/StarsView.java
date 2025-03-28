package com.example.myapplication.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class StarsView extends View {

    private Paint starPaint;
    private List<Star> stars;
    private Random random;
    private int snowLine; // Position above which stars should appear
    
    // Star colors - golden yellow
    private static final int STAR_COLOR_BRIGHT = Color.rgb(255, 215, 0); // Gold
    private static final int STAR_COLOR_MEDIUM = Color.rgb(255, 223, 80); // Light gold
    private static final int STAR_COLOR_PALE = Color.rgb(255, 236, 139); // Pale gold
    
    private static final int MAX_STARS = 30; // Maximum number of stars on screen
    private static final int MIN_STARS_PER_TAP = 10; // Minimum number of stars to add per tap
    private static final int MAX_STARS_PER_TAP = 20; // Maximum number of stars to add per tap
    private static final int APPEAR_DURATION = 800; // Duration for stars to appear
    private static final int DISAPPEAR_DURATION = 600; // Duration for stars to disappear
    
    public StarsView(Context context) {
        super(context);
        init();
    }

    public StarsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StarsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize Star paint with golden color
        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(STAR_COLOR_BRIGHT);
        starPaint.setStyle(Paint.Style.FILL);
        
        // Initialize stars list and random
        stars = new ArrayList<>();
        random = new Random();
    }

    public void setSnowLine(int snowLine) {
        this.snowLine = snowLine;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw all stars
        for (Star star : stars) {
            // Set color based on star brightness
            starPaint.setColor(star.color);
            starPaint.setAlpha((int)(255 * star.alpha));
            canvas.drawCircle(star.x, star.y, star.size, starPaint);
            
            // Optionally draw additional shapes to make stars twinkle/shine
            if (star.alpha > 0.7f) {
                // Draw a simple "shine" effect for bright stars
                float outerSize = star.size * 1.5f;
                starPaint.setAlpha((int)(100 * star.alpha));
                canvas.drawCircle(star.x, star.y, outerSize, starPaint);
                
                // Draw small "rays" from the star
                starPaint.setAlpha((int)(150 * star.alpha));
                float rayLength = star.size * 2;
                canvas.drawLine(star.x - rayLength, star.y, star.x + rayLength, star.y, starPaint);
                canvas.drawLine(star.x, star.y - rayLength, star.x, star.y + rayLength, starPaint);
            }
        }
        
        // Continue animation if any stars are appearing or disappearing
        boolean needsInvalidate = false;
        for (Star star : stars) {
            if (star.isAnimating) {
                needsInvalidate = true;
                break;
            }
        }
        
        if (needsInvalidate) {
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Only respond to touches in the sky area (above snow line)
            if (event.getY() < snowLine) {
                // Make all existing stars disappear
                makeAllStarsDisappear();
                
                // Create new stars across the entire sky
                createStarsAcrossSky();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
    
    private void makeAllStarsDisappear() {
        // Make all existing stars disappear
        for (Star star : new ArrayList<>(stars)) {
            if (!star.isDisappearing) {
                animateStarDisappear(star);
            }
        }
    }
    
    private void createStarsAcrossSky() {
        // Generate a random number of stars between MIN and MAX
        int starCount = MIN_STARS_PER_TAP + random.nextInt(MAX_STARS_PER_TAP - MIN_STARS_PER_TAP + 1);
        
        // Create stars across the entire sky
        for (int i = 0; i < starCount; i++) {
            // Random position across the entire width and above snow line
            float x = random.nextFloat() * getWidth();
            float y = random.nextFloat() * (snowLine * 0.9f); // Keep slightly below the top edge
            
            // Create a new star and animate it appearing
            Star star = new Star(x, y);
            stars.add(star);
            animateStarAppear(star);
        }
        
        invalidate();
    }
    
    private void removeSomeStars() {
        // Find non-animating stars to remove
        for (Iterator<Star> iterator = stars.iterator(); iterator.hasNext();) {
            Star star = iterator.next();
            if (!star.isAnimating) {
                iterator.remove();
                break;
            }
        }
    }
    
    private void animateStarAppear(Star star) {
        star.isAnimating = true;
        star.isDisappearing = false;
        
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(APPEAR_DURATION);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            star.alpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                star.isAnimating = false;
            }
        });
        
        animator.start();
    }
    
    private void animateStarDisappear(Star star) {
        star.isAnimating = true;
        star.isDisappearing = true;
        
        ValueAnimator animator = ValueAnimator.ofFloat(star.alpha, 0f);
        animator.setDuration(DISAPPEAR_DURATION);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            star.alpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                star.isAnimating = false;
                // Remove the star from the list
                stars.remove(star);
            }
        });
        
        animator.start();
    }
    
    // Star class to represent a star in the sky
    private class Star {
        float x, y;
        float size;
        float alpha; // For fading in/out
        boolean isAnimating;
        boolean isDisappearing;
        int color; // Star color
        
        Star(float x, float y) {
            this.x = x;
            this.y = y;
            this.size = 2f + random.nextFloat() * 3f; // Random size 2-5
            this.alpha = 0f; // Start invisible
            this.isAnimating = false;
            this.isDisappearing = false;
            
            // Assign a random yellow/gold shade
            int colorChoice = random.nextInt(3);
            if (colorChoice == 0) {
                this.color = STAR_COLOR_BRIGHT;
            } else if (colorChoice == 1) {
                this.color = STAR_COLOR_MEDIUM;
            } else {
                this.color = STAR_COLOR_PALE;
            }
        }
    }
} 