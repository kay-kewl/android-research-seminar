package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.myapplication.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String currentPhotoPath;
    private Uri photoUri;
    private Bitmap currentPhotoBitmap;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 102;

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    processNewPhoto();
                }
            });

    private final ActivityResultLauncher<Intent> editImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String editedImagePath = result.getData().getStringExtra("edited_image_path");
                    if (editedImagePath != null) {
                        File imageFile = new File(editedImagePath);
                        if (imageFile.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(editedImagePath);
                            updateImagePreview(bitmap);
                            currentPhotoBitmap = bitmap;
                            binding.btnShare.setEnabled(true);
                        }
                    }
                }
            });
            
    private final ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    dispatchTakePictureIntent();
                } else {
                    showPermissionExplanationDialog();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnTakePhoto.setOnClickListener(v -> checkPermissionsAndTakePhoto());
        binding.btnShare.setOnClickListener(v -> shareImage());
        
        // Initially the share button is disabled
        binding.btnShare.setEnabled(false);
    }

    private void checkPermissionsAndTakePhoto() {
        // Check both camera and storage permissions
        String[] requiredPermissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        
        // Check if all permissions are granted
        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (allPermissionsGranted) {
            dispatchTakePictureIntent();
        } else {
            requestMultiplePermissionsLauncher.launch(requiredPermissions);
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.camera_permission_required)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> 
                        checkPermissionsAndTakePhoto())
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // Check if there's a camera app available
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.example.myapplication.fileprovider",
                        photoFile);
                takePictureLauncher.launch(photoUri);
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void processNewPhoto() {
        if (currentPhotoPath != null) {
            // Load the captured image
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (bitmap != null) {
                updateImagePreview(bitmap);
                startImageEditing();
            } else {
                Toast.makeText(this, "Error loading captured image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startImageEditing() {
        Intent intent = new Intent(this, ImageEditorActivity.class);
        intent.putExtra("photo_path", currentPhotoPath);
        editImageLauncher.launch(intent);
    }

    private void updateImagePreview(Bitmap bitmap) {
        binding.imageView.setImageBitmap(bitmap);
    }

    private void shareImage() {
        if (currentPhotoBitmap != null) {
            // Save bitmap to MediaStore
            String imagePath = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    currentPhotoBitmap,
                    "Photo Editor Image",
                    "Image created with Photo Editor app"
            );

            if (imagePath != null) {
                Uri imageUri = Uri.parse(imagePath);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                startActivity(Intent.createChooser(shareIntent, "Share image using"));
            } else {
                Toast.makeText(this, "Error sharing image", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 