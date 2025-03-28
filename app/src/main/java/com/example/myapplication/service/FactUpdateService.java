package com.example.myapplication.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;

import com.example.myapplication.data.Fact;
import com.example.myapplication.widget.FactWidgetProvider;

/**
 * Service to handle automatic updates for the widget
 */
public class FactUpdateService extends Service {

    private static final int DAILY_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Schedule updates based on user preference
        scheduleUpdates();
        
        // Update widget now
        updateWidget();
        
        // Service doesn't need to stay running
        stopSelf();
        
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Schedule automatic updates for the widget
     */
    private void scheduleUpdates() {
        // Get user preferences
        SharedPreferences prefs = getSharedPreferences(FactWidgetProvider.PREFS_NAME, MODE_PRIVATE);
        String updateFreq = prefs.getString(FactWidgetProvider.PREF_UPDATE_FREQ_KEY, "on_tap");
        
        // Cancel any existing alarms
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = createUpdatePendingIntent();
        
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            
            // Set new alarm based on frequency
            if ("daily".equals(updateFreq)) {
                alarmManager.setInexactRepeating(
                        AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + DAILY_UPDATE_INTERVAL,
                        DAILY_UPDATE_INTERVAL,
                        pendingIntent
                );
            }
        }
    }

    /**
     * Create PendingIntent for updates
     */
    private PendingIntent createUpdatePendingIntent() {
        Intent intent = new Intent(this, FactWidgetProvider.class);
        intent.setAction(FactWidgetProvider.ACTION_UPDATE_FACT);
        return PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Update widget immediately
     */
    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, FactWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            // Create update intent
            Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.setComponent(widgetComponent);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            
            // Broadcast update
            sendBroadcast(updateIntent);
        }
    }

    /**
     * Start this service (static helper method)
     */
    public static void startService(Context context) {
        Intent serviceIntent = new Intent(context, FactUpdateService.class);
        context.startService(serviceIntent);
    }
} 