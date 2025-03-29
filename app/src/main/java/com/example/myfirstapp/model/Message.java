package com.example.myfirstapp.model;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String text;
    private String imageUrl;
    private long timestamp;
    private String replyToMessageId;  // ID сообщения, на которое отвечаем
    private boolean edited;
    private boolean reply;
    private Map<String, Boolean> readBy;  // Кто прочитал сообщение

    // Пустой конструктор для Firebase
    public Message() {
    }

    public Message(String messageId, String chatId, String senderId, String text) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.readBy = new HashMap<>();
        this.readBy.put(senderId, true);  // Отправитель автоматически считается прочитавшим
        this.edited = false;
        this.reply = false;
    }
    
    public Message(String messageId, String chatId, String senderId, String text, String imageUrl) {
        this(messageId, chatId, senderId, text);
        this.imageUrl = imageUrl;
    }
    
    public Message(String messageId, String chatId, String senderId, String text, String replyToMessageId, String imageUrl) {
        this(messageId, chatId, senderId, text, imageUrl);
        this.replyToMessageId = replyToMessageId;
        this.reply = true;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getReplyToMessageId() {
        return replyToMessageId;
    }
    
    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
        this.reply = replyToMessageId != null && !replyToMessageId.isEmpty();
    }
    
    public boolean isEdited() {
        return edited;
    }
    
    public void setEdited(boolean edited) {
        this.edited = edited;
    }
    
    public boolean isReply() {
        return reply;
    }
    
    public void setReply(boolean reply) {
        this.reply = reply;
    }
    
    public Map<String, Boolean> getReadBy() {
        if (readBy == null) {
            readBy = new HashMap<>();
        }
        return readBy;
    }
    
    public void setReadBy(Map<String, Boolean> readBy) {
        this.readBy = readBy;
    }
    
    public boolean isReadBy(String userId) {
        return getReadBy().containsKey(userId) && getReadBy().get(userId);
    }
    
    public void markAsReadBy(String userId) {
        getReadBy().put(userId, true);
    }
    
    // Проверяет, прочитано ли сообщение всеми участниками чата
    public boolean isReadByAll(Map<String, Boolean> participants) {
        for (String userId : participants.keySet()) {
            if (!isReadBy(userId)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }
    
    public boolean hasText() {
        return text != null && !text.isEmpty();
    }
} 