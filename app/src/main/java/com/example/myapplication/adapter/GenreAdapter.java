package com.example.myapplication.adapter;

import android.content.Context;
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

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
    
    private List<Genre> genres = new ArrayList<>();
    private List<Integer> selectedGenreIds = new ArrayList<>();
    private final OnGenreClickListener listener;
    
    public interface OnGenreClickListener {
        void onGenreClick(Genre genre);
    }
    
    public GenreAdapter(OnGenreClickListener listener) {
        this.listener = listener;
    }
    
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
        holder.bind(genre, selectedGenreIds.contains(genre.getId()));
    }
    
    @Override
    public int getItemCount() {
        return genres.size();
    }
    
    public void setGenres(List<Genre> genres) {
        this.genres = genres;
        notifyDataSetChanged();
    }
    
    public void setSelectedGenreIds(List<Integer> selectedGenreIds) {
        this.selectedGenreIds = selectedGenreIds;
        notifyDataSetChanged();
    }
    
    public List<Integer> getSelectedGenreIds() {
        return selectedGenreIds;
    }
    
    public class GenreViewHolder extends RecyclerView.ViewHolder {
        private final Chip chipGenre;
        
        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            chipGenre = (Chip) itemView;
            
            chipGenre.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Genre genre = genres.get(position);
                    if (selectedGenreIds.contains(genre.getId())) {
                        selectedGenreIds.remove(Integer.valueOf(genre.getId()));
                    } else {
                        selectedGenreIds.add(genre.getId());
                    }
                    chipGenre.setChecked(selectedGenreIds.contains(genre.getId()));
                    listener.onGenreClick(genre);
                }
            });
        }
        
        public void bind(Genre genre, boolean isSelected) {
            chipGenre.setText(genre.getName());
            chipGenre.setChecked(isSelected);
        }
    }
} 