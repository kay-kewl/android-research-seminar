package com.example.myapplication.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.example.myapplication.R;
import com.example.myapplication.activities.FactSettingsActivity;
import com.example.myapplication.data.Fact;
import com.example.myapplication.data.FactDatabase;
import com.example.myapplication.service.FactUpdateService;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Implementation of App Widget functionality.
 */
public class FactWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "com.example.myapplication.ACTION_UPDATE_WIDGET";
    public static final String ACTION_SHARE_FACT = "com.example.myapplication.ACTION_SHARE_FACT";
    public static final String PREFS_NAME = "com.example.myapplication.widget.FactWidgetProvider";
    public static final String PREF_CATEGORIES_KEY = "categories";
    public static final String PREF_UPDATE_FREQ_KEY = "update_frequency";
    public static final String PREF_CURRENT_FACT_KEY = "currentFact";
    public static final String PREF_CURRENT_CATEGORY_KEY = "currentCategory";
    public static final String ACTION_UPDATE_FACT = "com.example.myapplication.UPDATE_FACT";

    private static final int HOURLY_UPDATE_INTERVAL = 60 * 60 * 1000; // 1 hour in milliseconds
    private static final int DAILY_UPDATE_INTERVAL = 24 * HOURLY_UPDATE_INTERVAL; // 24 hours in milliseconds

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        
        // Логируем обновление виджетов
        android.util.Log.d("FactWidgetProvider", "onUpdate called for widget IDs: " + java.util.Arrays.toString(appWidgetIds));
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        android.util.Log.d("FactWidgetProvider", "onEnabled called - виджет добавлен");
        
        // Проверяем, является ли context активностью настроек
        if (context instanceof FactSettingsActivity) {
            android.util.Log.d("FactWidgetProvider", "onEnabled вызван из активности настроек");
        }
        
        // Предзаполняем базу данных если нужно
        try {
            FactDatabase db = FactDatabase.getInstance(context);
            if (db != null) {
                android.util.Log.d("FactWidgetProvider", "База данных успешно получена");
            }
        } catch (Exception e) {
            android.util.Log.e("FactWidgetProvider", "Ошибка при получении базы данных", e);
        }
        
        // Настраиваем автоматические обновления, если нужно
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String freqStr = prefs.getString(PREF_UPDATE_FREQ_KEY, "on_tap");
        
        if (!"on_tap".equals(freqStr)) {
            try {
                FactUpdateService.startService(context);
                android.util.Log.d("FactWidgetProvider", "Сервис обновления запущен");
            } catch (Exception e) {
                android.util.Log.e("FactWidgetProvider", "Ошибка запуска сервиса", e);
            }
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        // Last widget removed - stop scheduled updates
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FactUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        // Handle custom actions
        if (ACTION_UPDATE_FACT.equals(intent.getAction())) {
            // Get widget IDs
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new android.content.ComponentName(context, FactWidgetProvider.class));
            
            // Update all widgets
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
            }
        } else if (ACTION_SHARE_FACT.equals(intent.getAction())) {
            // Делимся фактом
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String factText = prefs.getString(PREF_CURRENT_FACT_KEY, "");
            String categoryStr = prefs.getString(PREF_CURRENT_CATEGORY_KEY, Fact.Category.SCIENCE.toString());
            
            if (factText != null && !factText.isEmpty()) {
                // Создаем интент для отправки
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Интересный факт (" + getCategoryDisplayName(context, Fact.Category.valueOf(categoryStr)) + "): " + factText);
                
                // Запускаем выбор приложения для отправки
                Intent chooser = Intent.createChooser(shareIntent, "Поделиться фактом");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
            }
        } else if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(intent.getAction())) {
            // Вызываем onEnabled при активации виджета
            this.onEnabled(context);
            
            // При добавлении виджета, также обновляем его
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, FactWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
            
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                this.onUpdate(context, appWidgetManager, appWidgetIds);
            }
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            // Системное обновление всех виджетов
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
            } else {
                // Если ID не указаны, обновляем все виджеты
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName widgetComponent = new ComponentName(context, FactWidgetProvider.class);
                appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
                
                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    this.onUpdate(context, appWidgetManager, appWidgetIds);
                }
            }
        } else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(intent.getAction())) {
            // Виджет удален с домашнего экрана
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                android.util.Log.d("FactWidgetProvider", "Виджет удален с ID: " + appWidgetId);
                this.onDeleted(context, new int[] { appWidgetId });
            }
        } else if (AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED.equals(intent.getAction())) {
            // Изменились опции виджета (обычно размер)
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Bundle newOptions = intent.getBundleExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
                this.onAppWidgetOptionsChanged(context, AppWidgetManager.getInstance(context), appWidgetId, newOptions);
            }
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        android.util.Log.d("FactWidgetProvider", "onAppWidgetOptionsChanged for widget: " + appWidgetId);
        
        // Обновляем виджет при изменении размеров без запуска сервиса
        updateAppWidget(context, appWidgetManager, appWidgetId);
        
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            android.util.Log.d("FactWidgetProvider", "Updating widget ID: " + appWidgetId);
            
            // Create an Intent to launch the settings activity
            Intent settingsIntent = new Intent(context, FactSettingsActivity.class);
            settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent settingsPendingIntent = PendingIntent.getActivity(context, appWidgetId, settingsIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create an Intent for the next fact button
            Intent nextFactIntent = new Intent(context, FactWidgetProvider.class);
            nextFactIntent.setAction(ACTION_UPDATE_FACT);
            nextFactIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent nextFactPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, nextFactIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Get the current category from preferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> categoryStrings = prefs.getStringSet(PREF_CATEGORIES_KEY, new HashSet<String>());
            
            // Default to ALL if no categories selected
            if (categoryStrings.isEmpty()) {
                categoryStrings = new HashSet<>();
                categoryStrings.add(Fact.Category.ALL.toString());
            }

            // Get a random fact from the database
            Fact fact = null;
            try {
                FactDatabase db = FactDatabase.getInstance(context);
                if (db != null) {
                    fact = getRandomFactFromSelectedCategories(db, categoryStrings);
                    android.util.Log.d("FactWidgetProvider", "Database returned fact: " + (fact != null ? "yes" : "no"));
                } else {
                    android.util.Log.e("FactWidgetProvider", "Database is null");
                }
            } catch (Exception e) {
                android.util.Log.e("FactWidgetProvider", "Error getting fact from database", e);
            }
            
            if (fact == null) {
                // If we couldn't get a fact, create a placeholder
                fact = new Fact();
                fact.setContent("Нажмите, чтобы загрузить факт");
                fact.setCategory(Fact.Category.ALL);
                fact.setSource("App");
                android.util.Log.w("FactWidgetProvider", "Could not get a fact, using placeholder");
            } else {
                // Mark the fact as shown
                try {
                    FactDatabase.getInstance(context).markFactAsShown(fact.getId());
                    // Save the current fact for sharing
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PREF_CURRENT_FACT_KEY, fact.getContent());
                    editor.putString(PREF_CURRENT_CATEGORY_KEY, fact.getCategory().toString());
                    editor.apply();
                    android.util.Log.d("FactWidgetProvider", "Got fact for widget: " + fact.getContent());
                } catch (Exception e) {
                    android.util.Log.e("FactWidgetProvider", "Error marking fact as shown", e);
                }
            }

            // Create an Intent for sharing the fact
            Intent shareIntent = new Intent(context, FactWidgetProvider.class);
            shareIntent.setAction(ACTION_SHARE_FACT);
            shareIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent sharePendingIntent = PendingIntent.getBroadcast(context, appWidgetId + 1000, shareIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Set up the RemoteViews object to use in the widget
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fact_widget);
            
            // Set the text
            views.setTextViewText(R.id.fact_text, fact.getContent());
            
            // Set category badge
            views.setTextViewText(R.id.category_badge, getCategoryDisplayName(context, fact.getCategory()));
            views.setInt(R.id.category_badge, "setBackgroundResource", getCategoryColorResource(fact.getCategory()));
            
            // Set the widget title
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title));
            
            // Set up click listeners
            views.setOnClickPendingIntent(R.id.widget_title, settingsPendingIntent);
            views.setOnClickPendingIntent(R.id.next_button, nextFactPendingIntent);
            views.setOnClickPendingIntent(R.id.share_button, sharePendingIntent);
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
            android.util.Log.d("FactWidgetProvider", "Widget updated successfully");
            
        } catch (Exception e) {
            android.util.Log.e("FactWidgetProvider", "Error updating widget", e);
        }
    }

    private static Fact getRandomFactFromSelectedCategories(FactDatabase db, Set<String> categoryStrings) {
        // Check if ALL category is selected
        if (categoryStrings.contains(Fact.Category.ALL.toString())) {
            return db.getRandomUnshownFact(Fact.Category.ALL);
        }
        
        // Create a list of selected categories
        Set<Fact.Category> selectedCategories = new HashSet<>();
        for (String categoryStr : categoryStrings) {
            try {
                Fact.Category category = Fact.Category.valueOf(categoryStr);
                selectedCategories.add(category);
            } catch (IllegalArgumentException e) {
                // Skip invalid categories
            }
        }
        
        // If no valid categories, default to ALL
        if (selectedCategories.isEmpty()) {
            return db.getRandomUnshownFact(Fact.Category.ALL);
        }
        
        // Pick a random category from the selected ones
        Fact.Category[] categoriesArray = selectedCategories.toArray(new Fact.Category[0]);
        Random random = new Random();
        Fact.Category randomCategory = categoriesArray[random.nextInt(categoriesArray.length)];
        
        // Get a random fact from the selected category
        return db.getRandomUnshownFact(randomCategory);
    }

    private static int getCategoryColorResource(Fact.Category category) {
        switch (category) {
            case SCIENCE:
                return R.color.science_category;
            case HISTORY:
                return R.color.history_category;
            case ANIMALS:
                return R.color.animal_category;
            case GEOGRAPHY:
                return R.color.geography_category;
            case PHYSICS:
                return R.color.physics_category;
            case ASTRONOMY:
                return R.color.astronomy_category;
            case ECONOMICS:
                return R.color.economics_category;
            case LINGUISTICS:
                return R.color.linguistics_category;
            case MUSIC:
                return R.color.music_category;
            case LITERATURE:
                return R.color.literature_category;
            case ALL:
            default:
                return R.color.all_category;
        }
    }
    
    private static String getCategoryDisplayName(Context context, Fact.Category category) {
        try {
            int stringId;
            switch (category) {
                case SCIENCE:
                    stringId = R.string.category_science;
                    break;
                case HISTORY:
                    stringId = R.string.category_history;
                    break;
                case ANIMALS:
                    stringId = R.string.category_animals;
                    break;
                case GEOGRAPHY:
                    stringId = R.string.category_geography;
                    break;
                case PHYSICS:
                    stringId = R.string.category_physics;
                    break;
                case ASTRONOMY:
                    stringId = R.string.category_astronomy;
                    break;
                case ECONOMICS:
                    stringId = R.string.category_economics;
                    break;
                case LINGUISTICS:
                    stringId = R.string.category_linguistics;
                    break;
                case MUSIC:
                    stringId = R.string.category_music;
                    break;
                case LITERATURE:
                    stringId = R.string.category_literature;
                    break;
                case ALL:
                    stringId = R.string.category_all;
                    break;
                default:
                    stringId = R.string.category_science;
                    break;
            }
            return context.getString(stringId);
        } catch (Exception e) {
            return category.toString();
        }
    }
} 