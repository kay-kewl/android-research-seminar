package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.adapter.GenreSelectionAdapter;
import com.example.myapplication.model.Genre;
import com.example.myapplication.model.Movie;
import com.example.myapplication.viewmodel.MovieViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AddEditMovieActivity extends AppCompatActivity implements GenreSelectionAdapter.OnGenreSelectedListener {

    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    public static final String EXTRA_MOVIE_ID = "com.example.myapplication.EXTRA_MOVIE_ID";

    private MovieViewModel movieViewModel;
    private EditText editTextTitle;
    private ImageView imageViewPoster;
    private Switch switchWatched, switchFavorite;
    private RecyclerView recyclerGenres;
    private GenreSelectionAdapter genreAdapter;
    private Uri selectedImageUri;
    private int movieId = -1;
    private Movie currentMovie;
    private List<Genre> allGenres = new ArrayList<>();
    private List<Integer> selectedGenreIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_movie);

        // Настраиваем Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Инициализируем компоненты UI
        editTextTitle = findViewById(R.id.edit_text_title);
        imageViewPoster = findViewById(R.id.image_view_poster);
        switchWatched = findViewById(R.id.switch_watched);
        switchFavorite = findViewById(R.id.switch_favorite);
        recyclerGenres = findViewById(R.id.recycler_genres);
        Button buttonSelectImage = findViewById(R.id.button_select_image);

        // Настраиваем ViewModel
        movieViewModel = new ViewModelProvider(this).get(MovieViewModel.class);

        // Настраиваем RecyclerView для жанров
        genreAdapter = new GenreSelectionAdapter(this);
        recyclerGenres.setLayoutManager(new LinearLayoutManager(this));
        recyclerGenres.setAdapter(genreAdapter);

        // Загружаем все жанры
        try {
            allGenres = movieViewModel.getAllGenresSync();
            genreAdapter.setGenres(allGenres);
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, "Ошибка загрузки жанров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Проверяем, редактируем ли существующий фильм или создаем новый
        if (getIntent().hasExtra(EXTRA_MOVIE_ID)) {
            // Редактирование существующего фильма
            movieId = getIntent().getIntExtra(EXTRA_MOVIE_ID, -1);
            getSupportActionBar().setTitle("Редактировать фильм");

            // Загружаем данные о фильме и его жанрах
            movieViewModel.getMovieWithGenresById(movieId).observe(this, movieWithGenres -> {
                if (movieWithGenres != null) {
                    currentMovie = movieWithGenres.movie;
                    fillUIWithMovieData(movieWithGenres.movie);

                    // Отмечаем выбранные жанры
                    List<Integer> genreIds = new ArrayList<>();
                    for (Genre genre : movieWithGenres.genres) {
                        genreIds.add(genre.getId());
                    }
                    selectedGenreIds = genreIds;
                    genreAdapter.setSelectedGenreIds(selectedGenreIds);
                }
            });
        } else {
            // Создание нового фильма
            getSupportActionBar().setTitle("Добавить фильм");
        }

        // Обработчик для выбора изображения
        buttonSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        });
    }

    private void fillUIWithMovieData(Movie movie) {
        editTextTitle.setText(movie.getTitle());

        // Загружаем изображение постера
        if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
            if (movie.getPosterUrl().startsWith("content://")) {
                selectedImageUri = Uri.parse(movie.getPosterUrl());
                Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .into(imageViewPoster);
            } else {
                Glide.with(this)
                    .load(movie.getPosterUrl())
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .into(imageViewPoster);
            }
        }

        switchWatched.setChecked(movie.isWatched());
        switchFavorite.setChecked(movie.isFavorite());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit_movie, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_save) {
            saveMovie();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void saveMovie() {
        String title = editTextTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите название фильма", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null && (currentMovie == null || currentMovie.getPosterUrl() == null)) {
            Toast.makeText(this, "Пожалуйста, выберите изображение для постера", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedGenreIds.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, выберите хотя бы один жанр", Toast.LENGTH_SHORT).show();
            return;
        }

        String posterUrl = selectedImageUri != null ? selectedImageUri.toString() : 
                            (currentMovie != null ? currentMovie.getPosterUrl() : "");
        
        boolean watched = switchWatched.isChecked();
        boolean favorite = switchFavorite.isChecked();

        // Преобразуем список ID жанров в строку
        String genreIdsString = Movie.getGenreIdsAsString(selectedGenreIds);

        if (movieId == -1) {
            // Создание нового фильма
            Movie newMovie = new Movie(
                title,
                posterUrl,
                0.0f,
                genreIdsString,
                watched,
                favorite,
                "",
                ""
            );
            
            movieViewModel.insert(newMovie, selectedGenreIds);
            Toast.makeText(this, "Фильм добавлен", Toast.LENGTH_SHORT).show();
        } else {
            // Обновление существующего фильма
            Movie updatedMovie = new Movie(
                title,
                posterUrl,
                currentMovie.getRating(),
                genreIdsString,
                watched,
                favorite,
                currentMovie.getOverview(),
                currentMovie.getReleaseDate()
            );
            updatedMovie.setId(movieId);
            
            // Instead of just updating the movie, we need to use the insert method
            // that also handles the genre relationships
            movieViewModel.insert(updatedMovie, selectedGenreIds);
            Toast.makeText(this, "Фильм обновлен", Toast.LENGTH_SHORT).show();
        }
        
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            
            // Делаем изображение постоянно доступным для приложения
            getContentResolver().takePersistableUriPermission(
                selectedImageUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
            
            // Отображаем выбранное изображение
            Glide.with(this)
                .load(selectedImageUri)
                .placeholder(R.drawable.ic_movie_placeholder)
                .into(imageViewPoster);
        }
    }

    @Override
    public void onGenreSelected(Genre genre, boolean isSelected) {
        if (isSelected) {
            if (!selectedGenreIds.contains(genre.getId())) {
                selectedGenreIds.add(genre.getId());
            }
        } else {
            selectedGenreIds.remove(Integer.valueOf(genre.getId()));
        }
    }
} 