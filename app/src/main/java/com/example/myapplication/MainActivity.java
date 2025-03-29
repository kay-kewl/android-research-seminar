package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.Gravity;
import java.util.List;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.app.AlertDialog;
import android.widget.Toast;
import java.util.ArrayList;
import android.content.res.Configuration;
import android.content.res.ColorStateList;

public class MainActivity extends AppCompatActivity {
    
    private static final int NEW_NOTE_REQUEST = 1;
    private static final int VIEW_NOTE_REQUEST = 2;
    private static final int SETTINGS_REQUEST = 3;
    
    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private NoteViewModel noteViewModel;
    private ChipGroup categoryChipGroup;
    private TextView emptyView;
    private TextView emptyInstructionView;
    
    private static final String PREFS_NAME = "NoteAppPrefs";
    private static final String LAST_CATEGORY = "LastCategory";
    private static final String THEME_MODE = "ThemeMode";
    private static final String APP_COLOR = "AppColor";
    private static final String FONT_SIZE = "FontSize";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Инициализация SharedPreferences и настройка темы
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        applySettings();
        
        setContentView(R.layout.activity_main);
        
        // Настройка Toolbar в качестве ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.app_name);
            }
            
            // Применение цветовой схемы к toolbar
            applyColorToToolbar(toolbar);
        }
        
        // Инициализация базы данных
        dbHelper = new DatabaseHelper(this);
        
        // Настройка RecyclerView для отображения заметок
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Инициализируем адаптер
        noteAdapter = new NoteAdapter(this);
        recyclerView.setAdapter(noteAdapter);
        
        // Настройка свайпа для удаления
        setupSwipeToDelete();
        
        // Инициализируем ViewModel
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        
        // Находим пустые view
        emptyView = findViewById(R.id.emptyView);
        emptyInstructionView = findViewById(R.id.emptyInstructionView);
        
        // Инициализируем группу чипов для категорий
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        setupCategoryChips();
        
        // Наблюдаем за изменениями в данных
        noteViewModel.getAllNotes().observe(this, notes -> {
            updateEmptyView(notes);
            noteAdapter.setNotes(notes);
        });
        
        // Обрабатываем клики на заметки
        noteAdapter.setOnItemClickListener((note, position) -> {
            Intent intent = new Intent(MainActivity.this, NoteViewActivity.class);
            intent.putExtra("note_id", note.getId());
            startActivityForResult(intent, VIEW_NOTE_REQUEST);
        });
        
        // Настройка FAB для добавления новой заметки
        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
            startActivityForResult(intent, NEW_NOTE_REQUEST);
        });
        
        // Apply color to FAB
        int colorResId = sharedPreferences.getInt(APP_COLOR, R.color.purple_500);
        int color = ContextCompat.getColor(this, colorResId);
        fab.setBackgroundTintList(ColorStateList.valueOf(color));
    }
    
    // Применить сохраненные настройки
    private void applySettings() {
        // Применение темы
        int themeMode = sharedPreferences.getInt(THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        // Применение размера шрифта
        int fontSize = sharedPreferences.getInt(FONT_SIZE, 16);
        setThemeWithFontSize(fontSize);
    }
    
    // Метод для установки размера шрифта
    private void setThemeWithFontSize(int fontSize) {
        Resources res = getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        
        // Установить размер шрифта
        float fontScale = fontSize / 16f;
        Configuration configuration = new Configuration(res.getConfiguration());
        configuration.fontScale = fontScale;
        metrics.scaledDensity = fontScale * metrics.density;
        
        // Применяем изменения в конфигурации
        res.updateConfiguration(configuration, metrics);
        
        // Force update the application context's resources
        getApplicationContext().getResources().updateConfiguration(configuration, metrics);
    }
    
    // Применить цветовую схему к ToolBar
    private void applyColorToToolbar(Toolbar toolbar) {
        int colorResId = sharedPreferences.getInt(APP_COLOR, R.color.purple_500);
        int color = ContextCompat.getColor(this, colorResId);
        toolbar.setBackgroundColor(color);
        
        // Также обновить цвет строки состояния
        getWindow().setStatusBarColor(color);
        
        // Применить цвет к FAB
        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем списки заметок - принудительно
        noteViewModel.refreshNotes();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // Убедиться, что в меню правильные цвета в зависимости от темы
        boolean isDarkTheme = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        
        if (settingsItem != null && isDarkTheme) {
            // В тёмной теме убедиться, что текст белый
            settingsItem.setTitleCondensed("Настройки");
            settingsItem.setTitle("Настройки");
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            // Открытие настроек без закрытия текущей активности
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SETTINGS_REQUEST) {
            // Check if settings were changed
            boolean fontSizeChanged = sharedPreferences.getBoolean("font_size_changed", false);
            boolean colorChanged = sharedPreferences.getBoolean("color_changed", false);
            boolean themeChanged = sharedPreferences.getBoolean("theme_changed", false);
            
            // Apply settings
            applySettings();
            
            // Update UI elements with new settings
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                applyColorToToolbar(toolbar);
            }
            
            // Refresh notes
            noteViewModel.getAllNotes();
            
            // Update menu
            invalidateOptionsMenu();
            
            // Clean up flags
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("font_size_changed", false);
            editor.putBoolean("color_changed", false);
            editor.putBoolean("theme_changed", false);
            editor.apply();
            
            // If font size or theme changed, recreate activity to fully apply changes
            if (fontSizeChanged || themeChanged) {
                recreate();
            }
        } else if (resultCode == RESULT_OK) {
            if (requestCode == NEW_NOTE_REQUEST || requestCode == VIEW_NOTE_REQUEST) {
                // Update data after returning from activities
                setupCategoryChips();
                
                // Force refresh the notes list
                noteViewModel.refreshNotes();
            }
        }
    }
    
    /**
     * Настраивает чипы категорий для фильтрации
     */
    private void setupCategoryChips() {
        // Очищаем группу чипов
        categoryChipGroup.removeAllViews();
        
        // Получаем все категории из базы данных
        List<String> dbCategories = dbHelper.getAllCategories();
        
        // Базовые категории
        String[] baseCategories = {"Работа", "Личное", "Покупки", "Идеи", "Важное"};
        List<String> allCategories = new ArrayList<>();
        
        // Добавляем вначале чип "Все" для сброса фильтров
        Chip allChip = new Chip(this);
        allChip.setText(R.string.all_categories);
        allChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setChipBackgroundColorResource(R.color.category_gradient_start);
        allChip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        categoryChipGroup.addView(allChip);
        
        // Добавляем базовые категории в список
        for (String baseCategory : baseCategories) {
            if (!allCategories.contains(baseCategory)) {
                allCategories.add(baseCategory);
            }
        }
        
        // Добавляем пользовательские категории из базы данных
        for (String dbCategory : dbCategories) {
            if (!allCategories.contains(dbCategory)) {
                allCategories.add(dbCategory);
            }
        }
        
        // Создаем чипы для всех категорий
        for (String category : allCategories) {
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
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            
            categoryChipGroup.addView(chip);
        }
        
        // Настраиваем группу чипов как группу с одиночным выбором
        categoryChipGroup.setSingleSelection(true);
        
        // Настраиваем слушатель для фильтрации заметок
        categoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                // Если ничего не выбрано, выбираем "Все"
                allChip.setChecked(true);
                return;
            }
            
            Chip selectedChip = findViewById(checkedId);
            if (selectedChip != null) {
                String category = selectedChip.getText().toString();
                
                if (category.equals(getString(R.string.all_categories))) {
                    // Показываем все заметки - принудительно обновляем их
                    noteViewModel.refreshNotes();
                    
                    // И наблюдаем за изменениями
                    noteViewModel.getAllNotes().observe(this, notes -> {
                        updateEmptyView(notes);
                        noteAdapter.setNotes(notes);
                    });
                } else {
                    // Фильтруем по выбранной категории
                    noteViewModel.getNotesByCategory(category).observe(this, notes -> {
                        updateEmptyView(notes);
                        noteAdapter.setNotes(notes);
                    });
                }
            }
        });
    }
    
    /**
     * Обновляет видимость пустого представления
     */
    private void updateEmptyView(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            emptyInstructionView.setVisibility(View.VISIBLE);
            findViewById(R.id.recyclerView).setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            emptyInstructionView.setVisibility(View.GONE);
            findViewById(R.id.recyclerView).setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Исправляет позиционирование FAB при изменении темы
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        
        // Проверяем, что FAB правильно прикреплен
        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        if (fab != null) {
            // Устанавливаем позицию FAB в правом нижнем углу
            CoordinatorLayout.LayoutParams params = 
                (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
            
            // Очищаем якорь, если он был установлен
            params.setAnchorId(View.NO_ID);
            
            // Устанавливаем позицию внизу справа
            params.gravity = Gravity.BOTTOM | Gravity.END;
            
            // Устанавливаем отступы
            params.setMargins(0, 0, 
                getResources().getDimensionPixelSize(R.dimen.fab_margin), 
                getResources().getDimensionPixelSize(R.dimen.fab_margin));
            
            // Применяем параметры
            fab.setLayoutParams(params);
            
            // Обновляем вид
            fab.requestLayout();
        }
    }
    
    /**
     * Настраивает свайп для удаления элементов списка
     */
    private void setupSwipeToDelete() {
        // Создаем колбэк для свайпа
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            
            // Для обработки перетаскивания (не используется)
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, 
                    RecyclerView.ViewHolder target) {
                return false;
            }
            
            // Обработка свайпа
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Note noteToDelete = noteAdapter.getNoteAt(position);
                
                // Показываем диалог подтверждения
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Удаление заметки")
                        .setMessage("Вы уверены, что хотите удалить эту заметку?")
                        .setPositiveButton("Удалить", (dialog, which) -> {
                            // Удаляем заметку через ViewModel
                            if (noteToDelete != null) {
                                noteViewModel.delete(noteToDelete);
                                
                                // Принудительно обновляем данные
                                noteViewModel.refreshNotes();
                                
                                // Показываем сообщение об успешном удалении
                                Toast.makeText(MainActivity.this, 
                                        "Заметка удалена", Toast.LENGTH_SHORT).show();
                                
                                // Обновляем чипы категорий
                                setupCategoryChips();
                            }
                        })
                        .setNegativeButton("Отмена", (dialog, which) -> {
                            // Отменяем удаление и возвращаем элемент
                            noteAdapter.notifyItemChanged(position);
                        })
                        .setCancelable(false)
                        .show();
            }
            
            // Настройка визуального оформления свайпа
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    
                    // Цвет фона при свайпе
                    Paint paint = new Paint();
                    paint.setColor(ContextCompat.getColor(MainActivity.this, R.color.delete_background));
                    
                    // Иконка удаления
                    Drawable deleteIcon = ContextCompat.getDrawable(MainActivity.this, 
                            android.R.drawable.ic_menu_delete);
                    
                    // Центрирование иконки
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    
                    // Свайп вправо
                    if (dX > 0) {
                        // Фон
                        c.drawRect(itemView.getLeft(), itemView.getTop(), 
                                itemView.getLeft() + dX, itemView.getBottom(), paint);
                        
                        // Иконка
                        int iconLeft = itemView.getLeft() + iconMargin;
                        int iconRight = itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth();
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    } 
                    // Свайп влево
                    else if (dX < 0) {
                        // Фон
                        c.drawRect(itemView.getRight() + dX, itemView.getTop(), 
                                itemView.getRight(), itemView.getBottom(), paint);
                        
                        // Иконка
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    }
                    
                    // Отрисовка иконки
                    deleteIcon.draw(c);
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        
        // Подключаем ItemTouchHelper к RecyclerView
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }
} 