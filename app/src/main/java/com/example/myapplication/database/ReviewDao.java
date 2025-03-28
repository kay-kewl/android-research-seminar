package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.Review;

import java.util.List;

@Dao
public interface ReviewDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Review review);
    
    @Update
    void update(Review review);
    
    @Delete
    void delete(Review review);
    
    @Query("DELETE FROM reviews WHERE movieId = :movieId")
    void deleteAllForMovie(int movieId);
    
    @Query("SELECT * FROM reviews WHERE id = :id")
    LiveData<Review> getReviewById(int id);
    
    @Query("SELECT * FROM reviews WHERE movieId = :movieId ORDER BY date DESC")
    LiveData<List<Review>> getReviewsForMovie(int movieId);
    
    @Query("SELECT AVG(rating) FROM reviews WHERE movieId = :movieId")
    float getAverageRating(int movieId);
    
    @Query("SELECT COUNT(*) FROM reviews WHERE movieId = :movieId")
    int getReviewCount(int movieId);
} 