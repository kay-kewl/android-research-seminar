package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Genre;
import com.example.myapplication.model.Movie;
import com.example.myapplication.model.MovieWithGenres;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends ListAdapter<Movie, MovieAdapter.MovieViewHolder> {
    
    private final OnMovieClickListener listener;
    private final Context context;
    
    public MovieAdapter(Context context, OnMovieClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }
    
    private static final DiffUtil.ItemCallback<Movie> DIFF_CALLBACK = new DiffUtil.ItemCallback<Movie>() {
        @Override
        public boolean areItemsTheSame(@NonNull Movie oldItem, @NonNull Movie newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Movie oldItem, @NonNull Movie newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getPosterUrl().equals(newItem.getPosterUrl()) &&
                   oldItem.getRating() == newItem.getRating() &&
                   oldItem.isWatched() == newItem.isWatched() &&
                   oldItem.isFavorite() == newItem.isFavorite() &&
                   oldItem.getGenreIds().equals(newItem.getGenreIds());
        }
    };
    
    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = getItem(position);
        
        holder.titleTextView.setText(movie.getTitle());
        holder.ratingBar.setVisibility(View.GONE);
        
        // Загружаем постер
        if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
            // Обработка различных типов URL
            if (movie.getPosterUrl().startsWith("http")) {
                // Web URL
                Glide.with(context)
                        .load(movie.getPosterUrl())
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .error(R.drawable.ic_movie_placeholder)
                        .into(holder.posterImageView);
            } else if (movie.getPosterUrl().startsWith("content://")) {
                // Content URI (selected from gallery)
                Glide.with(context)
                        .load(movie.getPosterUrl())
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .error(R.drawable.ic_movie_placeholder)
                        .into(holder.posterImageView);
            } else if (movie.getPosterUrl().startsWith("/")) {
                // Absolute file path
                Glide.with(context)
                        .load(movie.getPosterUrl())
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .error(R.drawable.ic_movie_placeholder)
                        .into(holder.posterImageView);
            } else {
                // Fallback to resource ID
                int resourceId = context.getResources().getIdentifier(
                        movie.getPosterUrl().replace(".jpg", ""), "drawable", context.getPackageName());
                if (resourceId != 0) {
                    Glide.with(context)
                            .load(resourceId)
                            .placeholder(R.drawable.ic_movie_placeholder)
                            .error(R.drawable.ic_movie_placeholder)
                            .into(holder.posterImageView);
                } else {
                    holder.posterImageView.setImageResource(R.drawable.ic_movie_placeholder);
                }
            }
        } else {
            holder.posterImageView.setImageResource(R.drawable.ic_movie_placeholder);
        }
        
        // Отображаем жанры
        List<Genre> genres = Genre.getGenresForMovie(movie);
        if (!genres.isEmpty()) {
            StringBuilder genreText = new StringBuilder();
            for (int i = 0; i < genres.size(); i++) {
                genreText.append(genres.get(i).getName());
                if (i < genres.size() - 1) {
                    genreText.append(", ");
                }
            }
            holder.genreTextView.setText(genreText.toString());
        } else {
            holder.genreTextView.setText(R.string.no_genres);
        }
        
        // Отображаем индикаторы "Просмотрено" и "Избранное"
        holder.watchedIndicator.setVisibility(movie.isWatched() ? View.VISIBLE : View.GONE);
        holder.favoriteIndicator.setVisibility(movie.isFavorite() ? View.VISIBLE : View.GONE);
        
        // Устанавливаем слушатель нажатий
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onMovieClick(getItem(pos));
            }
        });
    }
    
    public Movie getMovieAt(int position) {
        return getItem(position);
    }
    
    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }
    
    static class MovieViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView genreTextView;
        private final ImageView posterImageView;
        private final RatingBar ratingBar;
        private final ImageView watchedIndicator;
        private final ImageView favoriteIndicator;
        
        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_movie_title);
            genreTextView = itemView.findViewById(R.id.text_movie_genres);
            posterImageView = itemView.findViewById(R.id.image_movie_poster);
            ratingBar = itemView.findViewById(R.id.rating_bar_movie);
            watchedIndicator = itemView.findViewById(R.id.image_watched_indicator);
            favoriteIndicator = itemView.findViewById(R.id.image_favorite_indicator);
        }
    }

    /**
     * Устанавливает список фильмов с жанрами для отображения
     * @param moviesWithGenres список фильмов с прикрепленными жанрами
     */
    public void setMovies(List<MovieWithGenres> moviesWithGenres) {
        List<Movie> movies = new ArrayList<>();
        if (moviesWithGenres != null) {
            for (MovieWithGenres movieWithGenres : moviesWithGenres) {
                movies.add(movieWithGenres.movie);
            }
        }
        submitList(movies);
    }
} 