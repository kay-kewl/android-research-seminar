package com.example.myapplication.activities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.Fact;
import com.example.myapplication.widget.FactWidgetProvider;
import com.example.myapplication.service.FactUpdateService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FactSettingsActivity extends AppCompatActivity {

    private CheckBox checkAll;
    private CheckBox checkScience;
    private CheckBox checkHistory;
    private CheckBox checkAnimals;
    private CheckBox checkGeography;
    private CheckBox checkPhysics;
    private CheckBox checkAstronomy;
    private CheckBox checkEconomics;
    private CheckBox checkLinguistics;
    private CheckBox checkMusic;
    private CheckBox checkLiterature;
    private List<CheckBox> categoryCheckboxes = new ArrayList<>();
    
    private RadioGroup updateFrequencyGroup;
    private Button saveButton;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fact_settings);
        
        // Установить RESULT_CANCELED по умолчанию (на случай, если пользователь закроет активность)
        setResult(RESULT_CANCELED);
        
        // Получить ID виджета, если передан
        Intent intent = getIntent();
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Initialize views
        initializeViews();
        
        // Set up "All Categories" checkbox behavior
        setupAllCategoriesCheckbox();

        // Load current settings
        loadSettings();

        // Set up save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                updateWidget();
                Toast.makeText(FactSettingsActivity.this, "Settings saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void initializeViews() {
        // Initialize checkboxes
        checkAll = findViewById(R.id.check_all);
        checkScience = findViewById(R.id.check_science);
        checkHistory = findViewById(R.id.check_history);
        checkAnimals = findViewById(R.id.check_animals);
        checkGeography = findViewById(R.id.check_geography);
        checkPhysics = findViewById(R.id.check_physics);
        checkAstronomy = findViewById(R.id.check_astronomy);
        checkEconomics = findViewById(R.id.check_economics);
        checkLinguistics = findViewById(R.id.check_linguistics);
        checkMusic = findViewById(R.id.check_music);
        checkLiterature = findViewById(R.id.check_literature);
        
        // Add all category checkboxes to the list (excluding 'All')
        categoryCheckboxes.add(checkScience);
        categoryCheckboxes.add(checkHistory);
        categoryCheckboxes.add(checkAnimals);
        categoryCheckboxes.add(checkGeography);
        categoryCheckboxes.add(checkPhysics);
        categoryCheckboxes.add(checkAstronomy);
        categoryCheckboxes.add(checkEconomics);
        categoryCheckboxes.add(checkLinguistics);
        categoryCheckboxes.add(checkMusic);
        categoryCheckboxes.add(checkLiterature);
        
        // Initialize other views
        updateFrequencyGroup = findViewById(R.id.update_frequency_group);
        saveButton = findViewById(R.id.save_button);
    }
    
    private void setupAllCategoriesCheckbox() {
        // Set up All Categories checkbox to check/uncheck all categories
        checkAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (CheckBox checkBox : categoryCheckboxes) {
                    checkBox.setChecked(isChecked);
                    checkBox.setEnabled(!isChecked);
                }
            }
        });
        
        // Set up individual category checkboxes to update "All" status
        CompoundButton.OnCheckedChangeListener categoryListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    // If any category is unchecked, "All" should be unchecked
                    if (checkAll.isChecked()) {
                        checkAll.setChecked(false);
                    }
                } else {
                    // Check if all categories are checked
                    boolean allChecked = true;
                    for (CheckBox checkBox : categoryCheckboxes) {
                        if (!checkBox.isChecked()) {
                            allChecked = false;
                            break;
                        }
                    }
                    
                    // If all categories are checked, check "All"
                    if (allChecked) {
                        checkAll.setChecked(true);
                    }
                }
            }
        };
        
        // Apply the listener to all category checkboxes
        for (CheckBox checkBox : categoryCheckboxes) {
            checkBox.setOnCheckedChangeListener(categoryListener);
        }
    }

    private void loadSettings() {
        // Загружаем сохраненные настройки
        SharedPreferences prefs = getSharedPreferences(FactWidgetProvider.PREFS_NAME, MODE_PRIVATE);
        
        // Load selected categories
        Set<String> selectedCategories = prefs.getStringSet(FactWidgetProvider.PREF_CATEGORIES_KEY, null);
        
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            // Default to all categories if no selection
            checkAll.setChecked(true);
        } else if (selectedCategories.contains(Fact.Category.ALL.toString())) {
            // "All categories" was selected
            checkAll.setChecked(true);
        } else {
            // Handle individual categories
            for (String category : selectedCategories) {
                try {
                    Fact.Category factCategory = Fact.Category.valueOf(category);
                    setCheckboxForCategory(factCategory, true);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid categories
                }
            }
        }
        
        // Загружаем частоту обновления
        String freqStr = prefs.getString(FactWidgetProvider.PREF_UPDATE_FREQ_KEY, "on_tap");
        switch (freqStr) {
            case "daily":
                updateFrequencyGroup.check(R.id.radio_daily);
                break;
            default:
                updateFrequencyGroup.check(R.id.radio_on_tap);
                break;
        }
    }
    
    private void setCheckboxForCategory(Fact.Category category, boolean checked) {
        switch (category) {
            case SCIENCE:
                checkScience.setChecked(checked);
                break;
            case HISTORY:
                checkHistory.setChecked(checked);
                break;
            case ANIMALS:
                checkAnimals.setChecked(checked);
                break;
            case GEOGRAPHY:
                checkGeography.setChecked(checked);
                break;
            case PHYSICS:
                checkPhysics.setChecked(checked);
                break;
            case ASTRONOMY:
                checkAstronomy.setChecked(checked);
                break;
            case ECONOMICS:
                checkEconomics.setChecked(checked);
                break;
            case LINGUISTICS:
                checkLinguistics.setChecked(checked);
                break;
            case MUSIC:
                checkMusic.setChecked(checked);
                break;
            case LITERATURE:
                checkLiterature.setChecked(checked);
                break;
        }
    }

    private void saveSettings() {
        // Сохраняем настройки
        SharedPreferences prefs = getSharedPreferences(FactWidgetProvider.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Save selected categories
        Set<String> selectedCategories = new HashSet<>();
        
        if (checkAll.isChecked()) {
            // If "All Categories" is checked, just store ALL
            selectedCategories.add(Fact.Category.ALL.toString());
        } else {
            // Otherwise, store individual selected categories
            if (checkScience.isChecked()) selectedCategories.add(Fact.Category.SCIENCE.toString());
            if (checkHistory.isChecked()) selectedCategories.add(Fact.Category.HISTORY.toString());
            if (checkAnimals.isChecked()) selectedCategories.add(Fact.Category.ANIMALS.toString());
            if (checkGeography.isChecked()) selectedCategories.add(Fact.Category.GEOGRAPHY.toString());
            if (checkPhysics.isChecked()) selectedCategories.add(Fact.Category.PHYSICS.toString());
            if (checkAstronomy.isChecked()) selectedCategories.add(Fact.Category.ASTRONOMY.toString());
            if (checkEconomics.isChecked()) selectedCategories.add(Fact.Category.ECONOMICS.toString());
            if (checkLinguistics.isChecked()) selectedCategories.add(Fact.Category.LINGUISTICS.toString());
            if (checkMusic.isChecked()) selectedCategories.add(Fact.Category.MUSIC.toString());
            if (checkLiterature.isChecked()) selectedCategories.add(Fact.Category.LITERATURE.toString());
            
            // If nothing is selected, default to ALL
            if (selectedCategories.isEmpty()) {
                selectedCategories.add(Fact.Category.ALL.toString());
            }
        }
        
        // Save selected categories
        editor.putStringSet(FactWidgetProvider.PREF_CATEGORIES_KEY, selectedCategories);
        
        // Сохраняем частоту обновления
        String updateFreq = getSelectedFrequency();
        editor.putString(FactWidgetProvider.PREF_UPDATE_FREQ_KEY, updateFreq);
        
        // Применяем изменения
        editor.apply();
        
        // Обновляем виджет
        updateWidget();
        
        // Если активность была запущена как конфигуратор виджета
        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    }

    private void updateWidget() {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
            ComponentName thisWidget = new ComponentName(getApplicationContext(), FactWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                // Запускаем сервис для обработки обновления в фоне
                FactUpdateService.startService(this);
                
                // Обновляем виджет через провайдер
                Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.setClass(this, FactWidgetProvider.class);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                sendBroadcast(updateIntent);
                
                // Показываем Toast с сообщением
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            } else {
                // Если мы здесь и есть ID виджета в intent, но не в менеджере,
                // попробуем обновить один конкретный виджет
                if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Обновляем только указанный виджет
                    Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    updateIntent.setClass(this, FactWidgetProvider.class);
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId});
                    sendBroadcast(updateIntent);
                }
            }
        } catch (Exception e) {
            // Error handling quietly
        }
    }

    // Получение выбранной частоты обновления из RadioGroup
    private String getSelectedFrequency() {
        int selectedFrequencyId = updateFrequencyGroup.getCheckedRadioButtonId();
        if (selectedFrequencyId == R.id.radio_daily) {
            return "daily";
        } else {
            return "on_tap"; // По умолчанию
        }
    }
} 