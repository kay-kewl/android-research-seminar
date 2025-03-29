package com.example.myfirstapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myfirstapp.model.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    
    private final List<Message> messageList;
    private final String currentUserId;
    private OnMessageClickListener messageClickListener;
    private OnMessageLongClickListener messageLongClickListener;
    private Map<String, Message> messagesMap;
    
    public interface OnMessageClickListener {
        void onMessageClick(int position, View view);
        void onImageClick(int position, ImageView imageView);
    }
    
    public interface OnMessageLongClickListener {
        boolean onMessageLongClick(int position, View view);
    }
    
    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
    
    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.messageClickListener = listener;
    }
    
    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.messageLongClickListener = listener;
    }
    
    public void setMessagesMap(Map<String, Message> messagesMap) {
        this.messagesMap = messagesMap;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        
        // Set message text
        if (message.hasText()) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(message.getText());
        } else {
            holder.tvMessage.setVisibility(View.GONE);
        }
        
        // Set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(message.getTimestamp())));
        
        // Set read status (only for sent messages)
        if (getItemViewType(position) == VIEW_TYPE_SENT && holder.ivReadStatus != null) {
            if (message.isReadBy(currentUserId) && message.isReadByAll(getParticipantsMap())) {
                holder.ivReadStatus.setImageResource(R.drawable.ic_read);
                holder.ivReadStatus.setVisibility(View.VISIBLE);
            } else {
                holder.ivReadStatus.setImageResource(R.drawable.ic_delivered);
                holder.ivReadStatus.setVisibility(View.VISIBLE);
            }
        }
        
        // Set reply info if this message is a reply
        if (message.isReply() && holder.replyContainer != null) {
            holder.replyContainer.setVisibility(View.VISIBLE);
            
            // Find the original message from our map
            Message originalMessage = null;
            if (messagesMap != null) {
                originalMessage = messagesMap.get(message.getReplyToMessageId());
            }
            
            if (originalMessage != null) {
                // Set original message info
                if (originalMessage.getSenderId().equals(currentUserId)) {
                    holder.tvReplyAuthor.setText("Вы");
                } else {
                    holder.tvReplyAuthor.setText("Собеседник");
                }
                
                if (originalMessage.hasText()) {
                    holder.tvReplyText.setText(originalMessage.getText());
                } else if (originalMessage.hasImage()) {
                    holder.tvReplyText.setText("[Изображение]");
                } else {
                    holder.tvReplyText.setText("");
                }
            } else {
                holder.tvReplyAuthor.setText("");
                holder.tvReplyText.setText("Сообщение удалено");
            }
        } else if (holder.replyContainer != null) {
            holder.replyContainer.setVisibility(View.GONE);
        }
        
        // Set image if present
        if (message.hasImage() && holder.ivImage != null) {
            holder.ivImage.setVisibility(View.VISIBLE);
            
            if (message.getImageUrl().startsWith("data:image")) {
                try {
                    String base64Image = message.getImageUrl().split(",")[1];
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    
                    // Установка минимальной и максимальной ширины/высоты для картинки
                    ViewGroup.LayoutParams params = holder.ivImage.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    holder.ivImage.setLayoutParams(params);
                    holder.ivImage.setMinimumWidth(250); // Минимальная ширина
                    holder.ivImage.setMaxHeight(350);    // Максимальная высота
                    
                    holder.ivImage.setImageBitmap(decodedBitmap);
                } catch (Exception e) {
                    holder.ivImage.setVisibility(View.GONE);
                }
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(message.getImageUrl())
                        .into(holder.ivImage);
            }
            
            holder.ivImage.setOnClickListener(v -> {
                if (messageClickListener != null) {
                    messageClickListener.onImageClick(holder.getAdapterPosition(), holder.ivImage);
                }
            });
        } else if (holder.ivImage != null) {
            holder.ivImage.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (messageClickListener != null) {
                messageClickListener.onMessageClick(holder.getAdapterPosition(), v);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (messageLongClickListener != null) {
                return messageLongClickListener.onMessageLongClick(holder.getAdapterPosition(), v);
            }
            return false;
        });
    }
    
    private Map<String, Boolean> getParticipantsMap() {
        // В реальном приложении вы должны получить список участников чата
        // Здесь мы возвращаем пустую карту как заглушку
        return Map.of();
    }
    
    @Override
    public int getItemCount() {
        return messageList.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        String senderId = messageList.get(position).getSenderId();
        if (senderId.equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }
    
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvReplyAuthor, tvReplyText;
        ImageView ivImage, ivReadStatus;
        CardView replyContainer;
        ConstraintLayout messageContainer;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            
            // Эти view не всегда присутствуют в обоих типах сообщений
            ivImage = itemView.findViewById(R.id.ivImage);
            ivReadStatus = itemView.findViewById(R.id.ivReadStatus);
            replyContainer = itemView.findViewById(R.id.replyContainer);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            tvReplyAuthor = itemView.findViewById(R.id.tvReplyAuthor);
            tvReplyText = itemView.findViewById(R.id.tvReplyText);
        }
    }
} 