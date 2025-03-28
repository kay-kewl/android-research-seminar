package com.example.myapplication.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "movie_genre_cross_ref",
        primaryKeys = {"movieId", "genreId"},
        foreignKeys = {
                @ForeignKey(
                        entity = Movie.class,
                        parentColumns = "id",
                        childColumns = "movieId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Genre.class,
                        parentColumns = "id",
                        childColumns = "genreId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "movieId"),
                @Index(value = "genreId")
        }
)
public class MovieGenreCrossRef {
    private int movieId;
    private int genreId;

    public MovieGenreCrossRef(int movieId, int genreId) {
        this.movieId = movieId;
        this.genreId = genreId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getGenreId() {
        return genreId;
    }

    public void setGenreId(int genreId) {
        this.genreId = genreId;
    }
} 