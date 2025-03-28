package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.Movie;

import java.util.List;

@Dao
public interface MovieDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Movie movie);
    
    @Update
    void update(Movie movie);
    
    @Delete
    void delete(Movie movie);
    
    @Query("DELETE FROM movies")
    void deleteAll();
    
    @Query("SELECT * FROM movies WHERE id = :id")
    LiveData<Movie> getMovieById(int id);
    
    @Query("SELECT * FROM movies ORDER BY title ASC")
    LiveData<List<Movie>> getAllMovies();
    
    @Query("SELECT * FROM movies WHERE watched = 1 ORDER BY title ASC")
    LiveData<List<Movie>> getWatchedMovies();
    
    @Query("SELECT * FROM movies WHERE watched = 0 ORDER BY title ASC")
    LiveData<List<Movie>> getUnwatchedMovies();
    
    @Query("SELECT * FROM movies WHERE favorite = 1 ORDER BY title ASC")
    LiveData<List<Movie>> getFavoriteMovies();
    
    @Query("SELECT * FROM movies WHERE genreIds LIKE '%' || :genreId || '%' ORDER BY title ASC")
    LiveData<List<Movie>> getMoviesByGenre(int genreId);
    
    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    LiveData<List<Movie>> searchMovies(String query);
} 