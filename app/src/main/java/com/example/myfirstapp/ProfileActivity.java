package com.example.myfirstapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.myfirstapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    
    private ImageView ivProfilePicture;
    private EditText etDisplayName, etStatus;
    private Button btnSave;
    private ProgressBar progressBar;
    
    private Uri selectedImageUri = null;
    private String currentPhotoUrl = "";
    private User userProfile;
    
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this)
                            .load(selectedImageUri)
                            .circleCrop()
                            .into(ivProfilePicture);
                }
            });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Инициализация Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Если пользователь не авторизован, перенаправляем на экран входа
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://chat-app-a8ac0-default-rtdb.europe-west1.firebasedatabase.app");
        userRef = database.getReference("users").child(currentUser.getUid());
        
        // Инициализация UI
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Мой профиль");
        }
        
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etDisplayName = findViewById(R.id.etDisplayName);
        etStatus = findViewById(R.id.etStatus);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        
        // Загрузка данных профиля
        loadUserProfile();
        
        // Обработчики событий
        ivProfilePicture.setOnClickListener(v -> openImagePicker());
        btnSave.setOnClickListener(v -> saveProfile());
    }
    
    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userProfile = snapshot.getValue(User.class);
                if (userProfile != null) {
                    // Заполняем поля данными из профиля
                    etDisplayName.setText(userProfile.getDisplayName());
                    
                    if (userProfile.getStatus() != null) {
                        etStatus.setText(userProfile.getStatus());
                    }
                    
                    currentPhotoUrl = userProfile.getPhotoUrl();
                    if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
                        // Если фото в формате Base64, декодируем и отображаем
                        if (currentPhotoUrl.startsWith("data:image")) {
                            try {
                                String base64Image = currentPhotoUrl.split(",")[1];
                                byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                ivProfilePicture.setImageBitmap(decodedBitmap);
                            } catch (Exception e) {
                                ivProfilePicture.setImageResource(R.drawable.default_profile);
                            }
                        } else {
                            // Иначе загружаем по URL
                            Glide.with(ProfileActivity.this)
                                    .load(currentPhotoUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .circleCrop()
                                    .into(ivProfilePicture);
                        }
                    }
                } else {
                    // Если профиль не найден, создаем новый
                    String email = currentUser.getEmail();
                    String displayName = currentUser.getDisplayName();
                    if (TextUtils.isEmpty(displayName)) {
                        displayName = email.substring(0, email.indexOf('@'));
                    }
                    
                    etDisplayName.setText(displayName);
                    
                    userProfile = new User(currentUser.getUid(), email, displayName);
                }
                
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Ошибка загрузки профиля: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
    
    private void saveProfile() {
        String displayName = etDisplayName.getText().toString().trim();
        String status = etStatus.getText().toString().trim();
        
        if (TextUtils.isEmpty(displayName)) {
            etDisplayName.setError("Введите имя пользователя");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        
        // Обновляем данные профиля
        userProfile.setDisplayName(displayName);
        userProfile.setStatus(status);
        
        // Если выбрано новое изображение, обрабатываем его
        if (selectedImageUri != null) {
            try {
                // Получаем и сжимаем изображение
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                bitmap = getResizedBitmap(bitmap, 400); // Ограничиваем размер
                
                // Конвертируем изображение в Base64
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
                String photoUrl = "data:image/jpeg;base64," + base64Image;
                
                userProfile.setPhotoUrl(photoUrl);
            } catch (IOException e) {
                Toast.makeText(this, "Ошибка при обработке изображения", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                return;
            }
        }
        
        // Сохраняем данные в Firebase
        userRef.setValue(userProfile)
                .addOnSuccessListener(aVoid -> {
                    // Обновляем профиль в Firebase Auth
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build();
                    
                    currentUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                progressBar.setVisibility(View.GONE);
                                btnSave.setEnabled(true);
                                
                                if (task.isSuccessful()) {
                                    Toast.makeText(ProfileActivity.this, "Профиль обновлен", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(ProfileActivity.this, "Ошибка обновления профиля", 
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    // Уменьшаем размер изображения для экономии места
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 