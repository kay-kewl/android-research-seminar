package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Review;

public class ReviewAdapter extends ListAdapter<Review, ReviewAdapter.ReviewViewHolder> {

    private final OnReviewClickListener listener;

    public interface OnReviewClickListener {
        void onReviewClick(Review review);
    }

    public ReviewAdapter(OnReviewClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Review> DIFF_CALLBACK = new DiffUtil.ItemCallback<Review>() {
        @Override
        public boolean areItemsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
            return oldItem.getAuthor().equals(newItem.getAuthor()) &&
                    oldItem.getContent().equals(newItem.getContent()) &&
                    oldItem.getRating() == newItem.getRating() &&
                    oldItem.getDate() == newItem.getDate();
        }
    };

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review currentReview = getItem(position);
        holder.bind(currentReview);
    }

    public class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView authorTextView;
        private final TextView dateTextView;
        private final TextView contentTextView;
        private final RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.text_author);
            dateTextView = itemView.findViewById(R.id.text_date);
            contentTextView = itemView.findViewById(R.id.text_content);
            ratingBar = itemView.findViewById(R.id.rating_bar);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onReviewClick(getItem(position));
                }
            });
        }

        public void bind(Review review) {
            authorTextView.setText(review.getAuthor());
            dateTextView.setText(review.getFormattedDate());
            contentTextView.setText(review.getContent());
            ratingBar.setRating(review.getRating());
        }
    }
} 