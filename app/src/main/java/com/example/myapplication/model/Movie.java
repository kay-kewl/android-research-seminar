package com.example.myapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity(tableName = "movies")
public class Movie {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String posterUrl;
    private float rating;
    private String genreIds;
    private boolean watched;
    private boolean favorite;
    private String overview;
    private String releaseDate;

    public Movie() {
    }

    @Ignore
    public Movie(String title, String posterUrl, float rating, String genreIds,
                boolean watched, boolean favorite, String overview, String releaseDate) {
        this.title = title;
        this.posterUrl = posterUrl;
        this.rating = rating;
        this.genreIds = genreIds;
        this.watched = watched;
        this.favorite = favorite;
        this.overview = overview;
        this.releaseDate = releaseDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(String genreIds) {
        this.genreIds = genreIds;
    }

    public List<Integer> getGenreIdList() {
        return getGenreIdsFromString(genreIds);
    }

    public void setGenreIdList(List<Integer> genreIds) {
        this.genreIds = getGenreIdsAsString(genreIds);
    }

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @NonNull
    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", rating=" + rating +
                ", watched=" + watched +
                ", favorite=" + favorite +
                '}';
    }

    // Преобразование строки genreIds в список int
    public static List<Integer> getGenreIdsFromString(String genreIdsString) {
        if (genreIdsString == null || genreIdsString.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(genreIdsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    // Преобразование списка int в строку genreIds
    public static String getGenreIdsAsString(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return "";
        }
        return genreIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
} 