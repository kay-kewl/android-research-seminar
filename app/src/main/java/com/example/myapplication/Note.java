package com.example.myapplication;

import androidx.annotation.NonNull;
import java.util.Date;

/**
 * Модель данных заметки.
 */
public class Note {
    
    private long id;
    private String title;
    private String content;
    private String category;
    private Date dateCreated;
    private Date dateModified;
    
    /**
     * Конструктор по умолчанию
     */
    public Note() {
        this.title = "";
        this.content = "";
        this.category = "";
        this.dateCreated = new Date();
        this.dateModified = new Date();
    }
    
    /**
     * Конструктор с параметрами для новой заметки
     * 
     * @param title Заголовок заметки
     * @param content Содержание заметки
     * @param category Категория заметки
     */
    public Note(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.dateCreated = new Date();
        this.dateModified = new Date();
    }
    
    /**
     * Конструктор с параметрами для существующей заметки
     * 
     * @param id ID заметки
     * @param title Заголовок заметки
     * @param content Содержание заметки
     * @param category Категория заметки
     * @param dateCreated Дата создания
     * @param dateModified Дата изменения
     */
    public Note(long id, String title, String content, String category, 
                Date dateCreated, Date dateModified) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
    }
    
    // Геттеры и сеттеры
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Date getDateCreated() {
        return dateCreated;
    }
    
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    public Date getDateModified() {
        return dateModified;
    }
    
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }
    
    /**
     * Обновляет время модификации записи текущим временем
     */
    public void updateModifiedDate() {
        this.dateModified = new Date();
    }
    
    @NonNull
    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", dateCreated=" + dateCreated +
                ", dateModified=" + dateModified +
                '}';
    }
} 