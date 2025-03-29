package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    
    // Константы базы данных
    private static final String DATABASE_NAME = "notes_db";
    private static final int DATABASE_VERSION = 1;
    
    // Таблица заметок
    private static final String TABLE_NOTES = "notes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DATE_CREATED = "date_created";
    private static final String COLUMN_DATE_MODIFIED = "date_modified";
    
    // Формат даты для сохранения в базе данных
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    // SQL для создания таблицы
    private static final String SQL_CREATE_TABLE_NOTES =
            "CREATE TABLE " + TABLE_NOTES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_DATE_CREATED + " TEXT NOT NULL, " +
                    COLUMN_DATE_MODIFIED + " TEXT NOT NULL" +
                    ")";
    
    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_NOTES);
        Log.d(TAG, "Database created with table: " + TABLE_NOTES);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // В будущих версиях здесь будет код миграции
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
        Log.d(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
    }
    
    /**
     * Добавляет новую заметку в базу данных
     *
     * @param note Заметка для добавления
     * @return ID добавленной заметки или -1 в случае ошибки
     */
    public long addNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        Date currentDate = new Date();
        
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_CATEGORY, note.getCategory());
        values.put(COLUMN_DATE_CREATED, DATE_FORMAT.format(currentDate));
        values.put(COLUMN_DATE_MODIFIED, DATE_FORMAT.format(currentDate));
        
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        
        Log.d(TAG, "Added note: " + note.getTitle() + " with ID: " + id);
        return id;
    }
    
    /**
     * Получает заметку по ID
     *
     * @param id ID заметки
     * @return Заметка или null, если не найдена
     */
    public Note getNoteById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_NOTES,
                new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_CATEGORY, COLUMN_DATE_CREATED, COLUMN_DATE_MODIFIED},
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null, null);
        
        Note note = null;
        
        if (cursor != null && cursor.moveToFirst()) {
            try {
                note = new Note(
                        cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY)),
                        DATE_FORMAT.parse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE_CREATED))),
                        DATE_FORMAT.parse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE_MODIFIED)))
                );
                Log.d(TAG, "Retrieved note: " + note.getTitle() + " (ID: " + id + ")");
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date in getNoteById", e);
            }
            cursor.close();
        } else {
            Log.w(TAG, "Note with ID " + id + " not found");
        }
        
        db.close();
        return note;
    }
    
    /**
     * Получает все заметки из базы данных
     *
     * @return Список всех заметок
     */
    public List<Note> getAllNotes() {
        List<Note> noteList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NOTES + " ORDER BY " + COLUMN_DATE_MODIFIED + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                try {
                    Note note = new Note(
                            cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY)),
                            DATE_FORMAT.parse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE_CREATED))),
                            DATE_FORMAT.parse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE_MODIFIED)))
                    );
                    noteList.add(note);
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date in getAllNotes", e);
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        Log.d(TAG, "Retrieved " + noteList.size() + " notes");
        return noteList;
    }
    
    /**
     * Получает заметки определенной категории
     *
     * @param category Категория для фильтрации
     * @return Список заметок заданной категории
     */
    public List<Note> getNotesByCategory(String category) {
        List<Note> noteList = new ArrayList<>();
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_NOTES,
                new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_CATEGORY, COLUMN_DATE_CREATED, COLUMN_DATE_MODIFIED},
                COLUMN_CATEGORY + "=?",
                new String[]{category},
                null, null,
                COLUMN_DATE_MODIFIED + " DESC",
                null);
        
        if (cursor.moveToFirst()) {
            do {
                try {
                    Note note = new Note(
                            cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY)),
                            DATE_FORMAT.parse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE_CREATED))),
                            DATE_FORMAT.parse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE_MODIFIED)))
                    );
                    noteList.add(note);
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date in getNotesByCategory", e);
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        Log.d(TAG, "Retrieved " + noteList.size() + " notes with category: " + category);
        return noteList;
    }
    
    /**
     * Обновляет существующую заметку
     *
     * @param note Заметка для обновления
     * @return Количество обновленных строк
     */
    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_CATEGORY, note.getCategory());
        values.put(COLUMN_DATE_MODIFIED, DATE_FORMAT.format(new Date()));
        
        int rowsAffected = db.update(
                TABLE_NOTES,
                values,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(note.getId())}
        );
        
        db.close();
        
        Log.d(TAG, "Updated note: " + note.getTitle() + " (ID: " + note.getId() + "), rows affected: " + rowsAffected);
        return rowsAffected;
    }
    
    /**
     * Удаляет заметку по ID
     *
     * @param noteId ID заметки для удаления
     * @return Количество удаленных строк
     */
    public int deleteNote(long noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(
                TABLE_NOTES,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(noteId)}
        );
        
        db.close();
        
        Log.d(TAG, "Deleted note with ID: " + noteId + ", rows affected: " + rowsAffected);
        return rowsAffected;
    }
    
    /**
     * Получает список всех категорий, используемых в заметках
     *
     * @return Список категорий
     */
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String selectQuery = "SELECT DISTINCT " + COLUMN_CATEGORY + " FROM " + TABLE_NOTES + 
                " WHERE " + COLUMN_CATEGORY + " IS NOT NULL AND " + COLUMN_CATEGORY + " != ''";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY)));
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        Log.d(TAG, "Retrieved " + categories.size() + " categories");
        return categories;
    }
} 