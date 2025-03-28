package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.GenreAdapter;
import com.example.myapplication.adapter.MovieAdapter;
import com.example.myapplication.model.Genre;
import com.example.myapplication.model.Movie;
import com.example.myapplication.viewmodel.MovieViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MovieAdapter.OnMovieClickListener, GenreAdapter.OnGenreClickListener {

    private MovieViewModel movieViewModel;
    private MovieAdapter movieAdapter;
    private GenreAdapter genreAdapter;
    private RecyclerView recyclerMovies;
    private RecyclerView recyclerGenres;
    private ChipGroup filterChipGroup;
    private View emptyView;

    private static final int ADD_MOVIE_REQUEST_CODE = 1;
    private static final int EDIT_MOVIE_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настраиваем Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Инициализируем компоненты UI
        recyclerMovies = findViewById(R.id.recycler_movies);
        recyclerGenres = findViewById(R.id.recycler_genres);
        filterChipGroup = findViewById(R.id.filter_chip_group);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAddMovie = findViewById(R.id.fab_add_movie);

        // Настраиваем адаптеры и RecyclerView
        setupMovieRecyclerView();
        setupGenreRecyclerView();
        
        // Настраиваем ViewModel и наблюдателей
        movieViewModel = new ViewModelProvider(this).get(MovieViewModel.class);
        
        // Наблюдаем за списком фильмов с учетом фильтров
        movieViewModel.getFilteredMovies().observe(this, movies -> {
            movieAdapter.setMovies(movies);
            updateEmptyView(movies != null && movies.isEmpty());
        });
        
        // Наблюдаем за списком жанров
        movieViewModel.getAllGenres().observe(this, genres -> {
            genreAdapter.setGenres(genres);
        });

        // Настраиваем слушатели для фильтрации
        setupFilterListeners();
        
        // Настраиваем FAB для добавления фильма
        fabAddMovie.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditMovieActivity.class);
            startActivityForResult(intent, ADD_MOVIE_REQUEST_CODE);
        });
    }

    private void setupMovieRecyclerView() {
        movieAdapter = new MovieAdapter(this, this);
        recyclerMovies.setLayoutManager(new LinearLayoutManager(this));
        recyclerMovies.setHasFixedSize(true);
        recyclerMovies.setAdapter(movieAdapter);
    }

    private void setupGenreRecyclerView() {
        genreAdapter = new GenreAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerGenres.setLayoutManager(layoutManager);
        recyclerGenres.setHasFixedSize(true);
        recyclerGenres.setAdapter(genreAdapter);
    }

    private void setupFilterListeners() {
        // Настраиваем слушатели для чипов фильтрации
        Chip chipAll = findViewById(R.id.chip_all);
        Chip chipWatched = findViewById(R.id.chip_watched);
        Chip chipUnwatched = findViewById(R.id.chip_unwatched);
        Chip chipFavorite = findViewById(R.id.chip_favorite);
        
        // Ensure All is selected by default
        chipAll.setChecked(true);
        movieViewModel.setFilter(MovieViewModel.FILTER_ALL);
        
        // Use OnCheckedChangeListener to handle category selection logic
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Only process when the chip is checked (not when unchecked)
            if (isChecked) {
                // Uncheck other chips
                chipWatched.setChecked(false);
                chipUnwatched.setChecked(false);
                chipFavorite.setChecked(false);
                
                movieViewModel.setFilter(MovieViewModel.FILTER_ALL);
            } else {
                // If all categories are unchecked, re-check the All chip
                if (!chipWatched.isChecked() && !chipUnwatched.isChecked() && !chipFavorite.isChecked()) {
                    // Using post to avoid recursive listener calls
                    buttonView.post(() -> chipAll.setChecked(true));
                }
            }
        });
        
        chipWatched.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck other chips
                chipAll.setChecked(false);
                chipUnwatched.setChecked(false);
                chipFavorite.setChecked(false);
                
                movieViewModel.setFilter(MovieViewModel.FILTER_WATCHED);
            } else {
                // If all categories are unchecked, check the All chip
                if (!chipAll.isChecked() && !chipUnwatched.isChecked() && !chipFavorite.isChecked()) {
                    chipAll.setChecked(true);
                }
            }
        });
        
        chipUnwatched.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck other chips
                chipAll.setChecked(false);
                chipWatched.setChecked(false);
                chipFavorite.setChecked(false);
                
                movieViewModel.setFilter(MovieViewModel.FILTER_UNWATCHED);
            } else {
                // If all categories are unchecked, check the All chip
                if (!chipAll.isChecked() && !chipWatched.isChecked() && !chipFavorite.isChecked()) {
                    chipAll.setChecked(true);
                }
            }
        });
        
        chipFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck other chips
                chipAll.setChecked(false);
                chipWatched.setChecked(false);
                chipUnwatched.setChecked(false);
                
                movieViewModel.setFilter(MovieViewModel.FILTER_FAVORITES);
            } else {
                // If all categories are unchecked, check the All chip
                if (!chipAll.isChecked() && !chipWatched.isChecked() && !chipUnwatched.isChecked()) {
                    chipAll.setChecked(true);
                }
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            recyclerMovies.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerMovies.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMovieClick(Movie movie) {
        // Открываем детальную информацию о фильме
        Intent intent = new Intent(MainActivity.this, MovieDetailsActivity.class);
        intent.putExtra(MovieDetailsActivity.EXTRA_MOVIE_ID, movie.getId());
        startActivity(intent);
    }

    @Override
    public void onGenreClick(Genre genre) {
        // Обработка нажатия на жанр
        int genreId = genre.getId();
        
        // Если тот же жанр был выбран повторно, сбрасываем фильтр
        if (movieViewModel.getSelectedGenreId() == genreId) {
            movieViewModel.setSelectedGenreId(-1);
            genreAdapter.setSelectedGenreIds(new ArrayList<>());
        } else {
            // Иначе устанавливаем новый фильтр
            movieViewModel.setSelectedGenreId(genreId);
            List<Integer> selectedGenres = new ArrayList<>();
            selectedGenres.add(genreId);
            genreAdapter.setSelectedGenreIds(selectedGenres);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        // Настраиваем поиск
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
        }

        @Override
            public boolean onQueryTextChange(String newText) {
                movieViewModel.searchMovies(newText);
                return true;
            }
        });
        
        // Сбросим фильтры, когда закрывается поиск
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
        }

        @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                movieViewModel.searchMovies("");
                return true;
            }
        });
        
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == ADD_MOVIE_REQUEST_CODE) {
                // Фильм был добавлен
            } else if (requestCode == EDIT_MOVIE_REQUEST_CODE) {
                // Фильм был отредактирован
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force refresh the movies list when returning to MainActivity
        // This ensures any changes made in AddEditMovieActivity or MovieDetailsActivity are reflected
        movieAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_search) {
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}