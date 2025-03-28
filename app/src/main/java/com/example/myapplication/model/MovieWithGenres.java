package com.example.myapplication.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class MovieWithGenres {
    @Embedded
    public Movie movie;
    
    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(
                    value = MovieGenreCrossRef.class,
                    parentColumn = "movieId",
                    entityColumn = "genreId"
            )
    )
    public List<Genre> genres;
} 