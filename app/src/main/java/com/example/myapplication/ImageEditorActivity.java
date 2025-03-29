package com.example.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.databinding.ActivityImageEditorBinding;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ImageEditorActivity extends AppCompatActivity {

    private static final String TAG = "ImageEditorActivity";
    private ActivityImageEditorBinding binding;
    private String photoPath;
    private String editedPhotoPath;
    private float currentBrushSize = 10f;
    private int currentColor = Color.BLACK;
    // Constants for brush size
    private static final float MIN_BRUSH_SIZE = 5f;
    private static final float MAX_BRUSH_SIZE = 50f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        photoPath = getIntent().getStringExtra("photo_path");
        String photoUriString = getIntent().getStringExtra("photo_uri");

        Log.d(TAG, "onCreate: photoPath = " + photoPath);

        if (photoPath != null) {
            // Load the captured image into the drawing view
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            if (bitmap != null) {
                Log.d(TAG, "Loading image from path: " + photoPath + ", dimensions: " + 
                      bitmap.getWidth() + "x" + bitmap.getHeight());
                
                // Check EXIF data to get orientation information
                try {
                    android.media.ExifInterface exif = new android.media.ExifInterface(photoPath);
                    int orientation = exif.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL);
                    
                    // Rotate bitmap based on EXIF orientation
                    Matrix matrix = new Matrix();
                    switch (orientation) {
                        case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                            Log.d(TAG, "EXIF: Rotating 90 degrees");
                            matrix.postRotate(90);
                            break;
                        case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                            Log.d(TAG, "EXIF: Rotating 180 degrees");
                            matrix.postRotate(180);
                            break;
                        case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                            Log.d(TAG, "EXIF: Rotating 270 degrees");
                            matrix.postRotate(270);
                            break;
                    }
                    
                    // Apply rotation if needed
                    if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL) {
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                bitmap.getHeight(), matrix, true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading EXIF data", e);
                }
                
                binding.drawingView.setBackgroundBitmap(bitmap);
            } else {
                Log.e(TAG, "Failed to decode bitmap from path: " + photoPath);
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        } else if (photoUriString != null) {
            try {
                Uri photoUri = Uri.parse(photoUriString);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                Log.d(TAG, "Loading image from URI: " + photoUriString + ", dimensions: " + 
                      bitmap.getWidth() + "x" + bitmap.getHeight());
                
                // Get input stream from URI to read EXIF data
                try (InputStream in = getContentResolver().openInputStream(photoUri)) {
                    if (in != null) {
                        android.media.ExifInterface exif = new android.media.ExifInterface(in);
                        int orientation = exif.getAttributeInt(
                                android.media.ExifInterface.TAG_ORIENTATION,
                                android.media.ExifInterface.ORIENTATION_NORMAL);
                        
                        // Rotate bitmap based on EXIF orientation
                        Matrix matrix = new Matrix();
                        switch (orientation) {
                            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                                Log.d(TAG, "EXIF: Rotating 90 degrees");
                                matrix.postRotate(90);
                                break;
                            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                                Log.d(TAG, "EXIF: Rotating 180 degrees");
                                matrix.postRotate(180);
                                break;
                            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                                Log.d(TAG, "EXIF: Rotating 270 degrees");
                                matrix.postRotate(270);
                                break;
                        }
                        
                        // Apply rotation if needed
                        if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL) {
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                    bitmap.getHeight(), matrix, true);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading EXIF data from URI", e);
                }
                
                binding.drawingView.setBackgroundBitmap(bitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image from URI: " + photoUriString, e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Log.e(TAG, "No image path or URI provided");
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupClickListeners();
        setupModeButtons();
        
        // Set initial brush size
        binding.drawingView.setBrushSize(currentBrushSize);
        
        // Set initial color preview
        updateColorPreview();
    }

    private void setupClickListeners() {
        binding.btnBrush.setOnClickListener(v -> showBrushSizeDialog());
        binding.btnText.setOnClickListener(v -> {
            // Only add text in TEXT_EDIT mode
            if (binding.drawingView.getCurrentMode() == DrawingView.EditMode.TEXT_EDIT) {
                showAddTextDialog();
            } else {
                // Switch to text edit mode first
                binding.drawingView.setEditMode(DrawingView.EditMode.TEXT_EDIT);
                updateModeUI(DrawingView.EditMode.TEXT_EDIT);
                showAddTextDialog();
            }
        });
        binding.btnColor.setOnClickListener(v -> showColorPickerDialog());
        binding.colorPreview.setOnClickListener(v -> showColorPickerDialog());
        binding.btnUndo.setOnClickListener(v -> binding.drawingView.undo());
        binding.btnClear.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Очистить рисунок")
                    .setMessage("Вы уверены что хотите удалить все рисунки и тексты?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        binding.drawingView.clearCanvas();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });
        binding.btnCancel.setOnClickListener(v -> cancelEditing());
        binding.btnSave.setOnClickListener(v -> saveEditedImage());
    }
    
    private void setupModeButtons() {
        binding.btnModeDraw.setOnClickListener(v -> {
            binding.drawingView.setEditMode(DrawingView.EditMode.DRAW);
            updateModeUI(DrawingView.EditMode.DRAW);
        });
        
        binding.btnModeText.setOnClickListener(v -> {
            binding.drawingView.setEditMode(DrawingView.EditMode.TEXT_EDIT);
            updateModeUI(DrawingView.EditMode.TEXT_EDIT);
        });
        
        // Initial mode
        updateModeUI(DrawingView.EditMode.DRAW);
    }
    
    private void updateModeUI(DrawingView.EditMode mode) {
        // Update button backgrounds
        if (mode == DrawingView.EditMode.DRAW) {
            binding.btnModeDraw.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple_500));
            binding.btnModeText.setBackgroundTintList(ContextCompat.getColorStateList(this, com.google.android.material.R.color.design_default_color_secondary));
            
            // Show brush button, hide text button in tool panel
            binding.btnBrush.setVisibility(View.VISIBLE);
            binding.btnText.setVisibility(View.GONE);
        } else {
            binding.btnModeDraw.setBackgroundTintList(ContextCompat.getColorStateList(this, com.google.android.material.R.color.design_default_color_secondary));
            binding.btnModeText.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple_500));
            
            // Hide brush button, show text button in tool panel
            binding.btnBrush.setVisibility(View.GONE);
            binding.btnText.setVisibility(View.VISIBLE);
        }
    }

    private void showBrushSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_brush_size, null);
        SeekBar seekBar = view.findViewById(R.id.seekBar);
        final TextView sizeTextView = view.findViewById(R.id.brushSizeText);
        
        // Set min and max values for better control
        seekBar.setMax((int)(MAX_BRUSH_SIZE - MIN_BRUSH_SIZE));
        
        // Convert current brush size to seekbar progress
        int progress = (int)(currentBrushSize - MIN_BRUSH_SIZE);
        seekBar.setProgress(progress);
        
        // Show initial size
        sizeTextView.setText(String.format(Locale.getDefault(), "%.1f px", currentBrushSize));
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Calculate brush size from progress
                currentBrushSize = MIN_BRUSH_SIZE + progress;
                binding.drawingView.setBrushSize(currentBrushSize);
                
                // Update size text
                sizeTextView.setText(String.format(Locale.getDefault(), "%.1f px", currentBrushSize));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setTitle(R.string.brush)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null);
        
        builder.create().show();
    }

    private void showAddTextDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_text, null);
        EditText editText = view.findViewById(R.id.editText);
        
        builder.setTitle(R.string.add_text)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String text = editText.getText().toString();
                    if (!text.isEmpty()) {
                        // Add text at the center of the canvas
                        float x = binding.drawingView.getWidth() / 2f;
                        float y = binding.drawingView.getHeight() / 2f;
                        binding.drawingView.addText(text, x, y, currentColor, 50f);
                        
                        // Switch to text edit mode
                        binding.drawingView.setEditMode(DrawingView.EditMode.TEXT_EDIT);
                        updateModeUI(DrawingView.EditMode.TEXT_EDIT);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        
        builder.create().show();
    }

    private void showColorPickerDialog() {
        AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(
                this, 
                currentColor, 
                new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // Do nothing
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        currentColor = color;
                        binding.drawingView.setColor(currentColor);
                        updateColorPreview();
                    }
                });
        colorPickerDialog.show();
    }
    
    private void updateColorPreview() {
        // Update color preview
        View colorPreview = findViewById(R.id.colorPreview);
        if (colorPreview != null) {
            // Make circular preview
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(currentColor);
            colorPreview.setBackground(shape);
        }
    }
    
    // Helper method to get a contrast color for text over a background
    private int getContrastColor(int color) {
        double y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    private void cancelEditing() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void saveEditedImage() {
        Bitmap bitmap = binding.drawingView.getBitmap();
        if (bitmap != null) {
            try {
                Log.d(TAG, "Saving edited image, bitmap dimensions: " + 
                      bitmap.getWidth() + "x" + bitmap.getHeight());
                
                File imageFile = createImageFile();
                FileOutputStream outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
                outputStream.close();

                editedPhotoPath = imageFile.getAbsolutePath();
                Log.d(TAG, "Image saved to: " + editedPhotoPath);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("edited_image_path", editedPhotoPath);
                setResult(RESULT_OK, resultIntent);
                finish();
            } catch (IOException e) {
                Log.e(TAG, "Error saving image", e);
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Bitmap to save is null");
            Toast.makeText(this, "Error: no image to save", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "EDITED_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }
} 