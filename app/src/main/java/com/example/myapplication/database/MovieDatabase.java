package com.example.myapplication.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.myapplication.dao.GenreDao;
import com.example.myapplication.dao.MovieDao;
import com.example.myapplication.dao.MovieGenreCrossRefDao;
import com.example.myapplication.model.Genre;
import com.example.myapplication.model.Movie;
import com.example.myapplication.model.MovieGenreCrossRef;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Movie.class, Genre.class, MovieGenreCrossRef.class}, version = 1, exportSchema = false)
public abstract class MovieDatabase extends RoomDatabase {
    
    private static MovieDatabase instance;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    
    public abstract MovieDao movieDao();
    public abstract GenreDao genreDao();
    public abstract MovieGenreCrossRefDao movieGenreCrossRefDao();
    
    public static synchronized MovieDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            MovieDatabase.class, "movie_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback)
                    .build();
        }
        return instance;
    }
    
    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            
            databaseWriteExecutor.execute(() -> {
                // Заполняем базу данных начальными данными при создании
                GenreDao genreDao = instance.genreDao();
                genreDao.insertAll(Genre.getPopularGenres());
                
                // Не предустанавливаем фильмы, позволяем пользователю добавлять свои
            });
        }
    };
} 