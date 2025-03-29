package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.Random;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;

public class SettingsActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "NoteAppPrefs";
    private static final String THEME_MODE = "ThemeMode";
    private static final String APP_COLOR = "AppColor";
    private static final String FONT_SIZE = "FontSize";
    
    private RadioGroup themeRadioGroup;
    private RadioGroup colorRadioGroup;
    private SeekBar fontSizeSeekBar;
    private TextView fontSizeValue;
    private TextView fontPreviewText;
    private MaterialButton saveButton;
    
    private SharedPreferences sharedPreferences;
    
    private int[] colorOptions = {
            R.color.purple_500,
            R.color.ocean_secondary_variant,
            R.color.deep_orange_500,
            R.color.blue_500,
            R.color.green_500,
            R.color.amber_700,
            R.color.red_500
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Загрузка настроек
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Apply font size before content view is set
        int fontSize = sharedPreferences.getInt(FONT_SIZE, 16);
        setFontScale(fontSize);
        
        setContentView(R.layout.activity_settings);
        
        // Настройка тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_settings);
        }
        
        // Apply current app color to the toolbar
        int colorResId = sharedPreferences.getInt(APP_COLOR, R.color.purple_500);
        int color = ContextCompat.getColor(this, colorResId);
        toolbar.setBackgroundColor(color);
        getWindow().setStatusBarColor(color);
        
        // Инициализация компонентов
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        colorRadioGroup = findViewById(R.id.colorRadioGroup);
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        fontSizeValue = findViewById(R.id.fontSizeValue);
        saveButton = findViewById(R.id.btnSaveSettings);
        
        // Находим TextView из layout для примера размера шрифта
        TextView exampleText = findViewById(R.id.font_size_example);
        if (exampleText != null) {
            fontPreviewText = exampleText;
        }
        
        // Загружаем текущие настройки
        loadSettings();
        
        // Настраиваем обработчики событий
        setupListeners();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Ensure settings are consistently applied when returning to the activity
        
        // Apply current app color
        int colorResId = sharedPreferences.getInt(APP_COLOR, R.color.purple_500);
        updateToolbarColor(colorResId);
        
        // Apply current font size
        int fontSize = sharedPreferences.getInt(FONT_SIZE, 16);
        
        // Update the preview text without causing recreate
        if (fontPreviewText != null) {
            fontPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        }
        
        if (fontSizeValue != null) {
            fontSizeValue.setText(fontSize + "sp");
        }
        
        // Ensure font scale is applied
        setFontScale(fontSize);
        updateAllTextSizes();
    }
    
    // Helper method to apply font scale properly
    private void setFontScale(int fontSize) {
        Resources res = getResources();
        Configuration configuration = res.getConfiguration();
        
        // Calculate font scale and apply it to configuration
        float fontScale = (float) fontSize / 16;
        configuration.fontScale = fontScale;
        
        // Update metrics and configuration
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = metrics.density * fontScale;
        
        // Apply changes to resources
        res.updateConfiguration(configuration, metrics);
        
        // Apply to application context for consistency
        getApplicationContext().getResources().updateConfiguration(configuration, metrics);
    }
    
    private void loadSettings() {
        // Загрузка темы
        int themeMode = sharedPreferences.getInt(THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int selectedThemeId;
        
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_YES:
                selectedThemeId = R.id.radioDark;
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                selectedThemeId = R.id.radioLight;
                break;
            default:
                selectedThemeId = R.id.radioSystem;
                break;
        }
        
        themeRadioGroup.check(selectedThemeId);
        
        // Загрузка цветовой схемы
        int colorResId = sharedPreferences.getInt(APP_COLOR, R.color.purple_500);
        int selectedColorId;
        
        if (colorResId == R.color.blue_500) {
            selectedColorId = R.id.radioBlue;
        } else if (colorResId == R.color.purple_500) {
            selectedColorId = R.id.radioPurple;
        } else if (colorResId == R.color.green_500) {
            selectedColorId = R.id.radioGreen;
        } else if (colorResId == R.color.deep_orange_500) {
            selectedColorId = R.id.radioOrange;
        } else {
            selectedColorId = R.id.radioPurple;
        }
        
        colorRadioGroup.check(selectedColorId);
        
        // Загрузка размера шрифта
        int fontSize = sharedPreferences.getInt(FONT_SIZE, 16);
        fontSizeSeekBar.setProgress(fontSize);
        updateFontSizePreview(fontSize);
        
        // Apply the current color to the radio buttons
        applyColorToRadioButtons(colorResId);
        
        // Apply color to save button
        if (saveButton != null) {
            int color = ContextCompat.getColor(this, colorResId);
            saveButton.setBackgroundTintList(ColorStateList.valueOf(color));
            saveButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }
    
    private void setupListeners() {
        // Слушатель для SeekBar размера шрифта
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Just update the preview text and value label
                updateFontSizePreview(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Не требуется реализация
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Apply font size changes only when user stops moving the seekbar
                int progress = seekBar.getProgress();
                updateFontSize(progress);
            }
        });
        
        // Слушатель для группы радиокнопок темы
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // При изменении темы сразу применяем изменения
            int themeMode;
            if (checkedId == R.id.radioDark) {
                themeMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.radioLight) {
                themeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else {
                themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            
            // Сохраняем и применяем тему
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(THEME_MODE, themeMode);
            editor.putBoolean("theme_changed", true);
            editor.apply();
        });
        
        // Слушатель для группы радиокнопок цветовой схемы
        colorRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // При изменении цветовой схемы сразу применяем изменения
            int colorResId;
            if (checkedId == R.id.radioBlue) {
                colorResId = R.color.blue_500;
            } else if (checkedId == R.id.radioPurple) {
                colorResId = R.color.purple_500;
            } else if (checkedId == R.id.radioGreen) {
                colorResId = R.color.green_500;
            } else if (checkedId == R.id.radioOrange) {
                colorResId = R.color.deep_orange_500;
            } else {
                colorResId = R.color.purple_500;
            }
            
            // Сохраняем и применяем цветовую схему
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(APP_COLOR, colorResId);
            editor.putBoolean("color_changed", true);
            editor.apply();
            
            // Обновляем цвета UI элементов
            updateToolbarColor(colorResId);
        });
        
        // Слушатель для кнопки сохранения
        saveButton.setOnClickListener(v -> {
            saveSettings();
            finish();
        });
    }
    
    private void updateFontSizePreview(int sizeSp) {
        // Обновляем текст с размером шрифта
        fontSizeValue.setText(sizeSp + "sp");
        
        // Обновляем превью, если возможно
        if (fontPreviewText != null) {
            fontPreviewText.setTextSize(sizeSp);
        }
    }
    
    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // Сохранение темы
        int themeMode;
        int checkedThemeId = themeRadioGroup.getCheckedRadioButtonId();
        
        if (checkedThemeId == R.id.radioDark) {
            themeMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (checkedThemeId == R.id.radioLight) {
            themeMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        
        editor.putInt(THEME_MODE, themeMode);
        editor.putBoolean("theme_changed", true);
        
        // Сохранение цветовой схемы
        int colorResId;
        int checkedColorId = colorRadioGroup.getCheckedRadioButtonId();
        
        if (checkedColorId == R.id.radioBlue) {
            colorResId = R.color.blue_500;
        } else if (checkedColorId == R.id.radioPurple) {
            colorResId = R.color.purple_500;
        } else if (checkedColorId == R.id.radioGreen) {
            colorResId = R.color.green_500;
        } else if (checkedColorId == R.id.radioOrange) {
            colorResId = R.color.deep_orange_500;
        } else {
            colorResId = R.color.purple_500;
        }
        
        editor.putInt(APP_COLOR, colorResId);
        editor.putBoolean("color_changed", true);
        
        // Сохранение размера шрифта
        int fontSize = fontSizeSeekBar.getProgress();
        editor.putInt(FONT_SIZE, fontSize);
        editor.putBoolean("font_size_changed", true);
        
        // Применяем изменения
        editor.apply();
        
        // Применяем тему и цвета для немедленного эффекта
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        // Устанавливаем цвета тулбара в соответствии с выбранной схемой
        updateToolbarColor(colorResId);
        
        // Устанавливаем размер шрифта
        updateFontSize(fontSize);
        
        // Установка флага необходимости перезапуска основной активности
        boolean needsRestart = true;
        editor.putBoolean("needs_restart", needsRestart);
        editor.apply();
        
        // Устанавливаем результат для обновления настроек
        Intent result = new Intent();
        setResult(RESULT_OK, result);
    }
    
    /**
     * Обновляет цвет тулбара в соответствии с выбранной схемой
     */
    private void updateToolbarColor(int colorResId) {
        int color = ContextCompat.getColor(this, colorResId);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(color);
        }
        
        // Обновляем цвет строки состояния
        getWindow().setStatusBarColor(color);
        
        // Обновляем цвет кнопки сохранения
        if (saveButton != null) {
            saveButton.setBackgroundTintList(ColorStateList.valueOf(color));
            saveButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
        
        // Применяем цвет к радиокнопкам
        applyColorToRadioButtons(colorResId);
    }
    
    /**
     * Применяет цвет темы к радиокнопкам
     */
    private void applyColorToRadioButtons(int colorResId) {
        int color = ContextCompat.getColor(this, colorResId);
        ColorStateList colorStateList = ColorStateList.valueOf(color);
        
        // Находим все RadioButton в обеих группах
        for (int i = 0; i < themeRadioGroup.getChildCount(); i++) {
            View child = themeRadioGroup.getChildAt(i);
            if (child instanceof RadioButton) {
                ((RadioButton) child).setButtonTintList(colorStateList);
            }
        }
        
        for (int i = 0; i < colorRadioGroup.getChildCount(); i++) {
            View child = colorRadioGroup.getChildAt(i);
            if (child instanceof RadioButton) {
                ((RadioButton) child).setButtonTintList(colorStateList);
            }
        }
    }
    
    /**
     * Обновляет размер шрифта в приложении
     */
    private void updateFontSize(int fontSize) {
        // Use the common method for font scale application
        setFontScale(fontSize);
        
        // Update all text views in the current layout to reflect the new font size
        updateAllTextSizes();
        
        // Update the preview text specifically
        if (fontPreviewText != null) {
            fontPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        }
        
        // Set a flag to indicate font size was changed
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("font_size_changed", true);
        editor.apply();
        
        // We need to recreate to properly apply font size changes to all views
        recreate();
    }
    
    /**
     * Обновляет размер шрифта всех текстовых элементов в текущем макете
     */
    private void updateAllTextSizes() {
        // Получаем корневой view
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        updateTextSizesRecursively(rootView);
    }
    
    /**
     * Рекурсивно обновляет размер текста во всех TextView
     */
    private void updateTextSizesRecursively(View view) {
        if (view instanceof TextView) {
            // Получаем текущий размер и устанавливаем его снова, чтобы применить масштаб
            float currentSize = ((TextView) view).getTextSize();
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize);
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                updateTextSizesRecursively(viewGroup.getChildAt(i));
            }
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    public void onBackPressed() {
        finish();
    }
    
    // Экстра-функционал: возможность экспорта/импорта настроек
    private void exportSettings() {
        // Здесь можно добавить функционал экспорта настроек в файл
    }
    
    private void importSettings() {
        // Здесь можно добавить функционал импорта настроек из файла
    }
} 