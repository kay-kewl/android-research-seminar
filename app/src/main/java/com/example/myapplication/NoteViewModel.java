package com.example.myapplication;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteViewModel extends AndroidViewModel {
    
    private NoteRepository repository;
    private LiveData<List<Note>> allNotes;
    private ExecutorService executorService;
    
    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
        allNotes = repository.getAllNotes();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
    
    /**
     * Force refreshes all notes from the repository
     */
    public void refreshNotes() {
        executorService.execute(() -> repository.refreshAllNotes());
    }
    
    public LiveData<List<Note>> getNotesByCategory(String category) {
        return repository.getNotesByCategory(category);
    }
    
    public LiveData<Note> getNoteById(long id) {
        return repository.getNoteById(id);
    }
    
    public void insert(Note note) {
        executorService.execute(() -> repository.insert(note));
    }
    
    public void update(Note note) {
        executorService.execute(() -> repository.update(note));
    }
    
    public void delete(Note note) {
        executorService.execute(() -> repository.delete(note));
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 