package com.example.myapplication.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.myapplication.dao.GenreDao;
import com.example.myapplication.dao.MovieDao;
import com.example.myapplication.dao.MovieGenreCrossRefDao;
import com.example.myapplication.database.MovieDatabase;
import com.example.myapplication.model.Genre;
import com.example.myapplication.model.Movie;
import com.example.myapplication.model.MovieGenreCrossRef;
import com.example.myapplication.model.MovieWithGenres;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MovieRepository {
    
    private final MovieDao movieDao;
    private final GenreDao genreDao;
    private final MovieGenreCrossRefDao movieGenreCrossRefDao;
    private final LiveData<List<Movie>> allMovies;
    private final LiveData<List<MovieWithGenres>> allMoviesWithGenres;
    private final LiveData<List<Genre>> allGenres;
    
    public MovieRepository(Application application) {
        MovieDatabase database = MovieDatabase.getInstance(application);
        movieDao = database.movieDao();
        genreDao = database.genreDao();
        movieGenreCrossRefDao = database.movieGenreCrossRefDao();
        
        allMovies = movieDao.getAllMovies();
        allMoviesWithGenres = movieDao.getMoviesWithGenres();
        allGenres = genreDao.getAllGenres();
        
        // Initialize database with genres if needed
        if (getAllGenresSync().isEmpty()) {
            initializeGenres();
        }
    }
    
    private void initializeGenres() {
        MovieDatabase.databaseWriteExecutor.execute(() -> {
            for (Genre genre : Genre.getPopularGenres()) {
                genreDao.insert(genre);
            }
        });
    }
    
    public void update(Movie movie) {
        MovieDatabase.databaseWriteExecutor.execute(() -> {
            movieDao.update(movie);
        });
    } 

    // Adding a proper insert method that handles genre relationships
    public void insert(Movie movie, List<Integer> genreIds) {
        MovieDatabase.databaseWriteExecutor.execute(() -> {
            // Insert movie and get its ID
            long movieId = movieDao.insert(movie);
            
            // Delete any existing cross-references for this movie (in case of update)
            movieGenreCrossRefDao.deleteAllForMovie((int)movieId);
            
            // Create and insert new cross-references
            List<MovieGenreCrossRef> crossRefs = new ArrayList<>();
            for (Integer genreId : genreIds) {
                crossRefs.add(new MovieGenreCrossRef((int)movieId, genreId));
            }
            movieGenreCrossRefDao.insertAll(crossRefs);
        });
    }
    
    public void delete(Movie movie) {
        MovieDatabase.databaseWriteExecutor.execute(() -> {
            movieDao.delete(movie);
        });
    }
    
    public LiveData<List<Movie>> getAllMovies() {
        return allMovies;
    }
    
    public LiveData<List<MovieWithGenres>> getAllMoviesWithGenres() {
        return allMoviesWithGenres;
    }
    
    public LiveData<Movie> getMovieById(int id) {
        return movieDao.getMovieById(id);
    }
    
    public LiveData<MovieWithGenres> getMovieWithGenresById(int id) {
        return movieDao.getMovieWithGenresById(id);
    }
    
    public LiveData<List<Genre>> getAllGenres() {
        return allGenres;
    }
    
    public List<Genre> getAllGenresSync() {
        try {
            Future<List<Genre>> future = MovieDatabase.databaseWriteExecutor.submit(genreDao::getAllGenresSync);
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<Genre> getGenresForMovie(Movie movie) throws ExecutionException, InterruptedException {
        Future<List<Integer>> future = MovieDatabase.databaseWriteExecutor.submit(() -> 
            movieGenreCrossRefDao.getGenreIdsForMovie(movie.getId()));
        List<Integer> genreIds = future.get();
        
        // Use the static helper method from Genre instead of trying to load from DB
        List<Genre> genres = new ArrayList<>();
        for (Integer genreId : genreIds) {
            Genre genre = Genre.getGenreById(genreId);
            if (genre != null) {
                genres.add(genre);
            }
        }
        
        return genres;
    }
    
    public LiveData<List<MovieWithGenres>> searchMoviesWithGenres(String query) {
        return movieDao.searchMoviesWithGenres("%" + query + "%");
    }
} 