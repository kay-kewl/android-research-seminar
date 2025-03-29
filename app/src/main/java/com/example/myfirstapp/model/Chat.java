package com.example.myfirstapp.model;

import java.util.HashMap;
import java.util.Map;

public class Chat {
    private String chatId;
    private Map<String, Boolean> participants;
    private long lastMessageTimestamp;
    private String lastMessageText;
    private String lastMessageSenderId;
    private Map<String, Boolean> archivedBy; // Кем архивирован чат
    private Map<String, Boolean> pinnedBy; // Кем закреплен чат
    private Map<String, Long> unreadCount; // Количество непрочитанных для каждого пользователя
    private String chatPhoto; // URL изображения группового чата (если есть)
    private String chatName; // Название группового чата (если есть)

    // Required empty constructor for Firebase
    public Chat() {
        participants = new HashMap<>();
        archivedBy = new HashMap<>();
        pinnedBy = new HashMap<>();
        unreadCount = new HashMap<>();
    }

    public Chat(String chatId, Map<String, Boolean> participants) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessageTimestamp = System.currentTimeMillis();
        this.archivedBy = new HashMap<>();
        this.pinnedBy = new HashMap<>();
        this.unreadCount = new HashMap<>();
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public Map<String, Boolean> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<String, Boolean> participants) {
        this.participants = participants;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }
    
    public Map<String, Boolean> getArchivedBy() {
        return archivedBy;
    }

    public void setArchivedBy(Map<String, Boolean> archivedBy) {
        this.archivedBy = archivedBy;
    }
    
    public boolean isArchivedBy(String userId) {
        return archivedBy != null && archivedBy.containsKey(userId) && Boolean.TRUE.equals(archivedBy.get(userId));
    }
    
    public void setArchivedBy(String userId, boolean archived) {
        if (archivedBy == null) {
            archivedBy = new HashMap<>();
        }
        if (archived) {
            archivedBy.put(userId, true);
        } else {
            archivedBy.remove(userId);
        }
    }
    
    public Map<String, Boolean> getPinnedBy() {
        return pinnedBy;
    }

    public void setPinnedBy(Map<String, Boolean> pinnedBy) {
        this.pinnedBy = pinnedBy;
    }
    
    public boolean isPinnedBy(String userId) {
        return pinnedBy != null && pinnedBy.containsKey(userId) && Boolean.TRUE.equals(pinnedBy.get(userId));
    }
    
    public void setPinnedBy(String userId, boolean pinned) {
        if (pinnedBy == null) {
            pinnedBy = new HashMap<>();
        }
        if (pinned) {
            pinnedBy.put(userId, true);
        } else {
            pinnedBy.remove(userId);
        }
    }
    
    public Map<String, Long> getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Map<String, Long> unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public long getUnreadCountForUser(String userId) {
        if (unreadCount != null && unreadCount.containsKey(userId)) {
            return unreadCount.get(userId);
        }
        return 0;
    }
    
    public void incrementUnreadCountForUser(String userId) {
        if (unreadCount == null) {
            unreadCount = new HashMap<>();
        }
        long count = getUnreadCountForUser(userId);
        unreadCount.put(userId, count + 1);
    }
    
    public void resetUnreadCountForUser(String userId) {
        if (unreadCount != null) {
            unreadCount.put(userId, 0L);
        }
    }
    
    public String getChatPhoto() {
        return chatPhoto;
    }

    public void setChatPhoto(String chatPhoto) {
        this.chatPhoto = chatPhoto;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }
    
    public boolean isGroupChat() {
        return participants != null && participants.size() > 2;
    }

    public String getOtherUserId(String currentUserId) {
        // Find the other user in the chat
        for (String userId : participants.keySet()) {
            if (!userId.equals(currentUserId)) {
                return userId;
            }
        }
        return null;
    }
} 