package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "reviews", 
        foreignKeys = @ForeignKey(
                entity = Movie.class,
                parentColumns = "id",
                childColumns = "movieId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("movieId")})
public class Review {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int movieId;
    private String author;
    private String content;
    private float rating;
    private long date;

    public Review(int movieId, String author, String content, float rating) {
        this.movieId = movieId;
        this.author = author;
        this.content = content;
        this.rating = rating;
        this.date = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
    
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(new Date(date));
    }
} 