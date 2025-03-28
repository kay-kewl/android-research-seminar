package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.adapter.GenreAdapter;
import com.example.myapplication.adapter.ReadOnlyGenreAdapter;
import com.example.myapplication.model.Genre;
import com.example.myapplication.model.Movie;
import com.example.myapplication.model.MovieWithGenres;
import com.example.myapplication.viewmodel.MovieViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class MovieDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIE_ID = "com.example.myapplication.EXTRA_MOVIE_ID";
    private static final int EDIT_MOVIE_REQUEST_CODE = 1;

    private MovieViewModel movieViewModel;
    private int movieId;
    private Movie currentMovie;
    private ReadOnlyGenreAdapter genreAdapter;

    private ImageView imageViewPoster;
    private TextView textViewTitle, textViewOverview, textViewReleaseDate;
    private RatingBar ratingBar;
    private ChipGroup chipGroupGenres;
    private Chip chipWatched, chipFavorite;
    private RecyclerView genreRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        // Настраиваем Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Инициализируем компоненты UI
        imageViewPoster = findViewById(R.id.image_poster);
        textViewTitle = findViewById(R.id.text_title);
        textViewOverview = findViewById(R.id.text_overview);
        textViewReleaseDate = findViewById(R.id.text_release_date);
        ratingBar = findViewById(R.id.rating_bar);
        chipGroupGenres = findViewById(R.id.chip_group_genres);
        chipWatched = findViewById(R.id.chip_watched);
        chipFavorite = findViewById(R.id.chip_favorite);
        genreRecyclerView = findViewById(R.id.recycler_genres);

        // Настраиваем RecyclerView для жанров
        setupGenreRecyclerView();

        // Настраиваем ViewModel
        movieViewModel = new ViewModelProvider(this).get(MovieViewModel.class);

        // Получаем ID фильма из Intent
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_MOVIE_ID)) {
            movieId = intent.getIntExtra(EXTRA_MOVIE_ID, -1);
            loadMovie();
        } else {
            Toast.makeText(this, "Ошибка: ID фильма не указан", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Настраиваем слушатели для чипов Watched и Favorite
        setupChipListeners();
    }

    private void setupGenreRecyclerView() {
        genreAdapter = new ReadOnlyGenreAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        genreRecyclerView.setLayoutManager(layoutManager);
        genreRecyclerView.setAdapter(genreAdapter);
    }

    private void setupChipListeners() {
        // Обработка переключения статуса "Просмотрено"
        chipWatched.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentMovie != null && buttonView.isPressed()) {
                updateMovieWatchedStatus(isChecked);
            }
        });

        // Обработка переключения статуса "Избранное"
        chipFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentMovie != null && buttonView.isPressed()) {
                updateMovieFavoriteStatus(isChecked);
            }
        });
    }

    private void loadMovie() {
        movieViewModel.getMovieWithGenresById(movieId).observe(this, this::displayMovieDetails);
    }

    private void displayMovieDetails(MovieWithGenres movieWithGenres) {
        if (movieWithGenres != null) {
            currentMovie = movieWithGenres.movie;

            // Устанавливаем заголовок и данные фильма
            getSupportActionBar().setTitle(currentMovie.getTitle());
            textViewTitle.setText(currentMovie.getTitle());
            textViewOverview.setText(currentMovie.getOverview());
            textViewReleaseDate.setText(currentMovie.getReleaseDate());
            ratingBar.setRating(currentMovie.getRating() / 2); // 10-бальная в 5-бальную

            // Загружаем постер
            if (currentMovie.getPosterUrl() != null && !currentMovie.getPosterUrl().isEmpty()) {
                if (currentMovie.getPosterUrl().startsWith("content://")) {
                    Glide.with(this)
                        .load(currentMovie.getPosterUrl())
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .into(imageViewPoster);
                } else {
                    Glide.with(this)
                        .load(currentMovie.getPosterUrl())
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .into(imageViewPoster);
                }
            }

            // Устанавливаем состояние чипов
            chipWatched.setChecked(currentMovie.isWatched());
            chipFavorite.setChecked(currentMovie.isFavorite());

            // Отображаем жанры
            displayGenres(movieWithGenres.genres);
        }
    }

    private void displayGenres(List<Genre> genres) {
        genreAdapter.setGenres(genres);
    }

    private void updateMovieWatchedStatus(boolean isWatched) {
        if (currentMovie != null) {
            currentMovie.setWatched(isWatched);
            updateMovie(currentMovie);
        }
    }

    private void updateMovieFavoriteStatus(boolean isFavorite) {
        if (currentMovie != null) {
            currentMovie.setFavorite(isFavorite);
            updateMovie(currentMovie);
        }
    }

    private void updateMovie(Movie movie) {
        try {
            // Create a new instance to ensure the object is seen as changed
            Movie updatedMovie = new Movie();
            updatedMovie.setId(movie.getId());
            updatedMovie.setTitle(movie.getTitle());
            updatedMovie.setPosterUrl(movie.getPosterUrl());
            updatedMovie.setRating(movie.getRating());
            updatedMovie.setGenreIds(movie.getGenreIds());
            updatedMovie.setWatched(movie.isWatched());
            updatedMovie.setFavorite(movie.isFavorite());
            updatedMovie.setOverview(movie.getOverview());
            updatedMovie.setReleaseDate(movie.getReleaseDate());
            
            // Get genre IDs from the movie
            List<Integer> genreIds = movie.getGenreIdList();
            
            // Instead of simple update, use insert which handles genre relationships
            movieViewModel.insert(updatedMovie, genreIds);
            
            // Add a small delay to ensure DB operations complete before refreshing
            new android.os.Handler().postDelayed(() -> {
                // Force reload data after update
                loadMovie();
            }, 200);
            
            Toast.makeText(this, "Обновление статуса: " + 
                           (movie.isWatched() ? "Просмотрено" : "Не просмотрено") + ", " +
                           (movie.isFavorite() ? "Избранное" : "Не избранное"), 
                           Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при обновлении фильма: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_movie_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // Возврат к предыдущему экрану
            onBackPressed();
            return true;
        } else if (id == R.id.action_edit) {
            // Редактирование фильма
            editMovie();
            return true;
        } else if (id == R.id.action_delete) {
            // Удаление фильма
            confirmDeleteMovie();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void editMovie() {
        Intent intent = new Intent(this, AddEditMovieActivity.class);
        intent.putExtra(AddEditMovieActivity.EXTRA_MOVIE_ID, movieId);
        startActivityForResult(intent, EDIT_MOVIE_REQUEST_CODE);
    }

    private void confirmDeleteMovie() {
        new AlertDialog.Builder(this)
            .setTitle("Удалить фильм")
            .setMessage("Вы действительно хотите удалить этот фильм?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                movieViewModel.delete(currentMovie);
                Toast.makeText(this, "Фильм удален", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == EDIT_MOVIE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Обновляем данные фильма
            loadMovie();
        }
    }
} 