package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Genre;

import java.util.ArrayList;
import java.util.List;

public class GenreSelectionAdapter extends RecyclerView.Adapter<GenreSelectionAdapter.GenreViewHolder> {

    private List<Genre> genres = new ArrayList<>();
    private List<Integer> selectedGenreIds = new ArrayList<>();
    private final OnGenreSelectedListener listener;

    public GenreSelectionAdapter(OnGenreSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre_selection, parent, false);
        return new GenreViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre genre = genres.get(position);
        holder.textViewGenreName.setText(genre.getName());
        holder.checkBoxGenre.setChecked(selectedGenreIds.contains(genre.getId()));

        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.checkBoxGenre.isChecked();
            holder.checkBoxGenre.setChecked(newState);
            if (listener != null) {
                listener.onGenreSelected(genre, newState);
            }
        });

        holder.checkBoxGenre.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGenreSelected(genre, holder.checkBoxGenre.isChecked());
            }
        });
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

    static class GenreViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewGenreName;
        private final CheckBox checkBoxGenre;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewGenreName = itemView.findViewById(R.id.text_view_genre_name);
            checkBoxGenre = itemView.findViewById(R.id.checkbox_genre);
        }
    }

    public interface OnGenreSelectedListener {
        void onGenreSelected(Genre genre, boolean isSelected);
    }
} 