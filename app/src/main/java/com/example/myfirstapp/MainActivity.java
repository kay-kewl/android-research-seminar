package com.example.myfirstapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myfirstapp.model.Chat;
import com.example.myfirstapp.model.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference chatsRef, usersRef;
    private ValueEventListener chatsListener, usersListener;
    
    private RecyclerView chatListRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList, filteredChatList;
    private Map<String, User> userMap;
    private ChipGroup filterChipGroup;
    private SearchView searchView;
    private String currentFilter = "all"; // "all", "pinned", "archived"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        // Check if user is signed in
        if (currentUser == null) {
            // Not signed in, launch the Login activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Initialize Firebase Database with correct region URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://chat-app-a8ac0-default-rtdb.europe-west1.firebasedatabase.app");
        chatsRef = database.getReference("chats");
        usersRef = database.getReference("users");
        
        // Update online status
        updateUserOnlineStatus(true);
        
        // Get and register FCM token for push notifications
        registerFcmToken();
        
        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Set up filter chips
        filterChipGroup = findViewById(R.id.filter_chip_group);
        setupFilterChips();
        
        // Initialize RecyclerView
        chatListRecyclerView = findViewById(R.id.rvChatList);
        chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Add divider between items
        DividerItemDecoration divider = new DividerItemDecoration(
                chatListRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        chatListRecyclerView.addItemDecoration(divider);
        
        chatList = new ArrayList<>();
        filteredChatList = new ArrayList<>();
        userMap = new HashMap<>();
        
        chatAdapter = new ChatAdapter(filteredChatList, userMap, currentUser.getUid());
        chatListRecyclerView.setAdapter(chatAdapter);
        
        // Setup swipe actions
        setupSwipeActions();
        
        // Set up the FloatingActionButton
        FloatingActionButton fabNewChat = findViewById(R.id.fabNewChat);
        fabNewChat.setOnClickListener(v -> showNewChatDialog());
        
        // Setup search
        searchView = findViewById(R.id.search_view);
        setupSearch();
        
        // Load users and chats
        loadUsers();
        loadChats();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUserOnlineStatus(true);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        updateUserOnlineStatus(false);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove event listeners
        if (chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
        if (usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }
    
    private void updateUserOnlineStatus(boolean online) {
        if (currentUser != null) {
            usersRef.child(currentUser.getUid()).child("online").setValue(online);
            if (!online) {
                usersRef.child(currentUser.getUid()).child("lastSeen").setValue(System.currentTimeMillis());
            }
        }
    }
    
    private void setupFilterChips() {
        Chip allChip = findViewById(R.id.chip_all);
        Chip pinnedChip = findViewById(R.id.chip_pinned);
        Chip archivedChip = findViewById(R.id.chip_archived);
        
        allChip.setOnClickListener(v -> {
            currentFilter = "all";
            filterChats();
        });
        
        pinnedChip.setOnClickListener(v -> {
            currentFilter = "pinned";
            filterChats();
        });
        
        archivedChip.setOnClickListener(v -> {
            currentFilter = "archived";
            filterChats();
        });
    }
    
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                filterChats(newText);
                return true;
            }
        });
    }
    
    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, 
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                  @NonNull RecyclerView.ViewHolder viewHolder, 
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Chat chat = filteredChatList.get(position);
                
                if (direction == ItemTouchHelper.RIGHT) {
                    // Archive/Unarchive
                    boolean isArchived = chat.isArchivedBy(currentUser.getUid());
                    chat.setArchivedBy(currentUser.getUid(), !isArchived);
                    chatsRef.child(chat.getChatId()).child("archivedBy").child(currentUser.getUid())
                            .setValue(!isArchived);
                    
                    String message = isArchived ? "Чат разархивирован" : "Чат архивирован";
                    Snackbar.make(chatListRecyclerView, message, Snackbar.LENGTH_LONG)
                            .setAction("Отменить", v -> {
                                chat.setArchivedBy(currentUser.getUid(), isArchived);
                                chatsRef.child(chat.getChatId()).child("archivedBy")
                                        .child(currentUser.getUid()).setValue(isArchived);
                            }).show();
                } else {
                    // Pin/Unpin
                    boolean isPinned = chat.isPinnedBy(currentUser.getUid());
                    chat.setPinnedBy(currentUser.getUid(), !isPinned);
                    chatsRef.child(chat.getChatId()).child("pinnedBy").child(currentUser.getUid())
                            .setValue(!isPinned);
                    
                    String message = isPinned ? "Чат откреплен" : "Чат закреплен";
                    Snackbar.make(chatListRecyclerView, message, Snackbar.LENGTH_LONG)
                            .setAction("Отменить", v -> {
                                chat.setPinnedBy(currentUser.getUid(), isPinned);
                                chatsRef.child(chat.getChatId()).child("pinnedBy")
                                        .child(currentUser.getUid()).setValue(isPinned);
                            }).show();
                }
                
                filterChats();
            }
        };
        
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(chatListRecyclerView);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadUsers() {
        usersListener = usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userMap.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        userMap.put(user.getUid(), user);
                    }
                }
                chatAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load users: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadChats() {
        chatsListener = chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat != null && chat.getParticipants().containsKey(currentUser.getUid())) {
                        chatList.add(chat);
                    }
                }
                
                // Sort chats by timestamp (newest first)
                Collections.sort(chatList, (c1, c2) -> 
                        Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
                
                filterChats();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load chats: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void filterChats() {
        filterChats(searchView.getQuery().toString());
    }
    
    private void filterChats(String query) {
        filteredChatList.clear();
        
        String searchText = query != null ? query.toLowerCase().trim() : "";
        
        for (Chat chat : chatList) {
            // Apply category filter
            boolean isArchived = chat.isArchivedBy(currentUser.getUid());
            boolean isPinned = chat.isPinnedBy(currentUser.getUid());
            
            if (currentFilter.equals("pinned") && !isPinned) {
                continue;
            } else if (currentFilter.equals("archived") && !isArchived) {
                continue;
            } else if (currentFilter.equals("all") && isArchived) {
                continue;
            }
            
            // Apply search filter
            if (!searchText.isEmpty()) {
                boolean matchFound = false;
                
                // Check if chat name contains query
                if (chat.getChatName() != null && chat.getChatName().toLowerCase().contains(searchText)) {
                    matchFound = true;
                }
                
                // Check if any participant's name contains query
                if (!matchFound) {
                    for (String userId : chat.getParticipants().keySet()) {
                        if (userId.equals(currentUser.getUid())) {
                            continue;
                        }
                        
                        User user = userMap.get(userId);
                        if (user != null && (
                                user.getDisplayName().toLowerCase().contains(searchText) ||
                                user.getEmail().toLowerCase().contains(searchText))) {
                            matchFound = true;
                            break;
                        }
                    }
                }
                
                // Check if last message contains query
                if (!matchFound && chat.getLastMessageText() != null && 
                        chat.getLastMessageText().toLowerCase().contains(searchText)) {
                    matchFound = true;
                }
                
                if (!matchFound) {
                    continue;
                }
            }
            
            filteredChatList.add(chat);
        }
        
        // Sort filtered chats: pinned first, then by timestamp
        Collections.sort(filteredChatList, (c1, c2) -> {
            boolean p1 = c1.isPinnedBy(currentUser.getUid());
            boolean p2 = c2.isPinnedBy(currentUser.getUid());
            
            if (p1 && !p2) return -1;
            if (!p1 && p2) return 1;
            
            return Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp());
        });
        
        // Show empty view if no chats match filters
        TextView emptyView = findViewById(R.id.empty_view);
        if (filteredChatList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            if (currentFilter.equals("all")) {
                emptyView.setText("Нет чатов\nНачните новый чат, нажав на + внизу");
            } else if (currentFilter.equals("pinned")) {
                emptyView.setText("Нет закрепленных чатов");
            } else {
                emptyView.setText("Нет архивированных чатов");
            }
        } else {
            emptyView.setVisibility(View.GONE);
        }
        
        chatAdapter.notifyDataSetChanged();
    }
    
    private void showNewChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Новый чат");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_chat, null);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        
        builder.setView(dialogView);
        builder.setPositiveButton("Создать", (dialog, which) -> {
            String email = etEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                findUserByEmail(email, (AlertDialog) dialog);
            }
        });
        
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        
        builder.show();
    }
    
    private void findUserByEmail(String email, AlertDialog dialog) {
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User recipient = snapshot.getValue(User.class);
                        if (recipient != null) {
                            // Don't create chat with yourself
                            if (recipient.getUid().equals(currentUser.getUid())) {
                                Toast.makeText(MainActivity.this, "Вы не можете создать чат с самим собой",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Create new chat
                            createChat(recipient);
                            dialog.dismiss();
                            return;
                        }
                    }
                }
                Toast.makeText(MainActivity.this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Ошибка: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void createChat(User recipient) {
        // Check if chat already exists with this user
        for (Chat chat : chatList) {
            if (chat.getParticipants().size() == 2 && 
                    chat.getParticipants().containsKey(recipient.getUid())) {
                // Chat already exists, open it
                openChat(chat.getChatId(), recipient.getUid());
                return;
            }
        }
        
        // Create a new chat with both users as participants
        DatabaseReference newChatRef = chatsRef.push();
        String chatId = newChatRef.getKey();
        
        Map<String, Boolean> participants = new HashMap<>();
        participants.put(currentUser.getUid(), true);
        participants.put(recipient.getUid(), true);
        
        Chat newChat = new Chat(chatId, participants);
        
        newChatRef.setValue(newChat)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Чат создан", Toast.LENGTH_SHORT).show();
                    
                    // Open the chat
                    openChat(chatId, recipient.getUid());
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Не удалось создать чат: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }
    
    private void openChat(String chatId, String recipientId) {
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("recipientId", recipientId);
        startActivity(intent);
    }
    
    private void logoutUser() {
        // Update online status
        updateUserOnlineStatus(false);
        
        // Unsubscribe from FCM topic
        FirebaseMessaging.getInstance().unsubscribeFromTopic(currentUser.getUid());
        
        // Sign out
        auth.signOut();
        
        // Go to login screen
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
    
    private void registerFcmToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();

                // Save the token to the user's database entry
                if (currentUser != null) {
                    usersRef.child(currentUser.getUid()).child("fcmToken").setValue(token)
                        .addOnSuccessListener(aVoid -> 
                            Log.d("MainActivity", "FCM Token successfully updated"))
                        .addOnFailureListener(e -> 
                            Log.e("MainActivity", "Failed to update FCM token", e));
                }
                
                // Subscribe to user-specific topic for targeted notifications
                FirebaseMessaging.getInstance().subscribeToTopic(currentUser.getUid())
                    .addOnCompleteListener(subscribeTask -> {
                        if (subscribeTask.isSuccessful()) {
                            Log.d("MainActivity", "Subscribed to topic: " + currentUser.getUid());
                        } else {
                            Log.e("MainActivity", "Failed to subscribe to topic", subscribeTask.getException());
                        }
                    });
            });
    }
    
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        
        private List<Chat> chatList;
        private Map<String, User> userMap;
        private String currentUserId;
        
        public ChatAdapter(List<Chat> chatList, Map<String, User> userMap, String currentUserId) {
            this.chatList = chatList;
            this.userMap = userMap;
            this.currentUserId = currentUserId;
        }
        
        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            Chat chat = chatList.get(position);
            String otherUserId = chat.getOtherUserId(currentUserId);
            User otherUser = userMap.get(otherUserId);
            
            if (otherUser != null) {
                // Set chat name
                holder.tvChatName.setText(otherUser.getDisplayName());
                
                // Set last message
                String lastMessageText = chat.getLastMessageText();
                if (lastMessageText == null || lastMessageText.isEmpty()) {
                    holder.tvLastMessage.setText("Начните общение");
                } else {
                    // Если последнее сообщение от текущего пользователя, добавляем "Вы: " перед текстом
                    if (chat.getLastMessageSenderId() != null && 
                            chat.getLastMessageSenderId().equals(currentUserId)) {
                        holder.tvLastMessage.setText("Вы: " + lastMessageText);
                    } else {
                        holder.tvLastMessage.setText(lastMessageText);
                    }
                }
                
                // Check unread count
                long unreadCount = chat.getUnreadCountForUser(currentUserId);
                
                // Always show time
                holder.tvTimeOrUnread.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_secondary));
                
                if (chat.getLastMessageTimestamp() > 0) {
                    SimpleDateFormat dateFormat;
                    
                    // Разные форматы в зависимости от даты сообщения
                    if (android.text.format.DateUtils.isToday(chat.getLastMessageTimestamp())) {
                        // Сегодня - показываем только время (HH:mm)
                        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    } else {
                        // Не сегодня - показываем дату (dd.MM)
                        dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
                    }
                    holder.tvTimeOrUnread.setText(dateFormat.format(new Date(chat.getLastMessageTimestamp())));
                } else {
                    holder.tvTimeOrUnread.setText("");
                }
                
                // Show unread count separately
                if (unreadCount > 0) {
                    holder.tvUnreadCount.setVisibility(View.VISIBLE);
                    holder.tvUnreadCount.setText(String.valueOf(unreadCount));
                } else {
                    holder.tvUnreadCount.setVisibility(View.GONE);
                }
                
                // Set online status indicator - color only
                if (otherUser.isOnline()) {
                    holder.ivStatus.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.online_green));
                    
                    // Check if user is typing or choosing photo
                    String activityStatus = otherUser.getStatusForChatList();
                    if (!activityStatus.isEmpty()) {
                        // Show activity status in place of the last message
                        holder.tvLastMessage.setText(activityStatus);
                        holder.tvLastMessage.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.accent));
                        holder.tvLastMessage.setTypeface(null, Typeface.ITALIC);
                        
                        // Hide the user activity text view as we're showing the status in the message area
                        holder.tvUserActivity.setVisibility(View.GONE);
                    } else {
                        // Reset message text appearance if not typing
                        holder.tvLastMessage.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_secondary));
                        holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
                        holder.tvUserActivity.setVisibility(View.GONE);
                    }
                } else {
                    // Gray indicator for offline users
                    holder.ivStatus.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.gray));
                    holder.tvUserActivity.setVisibility(View.GONE);
                    
                    // Reset message text appearance
                    holder.tvLastMessage.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_secondary));
                    holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
                }
                
                // Always show the indicator now
                holder.ivStatus.setVisibility(View.VISIBLE);
                
                // Скрываем неиспользуемые элементы
                holder.tvLastSeen.setVisibility(View.GONE);
                
                // Load user avatar if available
                if (otherUser.getPhotoUrl() != null && !otherUser.getPhotoUrl().isEmpty()) {
                    if (otherUser.getPhotoUrl().startsWith("data:image")) {
                        try {
                            String base64Image = otherUser.getPhotoUrl().split(",")[1];
                            byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            holder.ivAvatar.setImageBitmap(decodedBitmap);
                        } catch (Exception e) {
                            holder.ivAvatar.setImageResource(R.drawable.default_profile);
                        }
                    } else {
                        Glide.with(MainActivity.this)
                            .load(otherUser.getPhotoUrl())
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .into(holder.ivAvatar);
                    }
                } else {
                    holder.ivAvatar.setImageResource(R.drawable.default_profile);
                }
                
                // Set pin status
                holder.ivPin.setVisibility(chat.isPinnedBy(currentUserId) ? View.VISIBLE : View.GONE);
            }
            
            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                String finalOtherUserId = otherUserId;
                openChat(chat.getChatId(), finalOtherUserId);
            });
        }
        
        @Override
        public int getItemCount() {
            return chatList.size();
        }
        
        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvChatName, tvLastMessage, tvTimeOrUnread, tvLastSeen, tvUserActivity, tvUnreadCount;
            ImageView ivStatus, ivPin, ivAvatar;
            
            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvChatName = itemView.findViewById(R.id.tvChatName);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvTimeOrUnread = itemView.findViewById(R.id.tvTimeOrUnread);
                tvLastSeen = itemView.findViewById(R.id.tvLastSeen);
                ivStatus = itemView.findViewById(R.id.ivStatus);
                ivPin = itemView.findViewById(R.id.ivPin);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvUserActivity = itemView.findViewById(R.id.tvUserActivity);
                tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            }
        }
    }
} 