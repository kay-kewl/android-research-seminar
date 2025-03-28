package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Genre;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class ReadOnlyGenreAdapter extends RecyclerView.Adapter<ReadOnlyGenreAdapter.GenreViewHolder> {
    
    private List<Genre> genres = new ArrayList<>();
    
    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre genre = genres.get(position);
        holder.bind(genre);
    }
    
    @Override
    public int getItemCount() {
        return genres.size();
    }
    
    public void setGenres(List<Genre> genres) {
        this.genres = genres;
        notifyDataSetChanged();
    }
    
    public static class GenreViewHolder extends RecyclerView.ViewHolder {
        private final Chip chipGenre;
        
        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            chipGenre = (Chip) itemView;
            // Make the chip non-clickable and non-checkable
            chipGenre.setClickable(false);
            chipGenre.setCheckable(false);
        }
        
        public void bind(Genre genre) {
            chipGenre.setText(genre.getName());
        }
    }
} 