package com.example.myapplication;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView themeRecyclerView;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "ThemePrefs";
    private static final String SELECTED_THEME = "selected_theme";
    private int currentThemePosition;

    private final List<ThemeItem> themeItems = Arrays.asList(
            new ThemeItem("Default", R.style.Theme_MyApplication, R.color.purple_500),
            new ThemeItem("Ocean", R.style.Theme_MyApplication_Ocean, R.color.ocean_primary),
            new ThemeItem("Sunset", R.style.Theme_MyApplication_Sunset, R.color.sunset_primary)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentThemePosition = preferences.getInt(SELECTED_THEME, 0);
        setTheme(themeItems.get(currentThemePosition).themeResId);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        standardizePreviewElements();
        setupThemeSelector();
    }

    private void setupThemeSelector() {
        themeRecyclerView = findViewById(R.id.themeRecyclerView);
        
        ThemeAdapter adapter = new ThemeAdapter(themeItems, currentThemePosition, position -> {
            applyTheme(position);
        });
        
        themeRecyclerView.setAdapter(adapter);
        
        themeRecyclerView.scrollToPosition(currentThemePosition);
    }

    private void standardizePreviewElements() {
        standardizeButtons();
        standardizeCardAndText();
    }

    private void standardizeButtons() {
        MaterialButton primaryButton = findViewById(R.id.primaryButton);
        MaterialButton secondaryButton = findViewById(R.id.secondaryButton);
        
        int buttonHeight = dpToPx(48);
        
        ViewGroup.LayoutParams primaryParams = primaryButton.getLayoutParams();
        primaryParams.height = buttonHeight;
        primaryButton.setLayoutParams(primaryParams);
        
        ViewGroup.LayoutParams secondaryParams = secondaryButton.getLayoutParams();
        secondaryParams.height = buttonHeight;
        secondaryButton.setLayoutParams(secondaryParams);
        
        int paddingHorizontal = dpToPx(16);
        int paddingVertical = dpToPx(8);
        
        primaryButton.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        secondaryButton.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        
        primaryButton.setTextSize(14);
        secondaryButton.setTextSize(14);
    }
    
    private void standardizeCardAndText() {
        MaterialCardView cardView = findViewById(R.id.cardView);
                
        if (cardView != null) {
            cardView.setCardElevation(dpToPx(2));
            cardView.setCardBackgroundColor(Color.WHITE);
            cardView.setRadius(dpToPx(4));
            
            cardView.setStrokeColor(Color.parseColor("#DDDDDD"));
            cardView.setStrokeWidth(1);
            cardView.setRippleColor(null);
            
            LinearLayout cardContent = (LinearLayout) cardView.getChildAt(0);
            if (cardContent != null) {
                cardContent.setBackgroundColor(Color.WHITE);
                cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                
                for (int i = 0; i < cardContent.getChildCount(); i++) {
                    View child = cardContent.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView textView = (TextView) child;
                        
                        textView.setTextColor(Color.BLACK);
                        
                        if (textView.getId() == R.id.cardTitle) {
                            textView.setTextSize(18);
                            textView.setTypeface(Typeface.DEFAULT_BOLD);
                            textView.setAllCaps(false);
                            textView.setLetterSpacing(0);
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
                            params.bottomMargin = dpToPx(8);
                            textView.setLayoutParams(params);
                        } else {
                            textView.setTextSize(14);
                            textView.setTypeface(Typeface.DEFAULT);
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
                            params.topMargin = dpToPx(8);
                            textView.setLayoutParams(params);
                        }
                    }
                }
            }
            
            TextView cardTitle = cardView.findViewById(R.id.cardTitle);
            if (cardTitle != null) {
                cardTitle.setTextSize(18);
                cardTitle.setTextColor(Color.BLACK);
                cardTitle.setTypeface(Typeface.DEFAULT_BOLD);
                cardTitle.setAllCaps(false);
                cardTitle.setLetterSpacing(0);
                cardTitle.setLineSpacing(0, 1.0f);
            }
        }
        
        TextView headlineText = findViewById(R.id.headlineText);
        TextView bodyText = findViewById(R.id.bodyText);
        
        if (headlineText != null) {
            headlineText.setTextSize(20);
            headlineText.setTextColor(Color.BLACK);
            headlineText.setTypeface(Typeface.DEFAULT_BOLD);
        }
        
        if (bodyText != null) {
            bodyText.setTextSize(14);
            bodyText.setTextColor(Color.BLACK);
            bodyText.setTypeface(Typeface.DEFAULT);
        }
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    private void applyTheme(int position) {
        if (position != currentThemePosition) {
            preferences.edit().putInt(SELECTED_THEME, position).apply();
            
            recreate();
        }
    }

    private static class ThemeItem {
        final String name;
        final int themeResId;
        final int colorResId;

        ThemeItem(String name, int themeResId, int colorResId) {
            this.name = name;
            this.themeResId = themeResId;
            this.colorResId = colorResId;
        }
    }

    interface OnThemeClickListener {
        void onThemeClick(int position);
    }

    private class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {
        private final List<ThemeItem> items;
        private final int currentThemePosition;
        private final OnThemeClickListener listener;

        ThemeAdapter(List<ThemeItem> items, int initialPosition, OnThemeClickListener listener) {
            this.items = items;
            this.currentThemePosition = initialPosition;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_theme, parent, false);
            return new ThemeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
            ThemeItem item = items.get(position);
            
            holder.themeName.setText(item.name);
            holder.themeName.setTextColor(Color.BLACK);
            
            holder.themeColorPreview.setBackgroundResource(R.drawable.circle_shape);
            int themeColor = getResources().getColor(item.colorResId, getTheme());
            holder.themeColorPreview.getBackground().setTint(themeColor);
            
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            
            if (isSelected(position)) {
                holder.cardView.setStrokeWidth(4);
                holder.cardView.setStrokeColor(themeColor);
            } else {
                holder.cardView.setStrokeWidth(0);
                holder.cardView.setStrokeColor(0);
            }
            
            holder.cardView.setOnClickListener(v -> {
                listener.onThemeClick(position);
            });
            
            ViewGroup.LayoutParams layoutParams = holder.themeColorPreview.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
                marginParams.bottomMargin = dpToPx(8);
                holder.themeColorPreview.setLayoutParams(marginParams);
            }
        }
        
        private boolean isSelected(int position) {
            return position == currentThemePosition;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ThemeViewHolder extends RecyclerView.ViewHolder {
            final TextView themeName;
            final ImageView themeColorPreview;
            final MaterialCardView cardView;

            ThemeViewHolder(@NonNull View itemView) {
                super(itemView);
                themeName = itemView.findViewById(R.id.themeName);
                themeColorPreview = itemView.findViewById(R.id.themeColorPreview);
                cardView = (MaterialCardView) itemView;
            }
        }
    }
}