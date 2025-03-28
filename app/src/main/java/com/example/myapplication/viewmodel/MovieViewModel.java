package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.model.Genre;
import com.example.myapplication.model.Movie;
import com.example.myapplication.model.MovieWithGenres;
import com.example.myapplication.repository.MovieRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MovieViewModel extends AndroidViewModel {
    
    private final MovieRepository repository;
    private final LiveData<List<Movie>> allMovies;
    private final LiveData<List<MovieWithGenres>> allMoviesWithGenres;
    private final LiveData<List<Genre>> allGenres;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<MovieWithGenres>> searchResultsWithGenres;
    
    // Используем MutableLiveData для текущего фильтра и жанра, чтобы изменения отображались
    private final MutableLiveData<Integer> currentFilter = new MutableLiveData<>(FILTER_ALL);
    private final MutableLiveData<Integer> selectedGenreId = new MutableLiveData<>(-1);
    
    public static final int FILTER_ALL = 0;
    public static final int FILTER_WATCHED = 1;
    public static final int FILTER_UNWATCHED = 2;
    public static final int FILTER_FAVORITES = 3;
    
    public MovieViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
        allMovies = repository.getAllMovies();
        allMoviesWithGenres = repository.getAllMoviesWithGenres();
        allGenres = repository.getAllGenres();
        searchResultsWithGenres = Transformations.switchMap(searchQuery, repository::searchMoviesWithGenres);
    }
    
    // Методы для фильтрации и поиска
    
    public LiveData<List<MovieWithGenres>> getFilteredMovies() {
        // Make searchQuery part of the transformation chain so it responds to search changes
        return Transformations.switchMap(searchQuery, query -> {
            // Select data source based on whether there's a search query
            LiveData<List<MovieWithGenres>> baseSource;
            if (query != null && !query.isEmpty()) {
                baseSource = searchResultsWithGenres;
            } else {
                baseSource = allMoviesWithGenres;
            }
            
            // Apply other filters (category and genre)
            return Transformations.switchMap(currentFilter, filter -> 
                  Transformations.switchMap(selectedGenreId, genreId -> 
                  Transformations.map(baseSource, movies -> {
                      if (movies == null) return new ArrayList<>();
                      List<MovieWithGenres> filtered = new ArrayList<>(movies);
                      return applyFilters(filtered, filter, genreId);
                  })));
        });
    }
    
    // Вспомогательный метод для применения фильтров к списку фильмов
    private List<MovieWithGenres> applyFilters(List<MovieWithGenres> movies, int filter, int genreId) {
        List<MovieWithGenres> result = new ArrayList<>();
        
        // Фильтрация по типу (все/смотрел/не смотрел/избранное)
        for (MovieWithGenres movie : movies) {
            boolean passesFilter = true;
            
            switch (filter) {
                case FILTER_WATCHED:
                    if (!movie.movie.isWatched()) passesFilter = false;
                    break;
                case FILTER_UNWATCHED:
                    if (movie.movie.isWatched()) passesFilter = false;
                    break;
                case FILTER_FAVORITES:
                    if (!movie.movie.isFavorite()) passesFilter = false;
                    break;
                case FILTER_ALL:
                default:
                    // Не применяем дополнительную фильтрацию
                    break;
            }
            
            // Фильтрация по жанру
            if (passesFilter && genreId > 0) {
                boolean hasGenre = false;
                for (Genre genre : movie.genres) {
                    if (genre.getId() == genreId) {
                        hasGenre = true;
                        break;
                    }
                }
                if (!hasGenre) passesFilter = false;
            }
            
            if (passesFilter) {
                result.add(movie);
            }
        }
        
        return result;
    }
    
    public void setFilter(int filter) {
        currentFilter.setValue(filter);
    }
    
    public void setSelectedGenreId(int genreId) {
        selectedGenreId.setValue(genreId);
    }
    
    public int getSelectedGenreId() {
        return selectedGenreId.getValue() != null ? selectedGenreId.getValue() : -1;
    }
    
    public void searchMovies(String query) {
        searchQuery.setValue(query);
    }
    
    // Методы для работы с Movie
    
    public void insert(Movie movie, List<Integer> genreIds) {
        repository.insert(movie, genreIds);
    }
    
    public void update(Movie movie) {
        repository.update(movie);
    }
    
    public void delete(Movie movie) {
        repository.delete(movie);
    }
    
    public LiveData<List<Movie>> getAllMovies() {
        return allMovies;
    }
    
    public LiveData<Movie> getMovieById(int id) {
        return repository.getMovieById(id);
    }
    
    public LiveData<MovieWithGenres> getMovieWithGenresById(int id) {
        return repository.getMovieWithGenresById(id);
    }
    
    // Методы для работы с Genre
    
    public LiveData<List<Genre>> getAllGenres() {
        return allGenres;
    }
    
    public List<Genre> getAllGenresSync() throws ExecutionException, InterruptedException {
        return repository.getAllGenresSync();
    }
    
    public List<Genre> getGenresForMovie(Movie movie) throws ExecutionException, InterruptedException {
        return repository.getGenresForMovie(movie);
    }
} 