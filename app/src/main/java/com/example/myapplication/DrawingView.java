package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private static final String TAG = "DrawingView";
    private final Paint mPaint;
    private final Path mPath;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private Bitmap mBackgroundImage;
    private final List<DrawingAction> drawingActions;
    private final List<DrawingAction> redoActions;

    // Drawing properties
    private int currentColor = Color.BLACK;
    private int mBackgroundColor = Color.WHITE;
    private float mBrushSize = 10f;
    private boolean isEraser = false;

    // Path tracking
    private float mLastTouchX;
    private float mLastTouchY;

    // Text properties
    private final List<TextObject> mTextObjects;
    private TextObject mSelectedTextObject;
    private boolean mIsTextSelected = false;
    private boolean mIsScalingText = false;
    private boolean mIsRotatingText = false;
    private float mTextRotationStartAngle;
    private final int TEXT_HANDLE_SIZE = 40;
    private final int TEXT_PADDING = 20;

    // Edit mode
    public enum EditMode {
        DRAW, TEXT_EDIT
    }
    private EditMode mCurrentMode = EditMode.DRAW;

    public DrawingView(Context context) {
        this(context, null);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPath = new Path();
        mPaint = new Paint();
        setupPaint();

        drawingActions = new ArrayList<>();
        redoActions = new ArrayList<>();
        mTextObjects = new ArrayList<>();
    }

    private void setupPaint() {
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(currentColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mBrushSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        if (mBitmap == null || w != oldw || h != oldh) {
            // Create a new bitmap with the new dimensions
            Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas newCanvas = new Canvas(newBitmap);
            
            // Fill with background color first
            newCanvas.drawColor(mBackgroundColor);
            
            // Draw background image if exists
            if (mBackgroundImage != null) {
                // Scale bitmap to fit the canvas
                Bitmap scaledBg = Bitmap.createScaledBitmap(mBackgroundImage, w, h, true);
                newCanvas.drawBitmap(scaledBg, 0, 0, null);
                
                Log.d(TAG, "Background image drawn in onSizeChanged, dimensions: " + 
                      scaledBg.getWidth() + "x" + scaledBg.getHeight());
            } else {
                Log.d(TAG, "No background image to draw in onSizeChanged");
            }
            
            // Copy old bitmap content if it exists
            if (mBitmap != null) {
                newCanvas.drawBitmap(mBitmap, 0, 0, null);
            }
            
            mBitmap = newBitmap;
            mCanvas = newCanvas;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw the bitmap with all permanent content
        canvas.drawBitmap(mBitmap, 0, 0, null);
        
        // Draw current path being drawn
        if (!isEraser && mCurrentMode == EditMode.DRAW) {
            canvas.drawPath(mPath, mPaint);
        }
        
        // Draw all text objects
        drawAllTextObjects(canvas);
    }
    
    private void drawAllTextObjects(Canvas canvas) {
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        
        for (TextObject textObject : mTextObjects) {
            // Save canvas state for rotation
            canvas.save();
            
            // Calculate text bounds first
            textPaint.setTextSize(textObject.getTextSize());
            float textWidth = textPaint.measureText(textObject.getText());
            Rect textBounds = textObject.getTextBounds();
            float textHeight = textBounds.height();
            
            // Calculate the center of the text for rotation
            float centerX = textObject.getX() + textWidth / 2;
            float centerY = textObject.getY() - textHeight / 2;
            
            // Rotate around this center
            if (textObject.getRotation() != 0) {
                canvas.rotate(textObject.getRotation(), centerX, centerY);
            }
            
            // Set up paint for this text object
            textPaint.setColor(textObject.getColor());
            textPaint.setTypeface(Typeface.DEFAULT);
            
            // Draw the text
            canvas.drawText(textObject.getText(), textObject.getX(), textObject.getY(), textPaint);
            
            // Draw selection handles if this text is selected
            if (mIsTextSelected && mSelectedTextObject == textObject && mCurrentMode == EditMode.TEXT_EDIT) {
                // Calculate text bounds with padding
                RectF bounds = new RectF();
                bounds.left = textObject.getX() - TEXT_PADDING;
                bounds.top = textObject.getY() - textHeight - TEXT_PADDING;
                bounds.right = textObject.getX() + textWidth + TEXT_PADDING;
                bounds.bottom = textObject.getY() + TEXT_PADDING;
                
                Paint selectionPaint = new Paint();
                selectionPaint.setColor(Color.BLUE);
                selectionPaint.setStyle(Paint.Style.STROKE);
                selectionPaint.setStrokeWidth(2);
                
                canvas.drawRect(bounds, selectionPaint);
                
                // Draw combined resize/rotate handle at bottom right corner
                Paint handlePaint = new Paint();
                handlePaint.setColor(Color.BLUE);
                handlePaint.setStyle(Paint.Style.FILL);
                
                canvas.drawCircle(bounds.right, bounds.bottom, TEXT_HANDLE_SIZE/2f, handlePaint);
            }
            
            // Restore canvas to original state
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (mCurrentMode) {
            case DRAW:
                return handleDrawingMode(event, x, y);
            case TEXT_EDIT:
                return handleTextEditMode(event, x, y);
            default:
                return false;
        }
    }
    
    private boolean handleDrawingMode(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                return true;
            default:
                return false;
        }
    }
    
    private boolean handleTextEditMode(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() == 1) {
                    mLastTouchX = x;
                    mLastTouchY = y;
                }
                
                if (mSelectedTextObject != null && mIsTextSelected) {
                    // Calculate text bounds
                    Paint textPaint = new Paint();
                    textPaint.setTextSize(mSelectedTextObject.getTextSize());
                    float textWidth = textPaint.measureText(mSelectedTextObject.getText());
                    Rect textBounds = mSelectedTextObject.getTextBounds();
                    float textHeight = textBounds.height();
                    
                    // Create bounding rectangle with padding
                    RectF bounds = new RectF();
                    bounds.left = mSelectedTextObject.getX() - TEXT_PADDING;
                    bounds.top = mSelectedTextObject.getY() - textHeight - TEXT_PADDING;
                    bounds.right = mSelectedTextObject.getX() + textWidth + TEXT_PADDING;
                    bounds.bottom = mSelectedTextObject.getY() + TEXT_PADDING;
                    
                    // Calculate the center of the text
                    float centerX = mSelectedTextObject.getX() + textWidth / 2;
                    float centerY = mSelectedTextObject.getY() - textHeight / 2;
                    
                    // Need to rotate touch point back if text is rotated to check against handles
                    float[] touchPoint = new float[]{x, y};
                    if (mSelectedTextObject.getRotation() != 0) {
                        Matrix invertMatrix = new Matrix();
                        Matrix matrix = new Matrix();
                        
                        // Apply inverse rotation
                        matrix.setRotate(mSelectedTextObject.getRotation(), centerX, centerY);
                        matrix.invert(invertMatrix);
                        invertMatrix.mapPoints(touchPoint);
                    }
                    
                    // Check if resize/rotate handle was touched
                    RectF resizeHandle = new RectF(
                            bounds.right - TEXT_HANDLE_SIZE/2f,
                            bounds.bottom - TEXT_HANDLE_SIZE/2f,
                            bounds.right + TEXT_HANDLE_SIZE/2f,
                            bounds.bottom + TEXT_HANDLE_SIZE/2f
                    );
                    
                    if (resizeHandle.contains(touchPoint[0], touchPoint[1])) {
                        // Включаем оба режима одновременно
                        mIsRotatingText = true;
                        mIsScalingText = true;
                        mTextRotationStartAngle = (float) Math.toDegrees(Math.atan2(y - centerY, x - centerX));
                        
                        Log.d(TAG, "Starting combined text rotation and scaling");
                        
                        mLastTouchX = x;
                        mLastTouchY = y;
                        return true;
                    }
                }
                
                // Check if any text object was touched
                TextObject touchedText = findTouchedText(x, y);
                if (touchedText != null) {
                    mSelectedTextObject = touchedText;
                    mIsTextSelected = true;
                    mLastTouchX = x;
                    mLastTouchY = y;
                } else {
                    // Deselect if touched outside
                    mIsTextSelected = false;
                    mSelectedTextObject = null;
                }
                invalidate();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (mSelectedTextObject != null && mIsTextSelected) {
                    // Calculate text values for reference
                    Paint textPaint = new Paint();
                    textPaint.setTextSize(mSelectedTextObject.getTextSize());
                    float textWidth = textPaint.measureText(mSelectedTextObject.getText());
                    Rect textBounds = mSelectedTextObject.getTextBounds();
                    float textHeight = textBounds.height();
                    
                    // Calculate center of text for rotation and scaling
                    float centerX = mSelectedTextObject.getX() + textWidth / 2;
                    float centerY = mSelectedTextObject.getY() - textHeight / 2;
                    
                    // Если оба режима включены - обрабатываем вращение и масштабирование
                    if (mIsRotatingText && mIsScalingText) {
                        // 1. Обрабатываем вращение
                        float currentAngle = (float) Math.toDegrees(Math.atan2(y - centerY, x - centerX));
                        float rotationDiff = currentAngle - mTextRotationStartAngle;
                        float newRotation = mSelectedTextObject.getRotation() + rotationDiff;
                        mSelectedTextObject.setRotation(newRotation);
                        mTextRotationStartAngle = currentAngle;
                        
                        // 2. Обрабатываем масштабирование по изменению расстояния от центра текста
                        float dx = x - centerX;
                        float dy = y - centerY;
                        float currentDistance = (float) Math.sqrt(dx * dx + dy * dy);
                        
                        float prevDx = mLastTouchX - centerX;
                        float prevDy = mLastTouchY - centerY;
                        float previousDistance = (float) Math.sqrt(prevDx * prevDx + prevDy * prevDy);
                        
                        if (previousDistance > 0) {
                            float scaleFactor = currentDistance / previousDistance;
                            // Limit scale factor to avoid huge changes
                            scaleFactor = Math.max(0.95f, Math.min(1.05f, scaleFactor));
                            
                            // Apply scale factor with minimum size constraint
                            float newSize = Math.max(20, mSelectedTextObject.getTextSize() * scaleFactor);
                            Log.d(TAG, "Combined mode: scaling text, new size: " + newSize);
                            mSelectedTextObject.setTextSize(newSize);
                        }
                        
                        mLastTouchX = x;
                        mLastTouchY = y;
                    } else if (event.getPointerCount() == 1) {
                        // Move text
                        float dx = x - mLastTouchX;
                        float dy = y - mLastTouchY;
                        mSelectedTextObject.setX(mSelectedTextObject.getX() + dx);
                        mSelectedTextObject.setY(mSelectedTextObject.getY() + dy);
                        mLastTouchX = x;
                        mLastTouchY = y;
                    }
                    invalidate();
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                mIsRotatingText = false;
                mIsScalingText = false;
                invalidate();
                return true;
                
            default:
                return false;
        }
    }
    
    private TextObject findTouchedText(float x, float y) {
        for (TextObject textObject : mTextObjects) {
            // Get text dimensions
            Paint textPaint = new Paint();
            textPaint.setTextSize(textObject.getTextSize());
            
            float textWidth = textPaint.measureText(textObject.getText());
            float textHeight = textObject.getTextBounds().height();
            
            // Create the bounding rectangle with padding
            RectF bounds = new RectF(
                    textObject.getX() - TEXT_PADDING, 
                    textObject.getY() - textHeight - TEXT_PADDING, 
                    textObject.getX() + textWidth + TEXT_PADDING, 
                    textObject.getY() + TEXT_PADDING
            );
            
            // Calculate the center for rotation
            float centerX = textObject.getX() + textWidth / 2;
            float centerY = textObject.getY() - textHeight / 2;
            
            // Create a matrix for the inverse rotation if needed
            if (textObject.getRotation() != 0) {
                // Create rotation matrix
                Matrix rotationMatrix = new Matrix();
                rotationMatrix.setRotate(-textObject.getRotation(), centerX, centerY);
                
                // Transform the touch point
                float[] touchPoint = new float[]{x, y};
                rotationMatrix.mapPoints(touchPoint);
                
                // Check if the transformed point is in the bounds
                if (bounds.contains(touchPoint[0], touchPoint[1])) {
                    return textObject;
                }
            } else {
                // Simple bounds check for non-rotated text
                if (bounds.contains(x, y)) {
                    return textObject;
                }
            }
        }
        
        return null;
    }

    private void touchStart(float x, float y) {
        redoActions.clear();
        mPath.reset();
        mPath.moveTo(x, y);
        mLastTouchX = x;
        mLastTouchY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mLastTouchX);
        float dy = Math.abs(y - mLastTouchY);
        
        if (dx >= 4 || dy >= 4) {
            mPath.quadTo(
                    mLastTouchX,
                    mLastTouchY,
                    (x + mLastTouchX) / 2,
                    (y + mLastTouchY) / 2
            );
            
            mLastTouchX = x;
            mLastTouchY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mLastTouchX, mLastTouchY);
        mCanvas.drawPath(mPath, mPaint);
        
        // Save for undo
        PathAction action = new PathAction(new Path(mPath), new Paint(mPaint));
        drawingActions.add(action);
        
        mPath.reset();
    }

    public void setEditMode(EditMode mode) {
        mCurrentMode = mode;
        invalidate();
    }

    public EditMode getCurrentMode() {
        return mCurrentMode;
    }

    public void setColor(int color) {
        currentColor = color;
        if (!isEraser) {
            mPaint.setColor(color);
        }
        
        // Update selected text color if in text edit mode
        if (mCurrentMode == EditMode.TEXT_EDIT && mSelectedTextObject != null) {
            mSelectedTextObject.setColor(color);
            invalidate();
        }
    }

    public void setBrushSize(float size) {
        mBrushSize = size;
        mPaint.setStrokeWidth(size);
    }

    public void enableEraser(boolean enable) {
        isEraser = enable;
        if (enable) {
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            mPaint.setXfermode(null);
            mPaint.setColor(currentColor);
        }
    }

    public void undo() {
        if (drawingActions.size() > 0) {
            DrawingAction lastAction = drawingActions.remove(drawingActions.size() - 1);
            redoActions.add(lastAction);
            
            // If the action is a TextObject, remove it from text objects list
            if (lastAction instanceof TextObject) {
                mTextObjects.remove(lastAction);
            }
            
            redrawCanvas();
        }
    }

    public void clearCanvas() {
        drawingActions.clear();
        redoActions.clear();
        mTextObjects.clear();
        mSelectedTextObject = null;
        
        if (mCanvas != null) {
            mCanvas.drawColor(mBackgroundColor, PorterDuff.Mode.CLEAR);
            mCanvas.drawColor(mBackgroundColor);
            
            // Redraw background image if exists
            if (mBackgroundImage != null) {
                Bitmap scaledBg = Bitmap.createScaledBitmap(mBackgroundImage, getWidth(), getHeight(), true);
                mCanvas.drawBitmap(scaledBg, 0, 0, null);
                
                Log.d(TAG, "Background image redrawn in clearCanvas");
            } else {
                Log.d(TAG, "No background image to draw in clearCanvas");
            }
        }
        invalidate();
    }

    private void redrawCanvas() {
        mCanvas.drawColor(mBackgroundColor, PorterDuff.Mode.CLEAR);
        mCanvas.drawColor(mBackgroundColor);
        
        // Redraw background image if exists
        if (mBackgroundImage != null) {
            Bitmap scaledBg = Bitmap.createScaledBitmap(mBackgroundImage, getWidth(), getHeight(), true);
            mCanvas.drawBitmap(scaledBg, 0, 0, null);
            
            Log.d(TAG, "Background image redrawn in redrawCanvas");
        } else {
            Log.d(TAG, "No background image to draw in redrawCanvas");
        }
        
        for (DrawingAction action : drawingActions) {
            action.draw(mCanvas);
        }
        
        invalidate();
    }

    public void addText(String text, float x, float y, int color, float textSize) {
        TextObject textObject = new TextObject(text, x, y, color, textSize);
        mTextObjects.add(textObject);
        drawingActions.add(textObject);
        
        // Auto-select new text
        mSelectedTextObject = textObject;
        mIsTextSelected = true;
        mCurrentMode = EditMode.TEXT_EDIT;
        
        invalidate();
    }

    public void setBackgroundBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            // Save background image
            mBackgroundImage = bitmap;
            
            Log.d(TAG, "Background image set, dimensions: " + 
                  bitmap.getWidth() + "x" + bitmap.getHeight());
            
            if (mCanvas != null) {
                // Clear the canvas
                mCanvas.drawColor(mBackgroundColor, PorterDuff.Mode.CLEAR);
                mCanvas.drawColor(mBackgroundColor);
                
                // Просто масштабируем изображение для холста без дополнительных поворотов
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), true);
                mCanvas.drawBitmap(scaledBitmap, 0, 0, null);
                
                Log.d(TAG, "Background image drawn, canvas dimensions: " + 
                      getWidth() + "x" + getHeight());
                
                invalidate();
            } else {
                Log.d(TAG, "Canvas is null when setting background bitmap");
            }
        } else {
            Log.d(TAG, "Attempt to set null background bitmap");
        }
    }

    public Bitmap getBitmap() {
        if (mBitmap == null) {
            Log.e(TAG, "getBitmap(): mBitmap is null");
            return null;
        }
        
        // Create a copy of the bitmap to draw on
        Bitmap resultBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
        Canvas canvas = new Canvas(resultBitmap);
        
        // Draw the existing bitmap first
        canvas.drawBitmap(mBitmap, 0, 0, null);
        
        // Draw all text objects onto the bitmap
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        
        for (TextObject textObject : mTextObjects) {
            // Save canvas state
            canvas.save();
            
            // Calculate text dimensions
            textPaint.setTextSize(textObject.getTextSize());
            float textWidth = textPaint.measureText(textObject.getText());
            Rect textBounds = textObject.getTextBounds();
            float textHeight = textBounds.height();
            
            // Calculate the center of text for rotation
            float centerX = textObject.getX() + textWidth / 2;
            float centerY = textObject.getY() - textHeight / 2;
            
            // Apply rotation if needed
            if (textObject.getRotation() != 0) {
                canvas.rotate(textObject.getRotation(), centerX, centerY);
            }
            
            // Set up paint for this text
            textPaint.setColor(textObject.getColor());
            textPaint.setTypeface(Typeface.DEFAULT);
            
            // Draw the text
            canvas.drawText(textObject.getText(), textObject.getX(), textObject.getY(), textPaint);
            
            // Restore canvas
            canvas.restore();
        }
        
        return resultBitmap;
    }

    // Inner classes for drawing actions
    private interface DrawingAction {
        void draw(Canvas canvas);
    }

    private static class PathAction implements DrawingAction {
        private final Path path;
        private final Paint paint;

        PathAction(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawPath(path, paint);
        }
    }

    public static class TextObject implements DrawingAction {
        private final String text;
        private float x, y;
        private int color;
        private float textSize;
        private float rotation = 0;
        private final Rect textBounds;

        TextObject(String text, float x, float y, int color, float textSize) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.textSize = textSize;
            this.textBounds = new Rect();
            
            // Calculate initial bounds
            Paint paint = new Paint();
            paint.setTextSize(textSize);
            paint.getTextBounds(text, 0, text.length(), this.textBounds);
        }

        public String getText() {
            return text;
        }

        public float getX() {
            return x;
        }
        
        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }
        
        public void setY(float y) {
            this.y = y;
        }

        public int getColor() {
            return color;
        }
        
        public void setColor(int color) {
            this.color = color;
        }

        public float getTextSize() {
            return textSize;
        }
        
        public void setTextSize(float size) {
            this.textSize = size;
            
            // Update bounds
            Paint paint = new Paint();
            paint.setTextSize(size);
            paint.getTextBounds(text, 0, text.length(), this.textBounds);
        }
        
        public float getRotation() {
            return rotation;
        }
        
        public void setRotation(float rotation) {
            // Normalize rotation to 0-360
            this.rotation = (rotation % 360 + 360) % 360;
        }
        
        public Rect getTextBounds() {
            return textBounds;
        }

        @Override
        public void draw(Canvas canvas) {
            Paint textPaint = new Paint();
            textPaint.setColor(color);
            textPaint.setTextSize(textSize);
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setAntiAlias(true);
            
            // Save canvas state
            canvas.save();
            
            // Apply rotation if needed
            if (rotation != 0) {
                // Calculate the center of the text
                float textWidth = textPaint.measureText(text);
                float centerX = x + textWidth / 2;
                float centerY = y - textBounds.height() / 2;
                
                // Rotate around this center
                canvas.rotate(rotation, centerX, centerY);
            }
            
            // Draw the text
            canvas.drawText(text, x, y, textPaint);
            
            // Restore canvas state
            canvas.restore();
        }
    }
} 