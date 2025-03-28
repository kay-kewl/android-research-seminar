package com.example.myapplication.database;

import com.example.myapplication.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TaskDao {
    private static TaskDao instance;
    private final List<Task> tasks;

    private TaskDao() {
        tasks = new ArrayList<>();
        // Add some sample tasks for testing
        tasks.add(new Task("Купить продукты", "Молоко, хлеб, яйца", 2));
        tasks.add(new Task("Позвонить врачу", "Записаться на прием", 3));
        tasks.add(new Task("Оплатить счета", "Интернет и телефон", 2));
        tasks.add(new Task("Прочитать книгу", "Закончить главу 5", 1));
    }

    public static TaskDao getInstance() {
        if (instance == null) {
            instance = new TaskDao();
        }
        return instance;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getCompletedTasks() {
        return tasks.stream()
                .filter(Task::isCompleted)
                .collect(Collectors.toList());
    }

    public List<Task> getTasksSortedByPriority() {
        List<Task> sortedTasks = new ArrayList<>(tasks);
        Collections.sort(sortedTasks, (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
        return sortedTasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void updateTask(Task updatedTask) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(updatedTask.getId())) {
                tasks.set(i, updatedTask);
                break;
            }
        }
    }

    public void deleteTask(String taskId) {
        tasks.removeIf(task -> task.getId().equals(taskId));
    }

    public Task getTaskById(String taskId) {
        for (Task task : tasks) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }
} 