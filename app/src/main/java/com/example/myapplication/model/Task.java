package com.example.myapplication.model;

import java.util.UUID;

public class Task {
    private String id;
    private String title;
    private String description;
    private int priority; // 1-3 where 3 is highest
    private boolean completed;
    private long createdAt;

    public Task() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.priority = 1; // Default priority
        this.completed = false;
    }

    public Task(String title, String description, int priority) {
        this();
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }
} 