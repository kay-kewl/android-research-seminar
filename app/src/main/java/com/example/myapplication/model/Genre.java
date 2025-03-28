package com.example.myapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity(tableName = "genres")
public class Genre {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;

    public Genre(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Genre genre = (Genre) obj;
        return id == genre.id;
    }
    
    @Override
    public int hashCode() {
        return id;
    }

    public static List<Genre> getGenres() {
        return Arrays.asList(
            new Genre("Фантастика"),
            new Genre("Приключения"),
            new Genre("Триллер"),
            new Genre("Драма"),
            new Genre("Фэнтези"),
            new Genre("Боевик"),
            new Genre("Комедия"),
            new Genre("Криминал"),
            new Genre("Мультфильм")
        );
    }
    
    public static Genre getGenreById(int id) {
        // Use popular genres list which has proper IDs
        Genre[] allGenres = getPopularGenres();
        // Set IDs for each genre (1-based)
        for (int i = 0; i < allGenres.length; i++) {
            allGenres[i].setId(i + 1);
        }
        
        // Find by ID
        for (Genre genre : allGenres) {
            if (genre.getId() == id) {
                return genre;
            }
        }
        return null;
    }
    
    public static List<Genre> getGenresForMovie(Movie movie) {
        List<Genre> genres = new ArrayList<>();
        for (Integer id : movie.getGenreIdList()) {
            Genre genre = getGenreById(id);
            if (genre != null) {
                genres.add(genre);
            }
        }
        return genres;
    }

    public static Genre[] getPopularGenres() {
        return new Genre[] {
                new Genre("Боевик"),
                new Genre("Драма"),
                new Genre("Комедия"),
                new Genre("Фантастика"),
                new Genre("Ужасы"),
                new Genre("Триллер"),
                new Genre("Мелодрама"),
                new Genre("Детектив"),
                new Genre("Приключения"),
                new Genre("Мультфильм"),
                new Genre("Фэнтези"),
                new Genre("Документальный")
        };
    }
} 