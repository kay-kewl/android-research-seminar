package com.example.myapplication.data;

/**
 * Model class representing a single fact
 */
public class Fact {
    
    public enum Category {
        SCIENCE,
        HISTORY,
        ANIMALS,
        GEOGRAPHY,
        PHYSICS,
        ASTRONOMY,
        ECONOMICS,
        LINGUISTICS,
        MUSIC,
        LITERATURE,
        ALL
    }
    
    private int id;
    private String content;
    private Category category;
    private String source;
    private boolean isTrue; // Used for quiz functionality
    private boolean isShown; // Track if this fact has been shown to the user
    
    public Fact() {
        // Default constructor
    }
    
    public Fact(int id, String content, Category category, String source, boolean isTrue) {
        this.id = id;
        this.content = content;
        this.category = category;
        this.source = source;
        this.isTrue = isTrue;
        this.isShown = false;
    }
    
    // Getters and setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public boolean isTrue() {
        return isTrue;
    }
    
    public void setTrue(boolean isTrue) {
        this.isTrue = isTrue;
    }
    
    public boolean isShown() {
        return isShown;
    }
    
    public void setShown(boolean shown) {
        isShown = shown;
    }
    
    @Override
    public String toString() {
        return content;
    }
} 