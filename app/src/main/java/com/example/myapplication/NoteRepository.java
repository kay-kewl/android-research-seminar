package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для работы с заметками.
 * Предоставляет унифицированный интерфейс для работы с источниками данных.
 */
public class NoteRepository {
    
    private static final String TAG = "NoteRepository";
    private DatabaseHelper dbHelper;
    private MutableLiveData<List<Note>> allNotesLiveData;
    private MutableLiveData<Note> noteLiveData;
    
    public NoteRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
        allNotesLiveData = new MutableLiveData<>();
        noteLiveData = new MutableLiveData<>();
        refreshAllNotes();
    }
    
    /**
     * Обновляет список всех заметок в LiveData
     */
    public void refreshAllNotes() {
        List<Note> notes = dbHelper.getAllNotes();
        Log.d(TAG, "Loaded " + notes.size() + " notes from database");
        allNotesLiveData.postValue(notes);
    }
    
    /**
     * Получает список всех заметок
     *
     * @return LiveData с списком всех заметок
     */
    public LiveData<List<Note>> getAllNotes() {
        // Always refresh the data when asked for all notes to ensure we have the latest data
        List<Note> notes = dbHelper.getAllNotes();
        allNotesLiveData.setValue(notes); // Use setValue for synchronous update
        Log.d(TAG, "getAllNotes: Loaded " + notes.size() + " notes from database");
        return allNotesLiveData;
    }
    
    /**
     * Получает список заметок по категории
     *
     * @param category Категория для фильтрации
     * @return LiveData с списком заметок указанной категории
     */
    public LiveData<List<Note>> getNotesByCategory(String category) {
        MutableLiveData<List<Note>> notesByCategoryLiveData = new MutableLiveData<>();
        
        // Get fresh data from database
        List<Note> notes = dbHelper.getNotesByCategory(category);
        
        Log.d(TAG, "getNotesByCategory: Loaded " + notes.size() + " notes with category: " + category);
        notesByCategoryLiveData.setValue(notes); // Use setValue for synchronous update
        return notesByCategoryLiveData;
    }
    
    /**
     * Получает заметку по ID
     *
     * @param id ID заметки
     * @return LiveData с заметкой
     */
    public LiveData<Note> getNoteById(long id) {
        Note note = dbHelper.getNoteById(id);
        if (note != null) {
            Log.d(TAG, "Loaded note: " + note.getTitle() + " (ID: " + id + ")");
            noteLiveData.postValue(note);
        } else {
            Log.e(TAG, "Note with ID " + id + " not found");
        }
        return noteLiveData;
    }
    
    /**
     * Добавляет новую заметку
     *
     * @param note Заметка для добавления
     */
    public void insert(Note note) {
        long id = dbHelper.addNote(note);
        note.setId(id);
        Log.d(TAG, "Inserted note: " + note.getTitle() + " (ID: " + id + ")");
        refreshAllNotes();
    }
    
    /**
     * Обновляет существующую заметку
     *
     * @param note Заметка для обновления
     */
    public void update(Note note) {
        dbHelper.updateNote(note);
        Log.d(TAG, "Updated note: " + note.getTitle() + " (ID: " + note.getId() + ")");
        refreshAllNotes();
    }
    
    /**
     * Удаляет заметку
     *
     * @param note Заметка для удаления
     */
    public void delete(Note note) {
        dbHelper.deleteNote(note.getId());
        Log.d(TAG, "Deleted note: " + note.getTitle() + " (ID: " + note.getId() + ")");
        refreshAllNotes();
    }
} 