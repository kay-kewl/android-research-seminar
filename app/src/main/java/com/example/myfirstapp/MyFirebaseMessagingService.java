package com.example.myfirstapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "chat_messages";
    private static final String CHANNEL_NAME = "Chat Messages";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());
        String currentUserId = FirebaseAuth.getInstance().getUid();
        
        // Skip notification if user is not logged in or sender is current user
        if (currentUserId == null) {
            Log.d(TAG, "User not logged in, skipping notification");
            return;
        }

        // Get the data payload from the message
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Log.d(TAG, "Message data payload: " + data);

            String title = data.get("title");
            String body = data.get("body");
            String chatId = data.get("chatId");
            String senderId = data.get("senderId");
            String recipientId = data.get("recipientId");
            
            // Only show notification if this user is the recipient
            if (recipientId != null && recipientId.equals(currentUserId) && !currentUserId.equals(senderId)) {
                Log.d(TAG, "Sending notification to user: " + recipientId);
                sendNotification(title, body, chatId, senderId);
            } else {
                Log.d(TAG, "Skipping notification: recipientId=" + recipientId + ", currentUserId=" + currentUserId);
            }
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            
            // Get notification data
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            // Always show notification from notification payload
            sendNotification(title, body, null, null);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Store the new token in Firebase Database for this user
        storeTokenInDatabase(token);
    }

    private void storeTokenInDatabase(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User not logged in, can't store the token
            Log.d(TAG, "Cannot store token: user not logged in");
            return;
        }
        
        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance("https://chat-app-a8ac0-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users")
                .child(userId);
        
        userRef.child("fcmToken").setValue(token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token stored successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store FCM token", e));
        
        // Also resubscribe to the user topic
        FirebaseMessaging.getInstance().subscribeToTopic(userId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Resubscribed to topic: " + userId);
                    } else {
                        Log.e(TAG, "Failed to resubscribe to topic", task.getException());
                    }
                });
    }

    private void sendNotification(String title, String messageBody, String chatId, String senderId) {
        if (title == null || messageBody == null) {
            Log.d(TAG, "Skipping notification with null title or body");
            return;
        }
        
        Log.d(TAG, "Preparing notification: " + title + " - " + messageBody);
        
        // Create intent for when notification is tapped
        Intent intent;
        if (chatId != null && senderId != null) {
            // Open specific chat
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chatId", chatId);
            intent.putExtra("recipientId", senderId);
            Log.d(TAG, "Notification will open chat: " + chatId + " with user: " + senderId);
        } else {
            // Open main activity
            intent = new Intent(this, MainActivity.class);
            Log.d(TAG, "Notification will open main activity");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Create pending intent with unique request code based on chatId to allow multiple notifications
        int requestCode = 0;
        if (chatId != null) {
            requestCode = chatId.hashCode();
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // Build notification with improved style
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setVibrate(new long[]{0, 300, 200, 300});

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above with improved settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel");
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            
            channel.setDescription("Уведомления о новых сообщениях");
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Use unique notification ID for each chat to allow separate notifications
        int notificationId = 0;
        if (chatId != null) {
            notificationId = chatId.hashCode();
        }
        
        // Show notification
        Log.d(TAG, "Displaying notification with ID: " + notificationId);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
} 