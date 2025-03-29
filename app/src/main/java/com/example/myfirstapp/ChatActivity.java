package com.example.myfirstapp;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myfirstapp.model.Chat;
import com.example.myfirstapp.model.Message;
import com.example.myfirstapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ServerValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnAttach;
    private CardView replyPreviewCard;
    private TextView tvReplyAuthor, tvReplyText;
    private LinearLayout chatInfo;
    private TextView tvChatTitle, tvOnlineStatus;
    
    private MessageAdapter adapter;
    private List<Message> messageList;

    private FirebaseUser currentUser;
    private String chatId;
    private String recipientId;
    private String recipientName;
    private DatabaseReference chatRef, messagesRef, usersRef;
    private User recipientUser;
    
    private Message replyToMessage = null;
    private Uri selectedImageUri = null;
    private boolean isRecipientOnline = false;
    private ValueEventListener onlineStatusListener;
    
    private static final long TYPING_TIMEOUT = 3000; // 3 seconds
    private static final long ONLINE_HEARTBEAT_INTERVAL = 12000; // 12 seconds for online status to persist
    private Timer onlineHeartbeatTimer;
    private Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingRunnable;
    
    // Activity Result Launcher для выбора изображения
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    
                    // Показываем диалог подтверждения с превью
                    showImagePreviewDialog(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get chat ID and recipient ID from intent
        chatId = getIntent().getStringExtra("chatId");
        recipientId = getIntent().getStringExtra("recipientId");

        if (chatId == null || recipientId == null) {
            Toast.makeText(this, "Error: missing chat information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://chat-app-a8ac0-default-rtdb.europe-west1.firebasedatabase.app");
        chatRef = database.getReference("chats").child(chatId);
        messagesRef = database.getReference("messages").child(chatId);
        usersRef = database.getReference("users");

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);
        replyPreviewCard = findViewById(R.id.replyPreviewCard);
        tvReplyAuthor = findViewById(R.id.tvReplyAuthor);
        tvReplyText = findViewById(R.id.tvReplyText);
        chatInfo = findViewById(R.id.chatInfo);
        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        
        // Обработчик закрытия превью ответа
        findViewById(R.id.btnCloseReply).setOnClickListener(v -> cancelReply());

        // Set up RecyclerView
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
        
        // Настраиваем свайп для ответа на сообщение
        setupMessageSwipeActions();

        // Load recipient user info
        loadRecipientInfo();

        // Load messages
        loadMessages();

        // Настройка клика на шапку чата для просмотра профиля
        chatInfo.setOnClickListener(v -> showUserProfile());
        
        // Send message
        btnSend.setOnClickListener(v -> sendMessage());
        
        // Кнопка прикрепления изображения
        btnAttach.setOnClickListener(v -> openImagePicker());

        // Update the typing indicator implementation to reduce lag
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (typingRunnable != null) {
                    typingHandler.removeCallbacks(typingRunnable);
                }
                
                // Set typing status only when text changes
                if (s.length() > 0) {
                    updateTypingStatus(true);
                }
                
                // Schedule to reset typing status after delay
                typingRunnable = () -> updateTypingStatus(false);
                typingHandler.postDelayed(typingRunnable, TYPING_TIMEOUT);
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // We're handling this in onTextChanged instead
            }
        });

        // Make reply preview clickable
        replyPreviewCard.setOnClickListener(v -> {
            if (replyToMessage != null) {
                scrollToMessage(replyToMessage.getMessageId());
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        startListeningForOnlineStatus();
        startOnlineHeartbeat();
        
        // Mark messages as read immediately when returning to the chat
        if (!messageList.isEmpty()) {
            markVisibleMessagesAsRead();
            
            // Also set up a timer to periodically mark messages as read while in the chat
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing() && !isDestroyed()) {
                        markVisibleMessagesAsRead();
                        new Handler().postDelayed(this, 2000); // Check every 2 seconds
                    }
                }
            }, 2000);
        }
    }
    
    @Override
    protected void onPause() {
        stopListeningForOnlineStatus();
        stopOnlineHeartbeat();
        super.onPause();
    }
    
    private void startListeningForOnlineStatus() {
        if (recipientId != null) {
            onlineStatusListener = usersRef.child(recipientId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        isRecipientOnline = user.isOnline();
                        updateOnlineStatus(user);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Игнорируем ошибки
                }
            });
        }
    }
    
    private void stopListeningForOnlineStatus() {
        if (onlineStatusListener != null && recipientId != null) {
            usersRef.child(recipientId).removeEventListener(onlineStatusListener);
        }
    }
    
    private void updateOnlineStatus(User user) {
        if (user.isOnline()) {
            tvOnlineStatus.setText("В сети");
            tvOnlineStatus.setTextColor(getResources().getColor(R.color.online_green, null));
        } else {
            tvOnlineStatus.setText(user.getFormattedLastSeen());
            tvOnlineStatus.setTextColor(getResources().getColor(R.color.white, null));
        }
        
        // Обновляем текст статуса в зависимости от активности пользователя
        if (user.isTyping()) {
            tvOnlineStatus.setText("печатает...");
            tvOnlineStatus.setTextColor(getResources().getColor(R.color.accent, null));
        } else if (user.isChoosingPhoto()) {
            tvOnlineStatus.setText("выбирает фото...");
            tvOnlineStatus.setTextColor(getResources().getColor(R.color.accent, null));
        }
        
        // Загружаем аватар пользователя
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            ImageView recipientAvatar = findViewById(R.id.ivRecipientAvatar);
            
            if (user.getPhotoUrl().startsWith("data:image")) {
                try {
                    String base64Image = user.getPhotoUrl().split(",")[1];
                    byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    recipientAvatar.setImageBitmap(decodedBitmap);
                } catch (Exception e) {
                    recipientAvatar.setImageResource(R.drawable.default_profile);
                }
            } else {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(recipientAvatar);
            }
        }
    }
    
    private void openImagePicker() {
        // Обновляем статус при открытии выбора фото
        updateChoosingPhotoStatus(true);
        
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
    
    private void showImagePreviewDialog(Uri imageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Отправить изображение");
        
        View view = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageView imagePreview = view.findViewById(R.id.image_preview);
        EditText captionEditText = view.findViewById(R.id.caption_edit_text);
        
        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(imagePreview);
        
        builder.setView(view);
        
        builder.setPositiveButton("Отправить", (dialog, which) -> {
            String caption = captionEditText.getText().toString().trim();
            sendImageMessage(imageUri, caption);
        });
        
        builder.setNegativeButton("Отмена", (dialog, which) -> 
                selectedImageUri = null);
        
        builder.show();
    }
    
    private void sendImageMessage(Uri imageUri, String caption) {
        if (imageUri == null) return;
        
        // Показываем индикатор загрузки
        Toast.makeText(this, "Подготовка изображения...", Toast.LENGTH_SHORT).show();
        
        try {
            // Получаем и сжимаем изображение
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            bitmap = getResizedBitmap(bitmap, 800); // Ограничиваем размер
            
            // Конвертируем изображение в Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            // Создаем новое сообщение с изображением
            String messageId = messagesRef.push().getKey();
            if (messageId == null) return;
            
            Message message = new Message(messageId, chatId, currentUser.getUid(), caption);
            message.setImageUrl("data:image/jpeg;base64," + base64Image);
            
            // Устанавливаем ID сообщения-ответа, если есть
            if (replyToMessage != null) {
                message.setReplyToMessageId(replyToMessage.getMessageId());
                replyToMessage = null;
                replyPreviewCard.setVisibility(View.GONE);
            }
            
            // Сохраняем сообщение
            messagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        // Обновляем информацию о последнем сообщении в чате
                        String displayText = caption.isEmpty() ? "Изображение" : caption;
                        Map<String, Object> lastMessageInfo = new HashMap<>();
                        lastMessageInfo.put("lastMessageText", displayText);
                        lastMessageInfo.put("lastMessageTimestamp", message.getTimestamp());
                        lastMessageInfo.put("lastMessageSenderId", currentUser.getUid());
                        chatRef.updateChildren(lastMessageInfo);
                        
                        // Увеличиваем счетчик непрочитанных сообщений для получателя
                        chatRef.child("unreadCount").child(recipientId).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        Long count = dataSnapshot.getValue(Long.class);
                                        if (count == null) count = 0L;
                                        chatRef.child("unreadCount").child(recipientId).setValue(count + 1);
                                    }
                                    
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                                });
                        
                        // Прокручиваем список к последнему сообщению
                        if (!messageList.isEmpty()) {
                            rvMessages.smoothScrollToPosition(messageList.size() - 1);
                        }
                        
                        // Очищаем выбранное изображение
                        selectedImageUri = null;
                    })
                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this,
                            "Ошибка при отправке сообщения: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка при обработке изображения", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    // Уменьшаем размер изображения для экономии места
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
    
    private void showUserProfile() {
        if (recipientUser == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View profileView = getLayoutInflater().inflate(R.layout.dialog_user_profile, null);
        
        TextView tvProfileName = profileView.findViewById(R.id.profile_name);
        TextView tvProfileEmail = profileView.findViewById(R.id.profile_email);
        TextView tvProfileStatus = profileView.findViewById(R.id.profile_status);
        TextView tvProfileOnlineStatus = profileView.findViewById(R.id.profile_online_status);
        ImageView ivProfilePicture = profileView.findViewById(R.id.profile_picture);
        
        tvProfileName.setText(recipientUser.getDisplayName());
        tvProfileEmail.setText(recipientUser.getEmail());
        
        String status = recipientUser.getStatus();
        if (status != null && !status.isEmpty()) {
            tvProfileStatus.setText(status);
        } else {
            tvProfileStatus.setText("Статус не указан");
        }
        
        if (recipientUser.isOnline()) {
            tvProfileOnlineStatus.setText("В сети");
            tvProfileOnlineStatus.setTextColor(getResources().getColor(R.color.online_green, null));
        } else {
            tvProfileOnlineStatus.setText(recipientUser.getFormattedLastSeen());
        }
        
        String photoUrl = recipientUser.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .circleCrop()
                    .into(ivProfilePicture);
        } else {
            ivProfilePicture.setImageResource(R.drawable.default_profile);
        }
        
        builder.setView(profileView);
        builder.setPositiveButton("Закрыть", null);
        builder.show();
    }

    private void loadRecipientInfo() {
        usersRef.child(recipientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recipientUser = dataSnapshot.getValue(User.class);
                if (recipientUser != null) {
                    recipientName = recipientUser.getDisplayName();
                    tvChatTitle.setText(recipientName);
                    updateOnlineStatus(recipientUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Failed to load user info: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Store current scroll position to restore it later if needed
                LinearLayoutManager layoutManager = (LinearLayoutManager) rvMessages.getLayoutManager();
                int lastVisiblePosition = -1;
                if (layoutManager != null) {
                    lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                }
                
                int previousMessageCount = messageList.size();
                messageList.clear();
                Map<String, Message> tempMessagesMap = new HashMap<>();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        messageList.add(message);
                        tempMessagesMap.put(message.getMessageId(), message);
                    }
                }
                
                // Сортируем сообщения по времени
                Collections.sort(messageList, (m1, m2) -> 
                        Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                
                adapter.setMessagesMap(tempMessagesMap);
                adapter.notifyDataSetChanged();
                
                // If this is the first load, immediately mark all messages as read
                if (previousMessageCount == 0 && !messageList.isEmpty()) {
                    // Find the most recent message from other user
                    String lastMessageId = findLastMessageFromSender(recipientId);
                    if (lastMessageId != null) {
                        // Immediately mark this as the last read message
                        chatRef.child("lastReadMessageId").child(currentUser.getUid()).setValue(lastMessageId);
                        
                        // Also reset unread count
                        resetUnreadCount();
                    }
                }
                
                // Determine scroll position based on conditions
                if (lastVisiblePosition >= 0 && previousMessageCount > 0) {
                    // If we were already viewing messages, maintain position
                    if (lastVisiblePosition == previousMessageCount - 1) {
                        // Was at the bottom, so scroll to the new bottom
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    } else {
                        // Maintain the same relative position
                        int targetPosition = Math.min(lastVisiblePosition, messageList.size() - 1);
                        rvMessages.scrollToPosition(targetPosition);
                    }
                } else if (messageList.size() > 0) {
                    // First load - determine center position based on last read and last own message
                    chatRef.child("lastReadMessageId").child(currentUser.getUid()).addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String lastReadId = dataSnapshot.getValue(String.class);
                                
                                // Find position of last read message
                                int lastReadPosition = -1;
                                if (lastReadId != null) {
                                    lastReadPosition = findPositionByMessageId(lastReadId);
                                }
                                
                                // Find position of user's last message
                                String lastOwnMessageId = findLastMessageFromSender(currentUser.getUid());
                                int lastOwnPosition = -1;
                                if (lastOwnMessageId != null) {
                                    lastOwnPosition = findPositionByMessageId(lastOwnMessageId);
                                }
                                
                                // Determine which position to center on (max of lastRead and lastOwn)
                                int centerPosition = -1;
                                if (lastReadPosition >= 0 && lastOwnPosition >= 0) {
                                    // Both exist, take the max
                                    centerPosition = Math.max(lastReadPosition, lastOwnPosition);
                                } else if (lastReadPosition >= 0) {
                                    // Only last read exists
                                    centerPosition = lastReadPosition;
                                } else if (lastOwnPosition >= 0) {
                                    // Only last own exists
                                    centerPosition = lastOwnPosition;
                                }
                                
                                if (centerPosition != -1) {
                                    // Center this message on screen
                                    final int finalCenterPosition = centerPosition;
                                    rvMessages.post(() -> centerMessageOnScreen(finalCenterPosition));
                                } else {
                                    // No suitable message found, fall back to default behavior
                                    handleNoLastReadMessage();
                                }
                                
                                // Always mark visible messages as read after initial positioning
                                rvMessages.post(() -> markVisibleMessagesAsRead());
                            }
                            
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Fallback to default position
                                handleNoLastReadMessage();
                                
                                // Still mark messages as read
                                rvMessages.post(() -> markVisibleMessagesAsRead());
                            }
                        });
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Ошибка загрузки сообщений: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        // Add scroll listener for updating read status when scrolling
        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            // Variable to track if we're currently scrolling
            private boolean isScrolling = false;
            
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                // When starting to scroll, track that we're scrolling
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isScrolling = true;
                }
                
                // When scrolling stops, mark messages as read and reset flag
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isScrolling = false;
                    markVisibleMessagesAsRead();
                }
            }
            
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // During fling scrolling, periodically check and mark messages
                // This helps with long chat histories
                if (isScrolling && Math.abs(dy) > 50) {
                    markVisibleMessagesAsRead();
                }
            }
        });
    }
    
    // Find the position of a message by its ID
    private int findPositionByMessageId(String messageId) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getMessageId().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }
    
    // Find the ID of the last message from a specific sender
    private String findLastMessageFromSender(String senderId) {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message message = messageList.get(i);
            if (message.getSenderId().equals(senderId)) {
                return message.getMessageId();
            }
        }
        return null;
    }
    
    // Handle the case when no last read message is found
    private void handleNoLastReadMessage() {
        int firstUnreadPosition = findFirstUnreadMessagePosition();
                    
        if (firstUnreadPosition != -1) {
            // Position a few messages above first unread
            int targetPosition = Math.max(0, firstUnreadPosition - 3);
            rvMessages.scrollToPosition(targetPosition);
        } else {
            // No unread messages, scroll to the bottom
            rvMessages.scrollToPosition(messageList.size() - 1);
        }
    }
    
    // Center a message on screen
    private void centerMessageOnScreen(int position) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvMessages.getLayoutManager();
        if (layoutManager != null) {
            // First scroll to position to ensure the view is created
            layoutManager.scrollToPositionWithOffset(position, 0);
            
            // Then post a delayed action to center it after layout is complete
            rvMessages.post(() -> {
                View targetView = layoutManager.findViewByPosition(position);
                if (targetView != null) {
                    int screenCenter = rvMessages.getHeight() / 2;
                    int targetCenter = targetView.getHeight() / 2;
                    int offset = screenCenter - targetCenter;
                    
                    // Apply the centering offset
                    layoutManager.scrollToPositionWithOffset(position, offset);
                }
            });
        }
    }
    
    // Find position of first unread message
    private int findFirstUnreadMessagePosition() {
        for (int i = 0; i < messageList.size(); i++) {
            Message message = messageList.get(i);
            if (!message.getSenderId().equals(currentUser.getUid()) && !message.isReadBy(currentUser.getUid())) {
                return i;
            }
        }
        return -1; // No unread messages
    }
    
    // Метод для отметки видимых на экране сообщений как прочитанных
    private void markVisibleMessagesAsRead() {
        if (messageList.isEmpty()) return;
        
        // Ищем сообщения от собеседника, которые видны на экране
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvMessages.getLayoutManager();
        if (layoutManager == null) return;
        
        int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
        
        if (firstVisiblePosition == -1 || lastVisiblePosition == -1) return;
        
        // Track the last message from recipient we find
        String lastVisibleMessageId = null;
        List<String> messagesToMarkAsRead = new ArrayList<>();
        
        // Go through all completely visible messages from recipient
        for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
            if (i >= messageList.size()) continue;
            
            Message message = messageList.get(i);
            // Если сообщение от собеседника и не прочитано текущим пользователем
            if (!message.getSenderId().equals(currentUser.getUid()) && !message.isReadBy(currentUser.getUid())) {
                messagesToMarkAsRead.add(message.getMessageId());
                lastVisibleMessageId = message.getMessageId();
            }
        }
        
        // If we don't have any completely visible messages, try partially visible ones
        if (messagesToMarkAsRead.isEmpty()) {
            int firstPartialVisible = layoutManager.findFirstVisibleItemPosition();
            int lastPartialVisible = layoutManager.findLastVisibleItemPosition();
            
            for (int i = firstPartialVisible; i <= lastPartialVisible; i++) {
                if (i >= messageList.size()) continue;
                
                Message message = messageList.get(i);
                if (!message.getSenderId().equals(currentUser.getUid()) && !message.isReadBy(currentUser.getUid())) {
                    messagesToMarkAsRead.add(message.getMessageId());
                    lastVisibleMessageId = message.getMessageId();
                }
            }
        }
        
        // If we found messages to mark as read, update them
        if (!messagesToMarkAsRead.isEmpty()) {
            // Mark each message as read with individual updates for reliability
            for (String messageId : messagesToMarkAsRead) {
                messagesRef.child(messageId).child("readBy").child(currentUser.getUid()).setValue(true);
            }
            
            // Update lastReadMessageId for the chat
            if (lastVisibleMessageId != null) {
                chatRef.child("lastReadMessageId").child(currentUser.getUid()).setValue(lastVisibleMessageId);
            }
            
            // Reset unread count
            resetUnreadCount();
            
            // Force adapter to refresh to show updated read status immediately
            adapter.notifyDataSetChanged();
        }
    }
    
    private void resetUnreadCount() {
        // Сбрасываем счетчик непрочитанных сообщений в чате для текущего пользователя
        chatRef.child("unreadCount").child(currentUser.getUid()).setValue(0L);
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        if (TextUtils.isEmpty(messageText) && selectedImageUri == null) {
            return;  // Не отправляем пустые сообщения без изображений
        }
        
        // Отключаем кнопку отправки, чтобы избежать дублирования
        btnSend.setEnabled(false);
        
        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;
        
        Message message = new Message(messageId, chatId, currentUser.getUid(), messageText);
        
        // Если отвечаем на сообщение
        if (replyToMessage != null) {
            message.setReplyToMessageId(replyToMessage.getMessageId());
        }
        
        // Если отправляем изображение
        if (selectedImageUri != null) {
            try {
                // Получаем и сжимаем изображение
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                bitmap = getResizedBitmap(bitmap, 800); // Ограничиваем размер
                
                // Конвертируем изображение в Base64
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                
                // Добавляем изображение в сообщение
                message.setImageUrl("data:image/jpeg;base64," + base64Image);
                
                // Сбрасываем URI изображения
                selectedImageUri = null;
            } catch (IOException e) {
                Toast.makeText(this, "Ошибка при обработке изображения", Toast.LENGTH_SHORT).show();
                btnSend.setEnabled(true);
                return;
            }
        }
        
        // Сохраняем сообщение в базу данных
        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    // Очищаем поле ввода
                    etMessage.setText("");
                    btnSend.setEnabled(true);
                    cancelReply();
                    
                    // Увеличиваем счетчик непрочитанных сообщений для получателя
                    chatRef.child("unreadCount").child(recipientId).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Long count = dataSnapshot.getValue(Long.class);
                                    if (count == null) count = 0L;
                                    chatRef.child("unreadCount").child(recipientId).setValue(count + 1);
                                }
                                
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {}
                            });
                    
                    // Обновляем информацию о последнем сообщении в чате
                    Map<String, Object> lastMessageInfo = new HashMap<>();
                    lastMessageInfo.put("lastMessageText", messageText.isEmpty() && message.hasImage() ? 
                            "[Изображение]" : messageText);
                    lastMessageInfo.put("lastMessageTimestamp", message.getTimestamp());
                    lastMessageInfo.put("lastMessageSenderId", currentUser.getUid());
                    
                    chatRef.updateChildren(lastMessageInfo);
                    
                    // Прокручиваем чат до последнего сообщения
                    rvMessages.post(() -> rvMessages.smoothScrollToPosition(messageList.size() - 1));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Ошибка отправки: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                });
    }
    
    private void replyToMessage(Message message) {
        replyToMessage = message;
        replyPreviewCard.setVisibility(View.VISIBLE);
        
        String senderName = message.getSenderId().equals(currentUser.getUid()) ? "Вы" : recipientName;
        tvReplyAuthor.setText(senderName);
        
        String previewText = message.getText();
        if (TextUtils.isEmpty(previewText) && message.hasImage()) {
            previewText = "[Изображение]";
        }
        tvReplyText.setText(previewText);
    }
    
    private void cancelReply() {
        replyToMessage = null;
        replyPreviewCard.setVisibility(View.GONE);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_view_profile) {
            showUserProfile();
            return true;
        } else if (id == R.id.action_clear_history) {
            showClearHistoryConfirmationDialog();
            return true;
        } else if (id == R.id.action_block_user) {
            showBlockUserConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showClearHistoryConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Очистить историю")
                .setMessage("Вы уверены, что хотите удалить всю историю переписки?")
                .setPositiveButton("Да", (dialog, which) -> clearChatHistory())
                .setNegativeButton("Нет", null)
                .show();
    }
    
    private void clearChatHistory() {
        messagesRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ChatActivity.this, "История чата очищена", Toast.LENGTH_SHORT).show();
                    
                    // Обновляем информацию о последнем сообщении
                    chatRef.child("lastMessageText").setValue(null);
                    chatRef.child("lastMessageTimestamp").setValue(System.currentTimeMillis());
                    chatRef.child("lastMessageSenderId").setValue(null);
                })
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this,
                        "Ошибка при очистке истории: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    private void showBlockUserConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Заблокировать пользователя")
                .setMessage("Вы уверены, что хотите заблокировать этого пользователя? Вы больше не сможете получать сообщения от него.")
                .setPositiveButton("Да", (dialog, which) -> blockUser())
                .setNegativeButton("Нет", null)
                .show();
    }
    
    private void blockUser() {
        // TODO: Реализация блокировки пользователя
        Toast.makeText(this, "Функция блокировки пока не реализована", Toast.LENGTH_SHORT).show();
    }
    
    private void showFullScreenImage(String imageUrl) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
            View view = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);
            com.github.chrisbanes.photoview.PhotoView fullscreenImageView = view.findViewById(R.id.fullscreen_image);
            
            // Ensure we have base64 data
            if (imageUrl.startsWith("data:image")) {
                // Extract the base64 part
                String base64Data = imageUrl.split(",")[1];
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                
                // Set the bitmap to the PhotoView
                fullscreenImageView.setImageBitmap(decodedBitmap);
            } else {
                // Direct URL
                Glide.with(this)
                    .load(imageUrl)
                    .into(fullscreenImageView);
            }
            
            // Configure PhotoView for proper scaling
            fullscreenImageView.setMaximumScale(5.0f);
            fullscreenImageView.setMediumScale(2.5f);
            fullscreenImageView.setMinimumScale(1.0f);
            
            builder.setView(view);
            
            AlertDialog dialog = builder.create();
            // Set completely black background
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK));
            
            // Setup close button
            ImageButton closeButton = view.findViewById(R.id.close_button);
            closeButton.setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при открытии изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ChatActivity", "Error displaying image", e);
        }
    }
    
    private void setupMessageSwipeActions() {
        // Настраиваем свайп для ответа на сообщение
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Не поддерживаем перемещение
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Message swipedMessage = messageList.get(position);
                
                // Отвечаем на сообщение
                replyToMessage(swipedMessage);
                
                // Сбрасываем свайп (возвращаем элемент в исходное положение)
                adapter.notifyItemChanged(position);
            }
            
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Ограничиваем свайп 1/3 ширины экрана
                float maxSwipe = recyclerView.getWidth() / 3f;
                float actualSwipe = Math.min(dX, maxSwipe);
                
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // Рисуем индикатор свайпа
                    View itemView = viewHolder.itemView;
                    
                    Paint paint = new Paint();
                    paint.setColor(getResources().getColor(R.color.primary_light, null));
                    
                    // Рисуем задний фон как индикатор свайпа
                    RectF background = new RectF(itemView.getLeft(), itemView.getTop(), 
                                                itemView.getLeft() + actualSwipe, itemView.getBottom());
                    c.drawRect(background, paint);
                    
                    // Рисуем иконку ответа
                    Drawable icon = getResources().getDrawable(android.R.drawable.ic_menu_revert, null);
                    int iconMargin = 16;
                    int iconSize = 48;
                    
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;
                    int iconRight = iconLeft + iconSize;
                    int iconBottom = iconTop + iconSize;
                    
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    icon.setTint(getResources().getColor(R.color.primary, null));
                    icon.draw(c);
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, actualSwipe, dY, actionState, isCurrentlyActive);
            }
        };
        
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvMessages);
    }

    // Message Adapter
    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        private final List<Message> messageList;
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;
        private Map<String, Message> messagesMap = new HashMap<>();

        public MessageAdapter(List<Message> messageList) {
            this.messageList = messageList;
        }
        
        public void setMessagesMap(Map<String, Message> messagesMap) {
            this.messagesMap = messagesMap;
        }

        @Override
        public int getItemViewType(int position) {
            Message message = messageList.get(position);
            if (message.getSenderId().equals(currentUser.getUid())) {
                return VIEW_TYPE_SENT;
            } else {
                return VIEW_TYPE_RECEIVED;
            }
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_SENT) {
                view = getLayoutInflater().inflate(R.layout.item_message_sent, parent, false);
            } else {
                view = getLayoutInflater().inflate(R.layout.item_message_received, parent, false);
            }
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = messageList.get(position);
            
            // Set message text if present
            if (message.hasText()) {
                holder.tvMessage.setVisibility(View.VISIBLE);
                holder.tvMessage.setText(message.getText());
            } else {
                holder.tvMessage.setVisibility(View.GONE);
            }
            
            // Set message time
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(message.getTimestamp())));
            
            // Set read status for outgoing messages
            if (getItemViewType(position) == VIEW_TYPE_SENT && holder.ivReadStatus != null) {
                // Listen for real-time changes to lastReadMessageId
                DatabaseReference lastReadRef = chatRef.child("lastReadMessageId").child(recipientId);
                
                // First attach a ValueEventListener to get real-time updates
                lastReadRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Get the last read message ID from the recipient
                        String lastReadId = dataSnapshot.getValue(String.class);
                        boolean isRead = false;
                        
                        if (lastReadId != null) {
                            // Check if this message's timestamp is less than or equal to the last read message timestamp
                            // which means it's been read
                            for (Message m : messageList) {
                                if (m.getMessageId().equals(lastReadId)) {
                                    isRead = message.getTimestamp() <= m.getTimestamp();
                                    break;
                                }
                            }
                            
                            // Also check directly if this message is specifically marked as read
                            if (message.isReadBy(recipientId)) {
                                isRead = true;
                            }
                        }
                        
                        // Update the UI based on the read status
                        holder.ivReadStatus.setImageResource(isRead ? R.drawable.ic_read : R.drawable.ic_delivered);
                        holder.ivReadStatus.setVisibility(View.VISIBLE);
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // In case of error, show the delivered status
                        holder.ivReadStatus.setImageResource(R.drawable.ic_delivered);
                        holder.ivReadStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
            
            // Handle image if present
            if (message.hasImage() && holder.ivImage != null) {
                holder.ivImage.setVisibility(View.VISIBLE);
                
                String imageUrl = message.getImageUrl();
                if (imageUrl.startsWith("data:image")) {
                    try {
                        String base64Image = imageUrl.split(",")[1];
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        holder.ivImage.setImageBitmap(decodedBitmap);
                    } catch (Exception e) {
                        Log.e("ChatActivity", "Error loading image", e);
                        holder.ivImage.setVisibility(View.GONE);
                    }
                }
                
                holder.ivImage.setOnClickListener(v -> showFullScreenImage(message.getImageUrl()));
            } else if (holder.ivImage != null) {
                holder.ivImage.setVisibility(View.GONE);
            }
            
            // Handle reply if present
            if (message.getReplyToMessageId() != null && holder.replyContainer != null) {
                holder.replyContainer.setVisibility(View.VISIBLE);
                
                // Find the referenced message
                Message repliedToMessage = null;
                for (Message msg : messageList) {
                    if (message.getReplyToMessageId().equals(msg.getMessageId())) {
                        repliedToMessage = msg;
                        break;
                    }
                }
                
                if (repliedToMessage != null) {
                    // Set who the message is replying to
                    String replyAuthor = repliedToMessage.getSenderId().equals(currentUser.getUid()) ? "Вы" : recipientName;
                    holder.tvReplyAuthor.setText(replyAuthor);
                    
                    // Set the preview of the replied message
                    String replyText = repliedToMessage.getText();
                    if (TextUtils.isEmpty(replyText) && repliedToMessage.hasImage()) {
                        replyText = "[Изображение]";
                    }
                    holder.tvReplyText.setText(replyText);
                    
                    // Make the reply container clickable to navigate to the original message
                    final Message finalRepliedToMessage = repliedToMessage;
                    holder.replyContainer.setOnClickListener(v -> scrollToMessage(finalRepliedToMessage.getMessageId()));
                    
                    // Add visual indication that it's clickable
                    holder.replyContainer.setBackgroundResource(R.drawable.clickable_reply_background);
                } else {
                    holder.tvReplyAuthor.setText("");
                    holder.tvReplyText.setText("Сообщение удалено или недоступно");
                    // Remove clickability for non-existing messages
                    holder.replyContainer.setOnClickListener(null);
                    holder.replyContainer.setBackgroundResource(R.drawable.reply_background);
                }
            } else if (holder.replyContainer != null) {
                holder.replyContainer.setVisibility(View.GONE);
            }
            
            // Set long click listener for message options
            holder.itemView.setOnLongClickListener(v -> {
                showMessageOptionsDialog(message, position);
                return true;
            });
        }
        
        private void showMessageOptionsDialog(Message message, int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
            boolean isSentMessage = message.getSenderId().equals(currentUser.getUid());
            
            if (isSentMessage) {
                builder.setItems(new String[]{"Ответить", "Копировать", "Изменить", "Удалить"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Ответить
                            replyToMessage(message);
                            break;
                        case 1: // Копировать
                            copyMessageToClipboard(message);
                            break;
                        case 2: // Изменить
                            editMessage(message);
                            break;
                        case 3: // Удалить
                            deleteMessage(message);
                            break;
                    }
                });
            } else {
                builder.setItems(new String[]{"Ответить", "Копировать", "Переслать"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Ответить
                            replyToMessage(message);
                            break;
                        case 1: // Копировать
                            copyMessageToClipboard(message);
                            break;
                        case 2: // Переслать
                            // TODO: Реализовать пересылку сообщений
                            Toast.makeText(ChatActivity.this, "Функция пересылки сообщений пока не реализована", Toast.LENGTH_SHORT).show();
                            break;
                    }
                });
            }
            
            builder.show();
        }
        
        private void copyMessageToClipboard(Message message) {
            if (!message.hasText()) return;
            
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Сообщение", message.getText());
            clipboard.setPrimaryClip(clip);
            
            Toast.makeText(ChatActivity.this, "Скопировано", Toast.LENGTH_SHORT).show();
        }
        
        private void editMessage(Message message) {
            // Проверяем, что сообщение принадлежит текущему пользователю
            if (!message.getSenderId().equals(currentUser.getUid())) {
                Toast.makeText(ChatActivity.this, "Можно редактировать только свои сообщения", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Создаем диалог с полем ввода
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
            builder.setTitle("Редактировать сообщение");
            
            final EditText input = new EditText(ChatActivity.this);
            input.setText(message.getText());
            input.setSelection(input.getText().length());
            builder.setView(input);
            
            builder.setPositiveButton("Сохранить", (dialog, which) -> {
                String newText = input.getText().toString().trim();
                if (TextUtils.isEmpty(newText)) {
                    Toast.makeText(ChatActivity.this, "Текст не может быть пустым", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Обновляем текст сообщения
                message.setText(newText);
                
                // Обновляем сообщение в базе данных
                Map<String, Object> updates = new HashMap<>();
                updates.put("text", newText);
                // В нашей текущей реализации нет setEdited, поэтому обновляем только текст
                messagesRef.child(message.getMessageId()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ChatActivity.this, "Сообщение обновлено", Toast.LENGTH_SHORT).show();
                        // Если это последнее сообщение, обновляем его текст в чате
                        chatRef.child("lastMessageSenderId").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String lastMessageSenderId = dataSnapshot.getValue(String.class);
                                if (lastMessageSenderId != null && lastMessageSenderId.equals(currentUser.getUid())) {
                                    chatRef.child("lastMessageText").setValue(newText);
                                }
                            }
                            
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {}
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, 
                            "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
            
            builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
            
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        
        private void deleteMessage(Message message) {
            new AlertDialog.Builder(ChatActivity.this)
                    .setTitle("Удалить сообщение")
                    .setMessage("Вы уверены, что хотите удалить это сообщение?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        messagesRef.child(message.getMessageId()).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ChatActivity.this, "Сообщение удалено", Toast.LENGTH_SHORT).show();
                                    
                                    // Check if this was the last message in the chat
                                    chatRef.child("lastMessageSenderId").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            String lastMessageId = message.getMessageId();
                                            // If this message was the last message in the chat
                                            if (dataSnapshot.exists() && 
                                                message.getSenderId().equals(dataSnapshot.getValue(String.class))) {
                                                
                                                // Find the new last message
                                                Message newLastMessage = null;
                                                long latestTimestamp = 0;
                                                
                                                for (Message msg : messageList) {
                                                    // Skip the deleted message
                                                    if (msg.getMessageId().equals(message.getMessageId())) {
                                                        continue;
                                                    }
                                                    
                                                    if (msg.getTimestamp() > latestTimestamp) {
                                                        latestTimestamp = msg.getTimestamp();
                                                        newLastMessage = msg;
                                                    }
                                                }
                                                
                                                // Update the last message info in the chat
                                                Map<String, Object> updates = new HashMap<>();
                                                if (newLastMessage != null) {
                                                    String text = newLastMessage.getText();
                                                    if (text == null || text.isEmpty()) {
                                                        if (newLastMessage.hasImage()) {
                                                            text = "[Изображение]";
                                                        } else {
                                                            text = "";
                                                        }
                                                    }
                                                    
                                                    updates.put("lastMessageText", text);
                                                    updates.put("lastMessageTimestamp", newLastMessage.getTimestamp());
                                                    updates.put("lastMessageSenderId", newLastMessage.getSenderId());
                                                } else {
                                                    // No messages left in the chat
                                                    updates.put("lastMessageText", "");
                                                    updates.put("lastMessageTimestamp", System.currentTimeMillis());
                                                    updates.put("lastMessageSenderId", "");
                                                }
                                                
                                                chatRef.updateChildren(updates)
                                                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, 
                                                        "Ошибка обновления чата: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                            }
                                        }
                                        
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            // Ignore error
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> 
                                        Toast.makeText(ChatActivity.this, "Ошибка при удалении сообщения", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        }

        @Override
        public int getItemCount() {
            return messageList.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;
            TextView tvTime;
            ImageView ivImage;
            ImageView ivReadStatus;
            CardView replyContainer;
            TextView tvReplyAuthor, tvReplyText;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
                ivImage = itemView.findViewById(R.id.ivImage);
                ivReadStatus = itemView.findViewById(R.id.ivReadStatus);
                replyContainer = itemView.findViewById(R.id.replyContainer);
                tvReplyAuthor = itemView.findViewById(R.id.tvReplyAuthor);
                tvReplyText = itemView.findViewById(R.id.tvReplyText);
            }
        }
    }

    private void updateTypingStatus(boolean isTyping) {
        if (currentUser != null && recipientId != null) {
            // First, check if we're already in the requested state to avoid unnecessary updates
            usersRef.child(currentUser.getUid()).child("currentActivity").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String currentActivity = dataSnapshot.getValue(String.class);
                    boolean currentlyTyping = "typing".equals(currentActivity);
                    
                    // Only update if the state is changing
                    if (isTyping != currentlyTyping) {
                        String activityValue = isTyping ? "typing" : null;
                        
                        // If we're switching from choosing photo to not typing, preserve the choosing photo status
                        if (!isTyping && "choosing_photo".equals(currentActivity)) {
                            // Don't change the status
                            return;
                        }
                        
                        // Otherwise update the status
                        usersRef.child(currentUser.getUid()).child("currentActivity").setValue(activityValue);
                        
                        // Also update the typing flag directly to ensure the model state is consistent
                        if (isTyping) {
                            usersRef.child(currentUser.getUid()).child("typing").setValue(true);
                        } else {
                            usersRef.child(currentUser.getUid()).child("typing").setValue(false);
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // In case of error, still try to update
                    String activityValue = isTyping ? "typing" : null;
                    usersRef.child(currentUser.getUid()).child("currentActivity").setValue(activityValue);
                }
            });
        }
    }
    
    private void updateChoosingPhotoStatus(boolean isChoosing) {
        if (currentUser != null && recipientId != null) {
            if (isChoosing) {
                // Apply all updates immediately for photo selection to ensure online status
                Map<String, Object> updates = new HashMap<>();
                updates.put("currentActivity", "choosing_photo");
                updates.put("choosingPhoto", true);
                updates.put("online", true);
                
                // Set a very long timeout to ensure the user stays online during photo selection
                long extendedOnlineTime = System.currentTimeMillis() + ONLINE_HEARTBEAT_INTERVAL * 3; // Triple the normal timeout
                updates.put("onlineUntil", extendedOnlineTime);
                
                // Apply all updates atomically
                usersRef.child(currentUser.getUid()).updateChildren(updates);
            } else {
                // Check current activity when canceling photo selection
                usersRef.child(currentUser.getUid()).child("currentActivity").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String currentActivity = dataSnapshot.getValue(String.class);
                        
                        // If they were typing before, maintain typing status
                        if ("typing".equals(currentActivity)) {
                            // Don't change the status, maintain typing
                            usersRef.child(currentUser.getUid()).child("choosingPhoto").setValue(false);
                        } else {
                            // Reset activity status but maintain online status
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("currentActivity", null);
                            updates.put("choosingPhoto", false);
                            updates.put("online", true); // Ensure user stays online
                            
                            // Refresh online timeout
                            long onlineUntil = System.currentTimeMillis() + ONLINE_HEARTBEAT_INTERVAL + 3000;
                            updates.put("onlineUntil", onlineUntil);
                            
                            usersRef.child(currentUser.getUid()).updateChildren(updates);
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Fallback in case of error - at least reset the choosingPhoto flag
                        usersRef.child(currentUser.getUid()).child("choosingPhoto").setValue(false);
                        usersRef.child(currentUser.getUid()).child("online").setValue(true);
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Reset the choosing photo status
        updateChoosingPhotoStatus(false);
        
        // Explicitly update online status to make sure we're shown as online
        // This prevents the user from appearing offline after returning from photo selection
        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            long onlineUntil = System.currentTimeMillis() + ONLINE_HEARTBEAT_INTERVAL + 3000;
            updates.put("online", true);
            updates.put("onlineUntil", onlineUntil);
            usersRef.child(currentUser.getUid()).updateChildren(updates);
        }
    }

    // Add method to start the online heartbeat timer
    private void startOnlineHeartbeat() {
        if (onlineHeartbeatTimer != null) {
            onlineHeartbeatTimer.cancel();
        }
        
        onlineHeartbeatTimer = new Timer();
        onlineHeartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateOnlineStatus(true);
            }
        }, 0, ONLINE_HEARTBEAT_INTERVAL);
    }

    // Add method to stop the online heartbeat timer
    private void stopOnlineHeartbeat() {
        if (onlineHeartbeatTimer != null) {
            onlineHeartbeatTimer.cancel();
            onlineHeartbeatTimer = null;
            updateOnlineStatus(false);
        }
    }

    // Method to scroll to a specific message by ID
    private void scrollToMessage(String messageId) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getMessageId().equals(messageId)) {
                rvMessages.smoothScrollToPosition(i);
                
                // Highlight the message briefly
                View view = rvMessages.getLayoutManager().findViewByPosition(i);
                if (view != null) {
                    view.setBackgroundColor(getResources().getColor(R.color.highlight_color, null));
                    new Handler().postDelayed(() -> 
                        view.setBackgroundColor(Color.TRANSPARENT), 1000);
                }
                break;
            }
        }
    }

    // Add the updateOnlineStatus for boolean parameter for user's online status
    private void updateOnlineStatus(boolean isOnline) {
        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            
            if (isOnline) {
                // If online, set the online flag and update onlineUntil timestamp
                long onlineUntil = System.currentTimeMillis() + ONLINE_HEARTBEAT_INTERVAL + 3000; // Current time + heartbeat interval + 3 seconds buffer
                updates.put("online", true);
                updates.put("onlineUntil", onlineUntil);
            } else {
                // If going offline, update lastSeen timestamp and set online to false
                updates.put("online", false);
                updates.put("lastSeen", ServerValue.TIMESTAMP);
                // Set onlineUntil to a short timeout (3 seconds grace period)
                long shortTimeout = System.currentTimeMillis() + 3000;
                updates.put("onlineUntil", shortTimeout);
            }
            
            usersRef.child(currentUser.getUid()).updateChildren(updates);
        }
    }
} 