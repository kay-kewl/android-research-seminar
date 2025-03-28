package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> taskList;
    private final Context context;
    private int contextMenuPosition = -1;
    private final TaskItemListener listener;

    public interface TaskItemListener {
        void onTaskCompleteToggle(Task task, boolean isComplete);
        void onTaskItemClick(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList, TaskItemListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    public int getContextMenuPosition() {
        return contextMenuPosition;
    }

    public Task getTaskAtPosition(int position) {
        if (position >= 0 && position < taskList.size()) {
            return taskList.get(position);
        }
        return null;
    }

    class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private final TextView textTitle;
        private final TextView textDescription;
        private final CheckBox checkBoxComplete;
        private final ImageView imagePriority;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            checkBoxComplete = itemView.findViewById(R.id.checkboxComplete);
            imagePriority = itemView.findViewById(R.id.imagePriority);

            itemView.setOnCreateContextMenuListener(this);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTaskItemClick(taskList.get(position));
                }
            });

            checkBoxComplete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    boolean isChecked = checkBoxComplete.isChecked();
                    listener.onTaskCompleteToggle(taskList.get(position), isChecked);
                    updateTextAppearance(isChecked);
                }
            });
        }

        void bind(Task task) {
            textTitle.setText(task.getTitle());
            textDescription.setText(task.getDescription());
            checkBoxComplete.setChecked(task.isCompleted());
            updateTextAppearance(task.isCompleted());
            
            // Set priority icon based on task priority
            switch (task.getPriority()) {
                case 3: // High
                    imagePriority.setImageResource(R.drawable.ic_priority_high);
                    imagePriority.setColorFilter(context.getResources().getColor(R.color.priority_high, null));
                    break;
                case 2: // Medium
                    imagePriority.setImageResource(R.drawable.ic_priority_high);
                    imagePriority.setColorFilter(context.getResources().getColor(R.color.priority_medium, null));
                    break;
                default: // Low
                    imagePriority.setImageResource(R.drawable.ic_priority_high);
                    imagePriority.setColorFilter(context.getResources().getColor(R.color.priority_low, null));
                    break;
            }
            imagePriority.setAlpha(1.0f);
        }
        
        private void updateTextAppearance(boolean isCompleted) {
            if (isCompleted) {
                textTitle.setPaintFlags(textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textTitle.setAlpha(0.5f);
                textDescription.setAlpha(0.5f);
            } else {
                textTitle.setPaintFlags(textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                textTitle.setAlpha(1.0f);
                textDescription.setAlpha(1.0f);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            contextMenuPosition = getAdapterPosition();
            v.setSelected(true);
            menu.add(0, R.id.action_mark_complete, 0, context.getString(R.string.context_menu_mark_complete));
            menu.add(0, R.id.action_edit, 1, context.getString(R.string.context_menu_edit));
            menu.add(0, R.id.action_delete, 2, context.getString(R.string.context_menu_delete));
        }
    }
} 