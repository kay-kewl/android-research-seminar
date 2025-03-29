package com.example.myfirstapp.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private String status; // Персональный статус "О себе"
    private String photoUrl;
    private boolean online;
    private long lastSeen;
    private boolean showOnlineStatus = true;
    private String currentActivity; // "typing", "choosing_photo", null
    private boolean typing;
    private boolean choosingPhoto;
    private String formattedLastSeen;
    private String statusForChatList;

    // Required empty constructor for Firebase
    public User() {
    }

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.online = false;
        this.lastSeen = System.currentTimeMillis();
        this.status = "";
        this.photoUrl = "";
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isShowOnlineStatus() {
        return showOnlineStatus;
    }

    public void setShowOnlineStatus(boolean showOnlineStatus) {
        this.showOnlineStatus = showOnlineStatus;
    }
    
    public String getCurrentActivity() {
        return currentActivity;
    }
    
    public void setCurrentActivity(String currentActivity) {
        this.currentActivity = currentActivity;
    }
    
    public boolean isTyping() {
        return "typing".equals(currentActivity);
    }
    
    public void setTyping(boolean typing) {
        this.typing = typing;
        if (typing) {
            setCurrentActivity("typing");
        } else if (isChoosingPhoto()) {
            // Keep the current activity as choosing photo
        } else {
            setCurrentActivity(null);
        }
    }
    
    public boolean isChoosingPhoto() {
        return "choosing_photo".equals(currentActivity);
    }
    
    public void setChoosingPhoto(boolean choosingPhoto) {
        this.choosingPhoto = choosingPhoto;
        if (choosingPhoto) {
            setCurrentActivity("choosing_photo");
        } else if (isTyping()) {
            // Keep the current activity as typing
        } else {
            setCurrentActivity(null);
        }
    }

    public String getFormattedLastSeen() {
        if (online) {
            return "В сети";
        }
        
        if (lastSeen == 0) {
            return "Не в сети";
        }
        
        // Получаем календари для текущего времени и времени последнего входа
        Calendar now = Calendar.getInstance();
        Calendar lastSeenTime = Calendar.getInstance();
        lastSeenTime.setTimeInMillis(lastSeen);
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        
        // Проверяем, было ли последнее посещение сегодня
        if (now.get(Calendar.YEAR) == lastSeenTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == lastSeenTime.get(Calendar.DAY_OF_YEAR)) {
            return "был(а) сегодня в " + timeFormat.format(new Date(lastSeen));
        }
        
        // Проверяем, было ли последнее посещение вчера
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.YEAR) == lastSeenTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == lastSeenTime.get(Calendar.DAY_OF_YEAR)) {
            return "был(а) вчера в " + timeFormat.format(new Date(lastSeen));
        }
        
        // Сбрасываем календарь к текущей дате
        now = Calendar.getInstance();
        
        // Если в этом году, но не сегодня и не вчера
        if (now.get(Calendar.YEAR) == lastSeenTime.get(Calendar.YEAR)) {
            SimpleDateFormat monthDayFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
            return "был(а) " + monthDayFormat.format(new Date(lastSeen));
        }
        
        // Если в другом году
        return "был(а) " + dateFormat.format(new Date(lastSeen));
    }
    
    public void setFormattedLastSeen(String formattedLastSeen) {
        this.formattedLastSeen = formattedLastSeen;
    }
    
    public String getStatusForChatList() {
        if (isTyping()) {
            return "печатает...";
        } else if (isChoosingPhoto()) {
            return "выбирает фото...";
        }
        // Don't display online status text anymore - just use icon color
        return "";
    }
    
    public void setStatusForChatList(String statusForChatList) {
        this.statusForChatList = statusForChatList;
    }
} 