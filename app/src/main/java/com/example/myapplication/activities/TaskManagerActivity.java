package com.example.myapplication.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.database.TaskDao;
import com.example.myapplication.model.Task;
import com.example.myapplication.util.NotificationHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.stream.Collectors;

public class TaskManagerActivity extends AppCompatActivity implements TaskAdapter.TaskItemListener {

    private TaskDao taskDao;
    private TaskAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private NotificationHelper notificationHelper;
    
    private boolean showCompletedOnly = false;
    private boolean sortByPriority = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manager);
        
        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        FloatingActionButton fab = findViewById(R.id.fab);
        
        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);
        
        // Initialize tasks data
        taskDao = TaskDao.getInstance();
        
        // Set up recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this, taskDao.getAllTasks(), this);
        recyclerView.setAdapter(adapter);
        
        // Add new task on FAB click
        fab.setOnClickListener(view -> showAddEditTaskDialog(null));
        
        // Update UI
        updateTaskList();
        
        // Register for context menu
        registerForContextMenu(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        // Set menu item states based on preferences
        MenuItem showCompletedItem = menu.findItem(R.id.action_show_completed);
        showCompletedItem.setChecked(showCompletedOnly);
        
        MenuItem sortPriorityItem = menu.findItem(R.id.action_sort_priority);
        sortPriorityItem.setChecked(sortByPriority);
        
        MenuItem notificationsItem = menu.findItem(R.id.action_toggle_notifications);
        notificationsItem.setChecked(notificationHelper.areNotificationsEnabled());
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_show_completed) {
            showCompletedOnly = !showCompletedOnly;
            item.setChecked(showCompletedOnly);
            updateTaskList();
            return true;
        } else if (id == R.id.action_sort_priority) {
            sortByPriority = !sortByPriority;
            item.setChecked(sortByPriority);
            updateTaskList();
            return true;
        } else if (id == R.id.action_toggle_notifications) {
            boolean enabled = !notificationHelper.areNotificationsEnabled();
            notificationHelper.setNotificationsEnabled(enabled);
            item.setChecked(enabled);
            Toast.makeText(this, 
                    enabled ? R.string.notifications_on : R.string.notifications_off, 
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int position = adapter.getContextMenuPosition();
        if (position < 0) {
            return false;
        }
        
        Task task = adapter.getTaskAtPosition(position);
        if (task == null) {
            return false;
        }
        
        int id = item.getItemId();
        if (id == R.id.action_mark_complete) {
            task.setCompleted(!task.isCompleted());
            taskDao.updateTask(task);
            updateTaskList();
            Toast.makeText(this, R.string.task_completed, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_edit) {
            showAddEditTaskDialog(task);
            return true;
        } else if (id == R.id.action_delete) {
            deleteTask(task);
            return true;
        }
        
        return super.onContextItemSelected(item);
    }

    private void updateTaskList() {
        List<Task> tasks;
        
        // Сначала определяем, какой список задач использовать (отсортированный или нет)
        if (sortByPriority) {
            tasks = taskDao.getTasksSortedByPriority();
        } else {
            tasks = taskDao.getAllTasks();
        }
        
        // Затем фильтруем по статусу выполнения, если нужно
        if (!showCompletedOnly) {
            tasks = tasks.stream()
                    .filter(task -> !task.isCompleted())
                    .collect(Collectors.toList());
        }
        
        adapter.updateTasks(tasks);
        
        // Show empty view if no tasks
        if (tasks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showAddEditTaskDialog(Task task) {
        boolean isEdit = task != null;
        
        // Create dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_edit_task, null);
        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        RadioGroup radioGroupPriority = dialogView.findViewById(R.id.radioGroupPriority);
        
        // Set initial values if editing
        if (isEdit) {
            editTextTitle.setText(task.getTitle());
            editTextDescription.setText(task.getDescription());
            
            // Set priority radio button
            switch (task.getPriority()) {
                case 3:
                    ((RadioButton) radioGroupPriority.findViewById(R.id.radioHigh)).setChecked(true);
                    break;
                case 2:
                    ((RadioButton) radioGroupPriority.findViewById(R.id.radioMedium)).setChecked(true);
                    break;
                default:
                    ((RadioButton) radioGroupPriority.findViewById(R.id.radioLow)).setChecked(true);
                    break;
            }
        }
        
        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEdit ? R.string.task_edit : R.string.task_add)
                .setView(dialogView)
                .setPositiveButton(isEdit ? R.string.task_save : R.string.task_add, null)
                .setNegativeButton(R.string.task_cancel, null)
                .create();
        
        dialog.show();
        
        // Handle positive button click
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();
            
            if (title.isEmpty()) {
                editTextTitle.setError(getString(R.string.empty_title_error));
                return;
            }
            
            // Get priority
            int priority = 1; // Default: Low
            int selectedRadioId = radioGroupPriority.getCheckedRadioButtonId();
            if (selectedRadioId == R.id.radioMedium) {
                priority = 2;
            } else if (selectedRadioId == R.id.radioHigh) {
                priority = 3;
            }
            
            if (isEdit) {
                // Update existing task
                task.setTitle(title);
                task.setDescription(description);
                task.setPriority(priority);
                taskDao.updateTask(task);
            } else {
                // Create new task
                Task newTask = new Task(title, description, priority);
                taskDao.addTask(newTask);
            }
            
            updateTaskList();
            dialog.dismiss();
        });
    }
    
    private void deleteTask(Task task) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.task_delete)
                .setMessage(task.getTitle())
                .setPositiveButton(R.string.task_delete, (dialog, which) -> {
                    taskDao.deleteTask(task.getId());
                    updateTaskList();
                    Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.task_cancel, null)
                .show();
    }

    @Override
    public void onTaskCompleteToggle(Task task, boolean isComplete) {
        task.setCompleted(isComplete);
        taskDao.updateTask(task);
        updateTaskList();
    }

    @Override
    public void onTaskItemClick(Task task) {
        showAddEditTaskDialog(task);
    }
}