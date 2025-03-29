package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Date;
import java.util.List;

public class NoteDetailActivity extends AppCompatActivity {
    
    private TextInputEditText titleEditText;
    private TextInputEditText contentEditText;
    private TextInputEditText categoryEditText;
    private TextInputLayout categoryInputLayout;
    private ChipGroup categoryChipGroup;
    private FloatingActionButton fabSave;
    
    private DatabaseHelper dbHelper;
    private Note currentNote;
    private long noteId = -1;
    private boolean isNewNote = true;
    private String selectedCategory = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        
        // Инициализация SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("NoteAppPrefs", MODE_PRIVATE);
        
        // Настройка Toolbar в качестве ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            
            // Применяем цвет к тулбару
            int colorResId = sharedPreferences.getInt("AppColor", R.color.purple_500);
            int color = ContextCompat.getColor(this, colorResId);
            toolbar.setBackgroundColor(color);
            getWindow().setStatusBarColor(color);
        }
        
        // Инициализация компонентов UI
        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        categoryInputLayout = findViewById(R.id.categoryInputLayout);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        fabSave = findViewById(R.id.fabSave);
        
        // Apply color to FAB
        int colorResId = sharedPreferences.getInt("AppColor", R.color.purple_500);
        int color = ContextCompat.getColor(this, colorResId);
        fabSave.setBackgroundTintList(ColorStateList.valueOf(color));
        
        // Инициализация помощника базы данных
        dbHelper = new DatabaseHelper(this);
        
        // Настройка кнопки сохранения
        fabSave.setOnClickListener(v -> saveNote());
        
        // Настройка категорий
        setupCategoryChips();
        
        // Проверка, редактируем ли существующую заметку
        long id = getIntent().getLongExtra("note_id", -1);
        if (id != -1) {
            isNewNote = false;
            noteId = id;
            loadNote();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Редактирование заметки");
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Новая заметка");
            }
        }
    }
    
    /**
     * Настраивает чипы для выбора категорий
     */
    private void setupCategoryChips() {
        // Очищаем группу чипов
        categoryChipGroup.removeAllViews();
        
        // Получаем все доступные категории из базы
        List<String> categories = dbHelper.getAllCategories();
        
        // Базовые категории, которые всегда должны быть доступны
        String[] baseCategories = {"Работа", "Личное", "Покупки", "Идеи", "Важное"};
        
        // Добавляем базовые категории, если их нет в списке
        for (String baseCategory : baseCategories) {
            if (!categories.contains(baseCategory)) {
                categories.add(baseCategory);
            }
        }
        
        // Создаем чипы для каждой категории
        for (String category : categories) {
            addCategoryChip(category);
        }
        
        // Добавляем флаг для предотвращения рекурсивных вызовов
        final boolean[] isUserInput = {true};
        
        // Настройка фильтрации категорий при вводе текста
        categoryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не требуется реализация
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Пропускаем, если это программное изменение, а не пользовательский ввод
                if (!isUserInput[0]) return;
                
                String input = s.toString().trim().toLowerCase();
                
                // Сбрасываем выбор чипа, если текст изменился
                if (s.length() > 0 && categoryChipGroup.getCheckedChipId() != View.NO_ID) {
                    categoryChipGroup.clearCheck();
                    selectedCategory = "";
                }
                
                // Если вводится новая категория, использовать ее
                if (s.length() > 0) {
                    selectedCategory = s.toString().trim();
                    
                    // Фильтруем и отображаем только подходящие категории
                    categoryChipGroup.removeAllViews();
                    boolean foundMatches = false;
                    
                    for (String category : categories) {
                        if (category.toLowerCase().contains(input)) {
                            addCategoryChip(category);
                            foundMatches = true;
                        }
                    }
                    
                    // Если нет совпадений, показываем подсказку о создании новой категории
                    if (!foundMatches && s.length() > 0) {
                        Chip newCategoryChip = new Chip(NoteDetailActivity.this);
                        newCategoryChip.setText("Создать категорию \"" + input + "\"");
                        newCategoryChip.setCheckable(true);
                        newCategoryChip.setChipIcon(ContextCompat.getDrawable(NoteDetailActivity.this, 
                                android.R.drawable.ic_menu_add));
                        newCategoryChip.setChipBackgroundColorResource(R.color.category_gradient_start);
                        newCategoryChip.setTextColor(ContextCompat.getColor(NoteDetailActivity.this, 
                                android.R.color.white));
                        
                        // При выборе этого чипа устанавливаем новую категорию
                        newCategoryChip.setOnClickListener(v -> {
                            selectedCategory = input;
                            isUserInput[0] = false;  // Предотвращаем рекурсию
                            categoryEditText.setText(input);
                            categoryEditText.setSelection(input.length());
                            isUserInput[0] = true;  // Восстанавливаем флаг
                        });
                        
                        categoryChipGroup.addView(newCategoryChip);
                    }
                } else {
                    // Если поле пустое, отображаем все категории
                    categoryChipGroup.removeAllViews();
                    for (String category : categories) {
                        addCategoryChip(category);
                    }
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Не требуется реализация
            }
        });
        
        // Настраиваем слушатель для выбора категории
        categoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip selectedChip = findViewById(checkedId);
                if (selectedChip != null) {
                    String chipText = selectedChip.getText().toString();
                    
                    // Проверяем, не чип ли это "Создать категорию"
                    if (chipText.startsWith("Создать категорию")) {
                        String newCategory = selectedCategory;
                        isUserInput[0] = false;  // Предотвращаем рекурсию
                        categoryEditText.setText(newCategory);
                        categoryEditText.setSelection(newCategory.length());
                        isUserInput[0] = true;  // Восстанавливаем флаг
                    } else {
                        selectedCategory = chipText;
                        isUserInput[0] = false;  // Предотвращаем рекурсию
                        categoryEditText.setText(chipText);
                        categoryEditText.setSelection(chipText.length());
                        isUserInput[0] = true;  // Восстанавливаем флаг
                    }
                }
            }
        });
    }
    
    /**
     * Добавляет чип категории в группу
     */
    private void addCategoryChip(String category) {
        Chip chip = new Chip(this);
        chip.setText(category);
        chip.setCheckable(true);
        
        // Устанавливаем цвет чипа в зависимости от категории
        int colorResId;
        switch (category.toLowerCase()) {
            case "работа":
                colorResId = R.color.work_start;
                break;
            case "личное":
                colorResId = R.color.personal_start;
                break;
            case "покупки":
                colorResId = R.color.shopping_start;
                break;
            case "идеи":
                colorResId = R.color.ideas_start;
                break;
            case "важное":
                colorResId = R.color.important_start;
                break;
            default:
                colorResId = R.color.category_gradient_start;
                break;
        }
        chip.setChipBackgroundColorResource(colorResId);
        
        // Устанавливаем текст белым цветом
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        
        // Добавляем чип в группу
        categoryChipGroup.addView(chip);
        
        // Выбираем чип, если он соответствует текущей категории
        if (category.equals(selectedCategory)) {
            // Устанавливаем checked без вызова слушателей
            chip.setChecked(true);
        }
    }
    
    /**
     * Загружает заметку для редактирования
     */
    private void loadNote() {
        currentNote = dbHelper.getNoteById(noteId);
        if (currentNote != null) {
            titleEditText.setText(currentNote.getTitle());
            contentEditText.setText(currentNote.getContent());
            
            // Выбор соответствующего чипа категории
            String category = currentNote.getCategory();
            selectedCategory = category;
            
            // Ищем соответствующий чип и отмечаем его
            boolean foundChip = false;
            for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) categoryChipGroup.getChildAt(i);
                if (chip.getText().toString().equals(category)) {
                    chip.setChecked(true);
                    foundChip = true;
                    break;
                }
            }
            
            // Если чип не найден, добавляем новый
            if (!foundChip && !category.isEmpty()) {
                categoryEditText.setText(category);
            }
        }
    }
    
    /**
     * Сохраняет заметку и закрывает активность
     */
    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String category = selectedCategory;
        
        // Если в поле категории что-то введено, используем это
        if (categoryEditText.getText() != null && !categoryEditText.getText().toString().trim().isEmpty()) {
            category = categoryEditText.getText().toString().trim();
        }
        
        // Проверяем, что заголовок не пустой
        if (title.isEmpty()) {
            titleEditText.setError("Пожалуйста, введите заголовок");
            titleEditText.requestFocus();
            return;
        }
        
        // Проверяем, что категория не пустая
        if (category.isEmpty()) {
            categoryInputLayout.setError("Пожалуйста, укажите категорию");
            categoryEditText.requestFocus();
            return;
        } else {
            categoryInputLayout.setError(null);
        }
        
        if (isNewNote) {
            // Создаем новую заметку
            Note note = new Note(title, content, category);
            long id = dbHelper.addNote(note);
            if (id > 0) {
                Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Ошибка при сохранении заметки", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Обновляем существующую заметку
            currentNote.setTitle(title);
            currentNote.setContent(content);
            currentNote.setCategory(category);
            currentNote.updateModifiedDate();
            
            int result = dbHelper.updateNote(currentNote);
            if (result > 0) {
                Toast.makeText(this, "Заметка обновлена", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Ошибка при обновлении заметки", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_detail_menu, menu);
        // Скрываем кнопку удаления для новой заметки
        menu.findItem(R.id.action_delete).setVisible(!isNewNote);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            // Обработка нажатия кнопки "Назад"
            checkUnsavedChanges();
            return true;
        } else if (id == R.id.action_delete) {
            // Обработка нажатия кнопки "Удалить"
            confirmDelete();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Показывает диалог подтверждения удаления
     */
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление заметки")
                .setMessage("Вы уверены, что хотите удалить эту заметку?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteNote();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    /**
     * Удаляет заметку
     */
    private void deleteNote() {
        if (noteId != -1) {
            dbHelper.deleteNote(noteId);
            Toast.makeText(this, "Заметка удалена", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }
    
    /**
     * Проверяет наличие несохраненных изменений перед закрытием
     */
    private void checkUnsavedChanges() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String category = selectedCategory;
        
        // Если в поле категории что-то введено, используем это
        if (categoryEditText.getText() != null && !categoryEditText.getText().toString().trim().isEmpty()) {
            category = categoryEditText.getText().toString().trim();
        }
        
        boolean hasChanges = isNewNote 
                ? !title.isEmpty() || !content.isEmpty() || !category.isEmpty()
                : !title.equals(currentNote.getTitle()) 
                    || !content.equals(currentNote.getContent()) 
                    || !category.equals(currentNote.getCategory());
        
        if (hasChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("Сохранение изменений")
                    .setMessage("Хотите сохранить изменения?")
                    .setPositiveButton("Сохранить", (dialog, which) -> {
                        saveNote();
                    })
                    .setNegativeButton("Не сохранять", (dialog, which) -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .setNeutralButton("Отмена", null)
                    .show();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
    
    @Override
    public void onBackPressed() {
        checkUnsavedChanges();
    }
} 