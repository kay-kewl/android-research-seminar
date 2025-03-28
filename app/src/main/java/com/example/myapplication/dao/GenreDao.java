package com.example.myapplication.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.Genre;

import java.util.List;

@Dao
public interface GenreDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Genre genre);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Genre... genres);
    
    @Update
    void update(Genre genre);
    
    @Delete
    void delete(Genre genre);
    
    @Query("SELECT * FROM genres WHERE id = :id")
    LiveData<Genre> getGenreById(int id);
    
    @Query("SELECT * FROM genres ORDER BY name ASC")
    LiveData<List<Genre>> getAllGenres();
    
    @Query("SELECT * FROM genres ORDER BY name ASC")
    List<Genre> getAllGenresSync();
} 