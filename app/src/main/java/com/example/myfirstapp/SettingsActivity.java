package com.example.myfirstapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchNotifications;
    private SwitchCompat switchDarkMode;
    private SwitchCompat switchReadReceipts;
    private SwitchCompat switchOnlineStatus;
    
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Инициализация Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://chat-app-a8ac0-default-rtdb.europe-west1.firebasedatabase.app");
            userRef = database.getReference("users").child(currentUser.getUid());
        }
        
        // Инициализация SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Настройка тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Настройки");
        }
        
        // Инициализация UI компонентов
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchReadReceipts = findViewById(R.id.switchReadReceipts);
        switchOnlineStatus = findViewById(R.id.switchOnlineStatus);
        
        // Загрузка текущих настроек
        loadSettings();
        
        // Установка слушателей для изменения настроек
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSettings(isChecked);
        });
        
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveDarkModeSettings(isChecked);
            updateDarkMode(isChecked);
        });
        
        switchReadReceipts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveReadReceiptsSettings(isChecked);
        });
        
        switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveOnlineStatusSettings(isChecked);
        });
        
        // Дополнительные кнопки/опции
        findViewById(R.id.btnClearHistory).setOnClickListener(v -> confirmClearHistory());
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> confirmDeleteAccount());
        findViewById(R.id.btnAbout).setOnClickListener(v -> showAboutDialog());
    }
    
    private void loadSettings() {
        // Загружаем настройки из SharedPreferences
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        boolean darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false);
        boolean readReceiptsEnabled = prefs.getBoolean("read_receipts_enabled", true);
        boolean onlineStatusEnabled = prefs.getBoolean("online_status_enabled", true);
        
        // Устанавливаем значения переключателей
        switchNotifications.setChecked(notificationsEnabled);
        switchDarkMode.setChecked(darkModeEnabled);
        switchReadReceipts.setChecked(readReceiptsEnabled);
        switchOnlineStatus.setChecked(onlineStatusEnabled);
    }
    
    private void saveNotificationSettings(boolean enabled) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply();
        String message = enabled ? "Уведомления включены" : "Уведомления отключены";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void saveDarkModeSettings(boolean enabled) {
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply();
    }
    
    private void updateDarkMode(boolean darkModeEnabled) {
        // Применяем темную тему, если включена
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        // Не пересоздаем активити сразу, так как это прервет переход
        // Вместо этого показываем сообщение о необходимости перезапуска приложения
        Toast.makeText(this, "Изменения темы вступят в силу после перезапуска приложения", Toast.LENGTH_LONG).show();
    }
    
    private void saveReadReceiptsSettings(boolean enabled) {
        prefs.edit().putBoolean("read_receipts_enabled", enabled).apply();
        String message = enabled ? "Отчеты о прочтении включены" : "Отчеты о прочтении отключены";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void saveOnlineStatusSettings(boolean enabled) {
        prefs.edit().putBoolean("online_status_enabled", enabled).apply();
        
        // Обновляем значение в базе данных Firebase
        if (currentUser != null) {
            // Если отключено показывать статус, устанавливаем online=false
            if (!enabled) {
                userRef.child("online").setValue(false);
            }
            
            // Сохраняем настройку приватности статуса
            userRef.child("showOnlineStatus").setValue(enabled);
        }
        
        String message = enabled ? "Онлайн статус включен" : "Онлайн статус скрыт";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void confirmClearHistory() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Очистить историю чатов")
                .setMessage("Вы уверены, что хотите очистить историю всех чатов? Это действие нельзя отменить.")
                .setPositiveButton("Очистить", (dialog, which) -> {
                    // TODO: Реализовать очистку истории чатов для текущего пользователя
                    Toast.makeText(this, "Функция пока не реализована", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    private void confirmDeleteAccount() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удалить аккаунт")
                .setMessage("Вы уверены, что хотите безвозвратно удалить свой аккаунт? Все данные будут потеряны.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    // TODO: Реализовать удаление аккаунта
                    Toast.makeText(this, "Функция пока не реализована", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("О приложении")
                .setMessage("Чат-приложение\nВерсия 1.0\n\nРазработано с использованием Firebase")
                .setPositiveButton("OK", null)
                .show();
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