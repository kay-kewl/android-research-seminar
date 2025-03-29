package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class NoteViewActivity extends AppCompatActivity {
    
    private TextView titleTextView;
    private TextView contentTextView;
    private TextView categoryTextView;
    private TextView dateTextView;
    
    private DatabaseHelper dbHelper;
    private Note currentNote;
    private long noteId = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        
        // Настройка Toolbar в качестве ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Просмотр заметки");
            }
        }
        
        // Инициализация элементов UI
        titleTextView = findViewById(R.id.titleTextView);
        contentTextView = findViewById(R.id.contentTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        dateTextView = findViewById(R.id.dateTextView);
        
        // Инициализация базы данных
        dbHelper = new DatabaseHelper(this);
        
        // Получение ID заметки из интента
        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            if (noteId != -1) {
                currentNote = dbHelper.getNoteById(noteId);
                if (currentNote != null) {
                    titleTextView.setText(currentNote.getTitle());
                    contentTextView.setText(currentNote.getContent());
                    categoryTextView.setText(currentNote.getCategory());
                    
                    // Форматирование даты
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    dateTextView.setText(sdf.format(currentNote.getDateCreated()));
                }
            }
        } else {
            Toast.makeText(this, "Ошибка: ID заметки не найден", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_view_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            // Обработка нажатия кнопки "Назад"
            finish();
            return true;
        } else if (id == R.id.action_edit) {
            // Переход к редактированию заметки
            Intent intent = new Intent(this, NoteDetailActivity.class);
            intent.putExtra("note_id", noteId);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_share) {
            // Отправка заметки через другие приложения
            shareNote();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void shareNote() {
        if (currentNote != null) {
            String shareText = String.format(
                    "Заголовок: %s\n\nКатегория: %s\n\n%s",
                    currentNote.getTitle(),
                    currentNote.getCategory(),
                    currentNote.getContent()
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentNote.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Поделиться заметкой"));
        }
    }
} 