package com.example.myfirstapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "daily_affirmations_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "MainActivity";
    
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Создаем канал уведомлений при запуске
        createNotificationChannel();
        
        // Инициализация лаунчера для запроса разрешения
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), 
            isGranted -> {
                if (isGranted) {
                    sendNotification();
                } else {
                    Toast.makeText(this, "Разрешение на отправку уведомлений необходимо", Toast.LENGTH_SHORT).show();
                }
            }
        );

        Button sendNotificationButton = findViewById(R.id.send_notification_button);
        sendNotificationButton.setOnClickListener(v -> {
            // Проверка и запрос разрешения, если нужно
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) 
                        == PackageManager.PERMISSION_GRANTED) {
                    sendNotification();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                // Для версий Android до 13 разрешение не требуется
                sendNotification();
            }
        });
    }
    
    private void playNotificationSound() {
        Log.d(TAG, "Playing notification sound...");
        try {
            // Если player уже создан, освободим ресурсы
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            // Используем наш кастомный звук уведомления
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound);
            mediaPlayer = MediaPlayer.create(this, soundUri);
            
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    Log.d(TAG, "Sound playback completed");
                });
                
                // Устанавливаем громкость на максимум
                mediaPlayer.setVolume(1.0f, 1.0f);
                
                // Начинаем воспроизведение
                mediaPlayer.start();
                Log.d(TAG, "Media player started");
                return;
            } else {
                Log.e(TAG, "Failed to create media player");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            // Высокая важность для гарантированного проигрывания звука
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            // Используем наш кастомный звук уведомления
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound);
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            
            channel.setSound(soundUri, audioAttributes);
            channel.enableVibration(true);
            channel.enableLights(true);
            
            // Получаем NotificationManager и регистрируем канал
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created with sound: " + soundUri);
            }
        }
    }

    private void sendNotification() {
        // Получение случайной аффирмации
        String affirmation = getRandomAffirmation();
        
        // Используем наш кастомный звук уведомления
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound);
        Log.d(TAG, "Using sound URI: " + soundUri);
        
        // Создание уведомления с высоким приоритетом и звуком
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(affirmation)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Наивысший приоритет
                .setSound(soundUri)                           // Установка звука
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)  // Только вибрация, без системного звука
                .setAutoCancel(true)                          // Авто-удаление после нажатия
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Видимо на экране блокировки

        // Отправка уведомления
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notification sent with sound");
            
            // Воспроизведем звук напрямую через MediaPlayer
            playNotificationSound();
            
            Toast.makeText(this, "Аффирмация отправлена!", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e(TAG, "Error sending notification", e);
            Toast.makeText(this, "Ошибка при отправке уведомления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getRandomAffirmation() {
        String[] affirmations = {
            "Я достоин всего самого лучшего!",
            "Каждый день я становлюсь лучше!",
            "Я люблю и принимаю себя таким, какой я есть!",
            "Я привлекаю успех в свою жизнь!",
            "Я излучаю уверенность и силу!",
            "Я благодарен за всё, что у меня есть!",
            "Я верю в себя и свои возможности!",
            "Моя жизнь наполнена радостью и благополучием!",
            "Я смелый и решительный человек!",
            "Всё, что я делаю, ведёт к успеху!"
        };
        
        int randomIndex = (int) (Math.random() * affirmations.length);
        return affirmations[randomIndex];
    }
    
    @Override
    protected void onDestroy() {
        // Освобождаем ресурсы MediaPlayer при закрытии активности
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
} 