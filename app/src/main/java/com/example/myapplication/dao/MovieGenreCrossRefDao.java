package com.example.myapplication.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.model.MovieGenreCrossRef;

import java.util.List;

@Dao
public interface MovieGenreCrossRefDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MovieGenreCrossRef crossRef);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MovieGenreCrossRef> crossRefs);
    
    @Delete
    void delete(MovieGenreCrossRef crossRef);
    
    @Query("DELETE FROM movie_genre_cross_ref WHERE movieId = :movieId")
    void deleteAllForMovie(int movieId);
    
    @Query("SELECT genreId FROM movie_genre_cross_ref WHERE movieId = :movieId")
    List<Integer> getGenreIdsForMovie(int movieId);
} 