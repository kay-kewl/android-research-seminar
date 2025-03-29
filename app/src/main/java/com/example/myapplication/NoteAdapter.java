package com.example.myapplication;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    
    private Context context;
    private List<Note> noteList;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(Note note, int position);
    }
    
    public NoteAdapter(Context context) {
        this.context = context;
        this.noteList = new ArrayList<>();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setNotes(List<Note> notes) {
        this.noteList = notes;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        
        holder.titleTextView.setText(note.getTitle());
        
        // Ограничиваем отображаемый контент для списка
        String contentPreview = note.getContent();
        if (contentPreview != null) {
            if (contentPreview.length() > 100) {
                contentPreview = contentPreview.substring(0, 97) + "...";
            }
            holder.contentTextView.setText(contentPreview);
        } else {
            holder.contentTextView.setText("");
        }
        
        // Отображаем категорию с соответствующим цветом фона
        String category = note.getCategory();
        holder.categoryTextView.setText(category);
        
        // Создаем фон категории с градиентом в зависимости от категории
        GradientDrawable drawable = (GradientDrawable) holder.categoryTextView.getBackground().mutate();
        int startColor, endColor;
        
        // Выбираем цвета градиента в зависимости от категории
        switch (category.toLowerCase()) {
            case "работа":
                startColor = ContextCompat.getColor(context, R.color.work_start);
                endColor = ContextCompat.getColor(context, R.color.work_end);
                break;
            case "личное":
                startColor = ContextCompat.getColor(context, R.color.personal_start);
                endColor = ContextCompat.getColor(context, R.color.personal_end);
                break;
            case "покупки":
                startColor = ContextCompat.getColor(context, R.color.shopping_start);
                endColor = ContextCompat.getColor(context, R.color.shopping_end);
                break;
            case "идеи":
                startColor = ContextCompat.getColor(context, R.color.ideas_start);
                endColor = ContextCompat.getColor(context, R.color.ideas_end);
                break;
            case "важное":
                startColor = ContextCompat.getColor(context, R.color.important_start);
                endColor = ContextCompat.getColor(context, R.color.important_end);
                break;
            default:
                startColor = ContextCompat.getColor(context, R.color.category_gradient_start);
                endColor = ContextCompat.getColor(context, R.color.category_gradient_end);
                break;
        }
        
        // Устанавливаем цвета градиента
        drawable.setColors(new int[]{startColor, endColor});
        
        // Отображаем дату создания
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        holder.dateTextView.setText(dateFormat.format(note.getDateCreated()));
        
        // Устанавливаем слушатель клика
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(note, position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return noteList.size();
    }
    
    public class NoteViewHolder extends RecyclerView.ViewHolder {
        
        CardView cardView;
        TextView titleTextView;
        TextView contentTextView;
        TextView categoryTextView;
        TextView dateTextView;
        
        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }
    }

    /**
     * Возвращает заметку по позиции в списке
     */
    public Note getNoteAt(int position) {
        if (position >= 0 && position < noteList.size()) {
            return noteList.get(position);
        }
        return null;
    }
} 