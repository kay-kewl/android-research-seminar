package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.model.Genre;

import java.util.List;

@Dao
public interface GenreDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Genre genre);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Genre> genres);
    
    @Query("SELECT * FROM genres ORDER BY name ASC")
    LiveData<List<Genre>> getAllGenres();
    
    @Query("SELECT * FROM genres WHERE id = :id")
    LiveData<Genre> getGenreById(int id);
    
    @Query("SELECT * FROM genres WHERE id IN (:ids)")
    List<Genre> getGenresByIds(List<Integer> ids);
} 