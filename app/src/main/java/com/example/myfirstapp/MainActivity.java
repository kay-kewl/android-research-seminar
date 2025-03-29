package com.example.myfirstapp;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "VideoApp";
    
    private PlayerView playerView;
    private ExoPlayer player;
    private RecyclerView videoListView;
    private List<VideoItem> videoList;
    private VideoAdapter adapter;
    
    // Хранение всех доступных видео
    private List<VideoItem> allVideos;
    
    // Хранение текущего просматриваемого видео 
    private VideoItem currentlyWatchingVideo = null;
    
    // URL для рикролла - "Never Gonna Give You Up"
    private final String VIDEO_URL = 
        "https://ia800206.us.archive.org/29/items/Rick_Astley_Never_Gonna_Give_You_Up/Rick_Astley_Never_Gonna_Give_You_Up.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Инициализация плеера
        playerView = findViewById(R.id.player_view);
        
        // Инициализация списка видео
        videoListView = findViewById(R.id.video_list);
        videoListView.setLayoutManager(new LinearLayoutManager(this));
        
        // Создаем список всех возможных видео
        createAllVideosList();
        
        // Установка начального просматриваемого видео (выбираем случайное из всех видео)
        Random random = new Random();
        currentlyWatchingVideo = allVideos.get(random.nextInt(allVideos.size()));
        
        // Инициализируем список рекомендованных видео - все видео кроме просматриваемого
        updateRecommendedVideosList();
        
        // Устанавливаем адаптер для списка
        adapter = new VideoAdapter(videoList, this::handleVideoClick);
        videoListView.setAdapter(adapter);
        
        // Инициализация плеера
        initializePlayer();
        
        // Помещаем рикролл на начальный экран
        playVideo(VIDEO_URL);
    }
    
    private void handleVideoClick(VideoItem clickedVideo) {
        // Воспроизводим рикролл
        playVideo(VIDEO_URL);
        
        // Показываем сообщение
        Toast.makeText(MainActivity.this, 
                "Воспроизводится: " + clickedVideo.getTitle(), 
                Toast.LENGTH_SHORT).show();
        
        // Устанавливаем нажатое видео как текущее просматриваемое
        currentlyWatchingVideo = clickedVideo;
        
        // Обновляем список рекомендованных видео
        updateRecommendedVideosList();
        
        // Уведомляем адаптер об изменениях
        adapter.notifyDataSetChanged();
    }
    
    /**
     * Обновляет список рекомендованных видео, исключая текущее просматриваемое видео
     */
    private void updateRecommendedVideosList() {
        // Очищаем текущий список
        if (videoList == null) {
            videoList = new ArrayList<>();
        } else {
            videoList.clear();
        }
        
        // Добавляем все видео, кроме просматриваемого
        for (VideoItem video : allVideos) {
            if (currentlyWatchingVideo == null || 
                !video.getTitle().equals(currentlyWatchingVideo.getTitle())) {
                videoList.add(video);
            }
        }
        
        // Перемешиваем список для эффекта рекомендаций
        Collections.shuffle(videoList);
    }
    
    private void createAllVideosList() {
        allVideos = new ArrayList<>();
        
        // Добавляем все возможные видео в список
        allVideos.add(new VideoItem("Новые приключения в мире технологий", 
                "2,5 млн просмотров • 2 дня назад", R.drawable.tech_thumbnail));
        allVideos.add(new VideoItem("Как научиться программировать за 30 дней",
                "1,2 млн просмотров • 1 неделю назад", R.drawable.programming_thumbnail));
        allVideos.add(new VideoItem("Топ-10 мобильных приложений 2025 года", 
                "3,8 млн просмотров • 3 дня назад", R.drawable.apps_thumbnail));
        allVideos.add(new VideoItem("Обзор нового Android 15", 
                "5,1 млн просмотров • 4 дня назад", R.drawable.android_thumbnail));
        allVideos.add(new VideoItem("Секреты эффективной работы из дома", 
                "950 тыс просмотров • 2 недели назад", R.drawable.work_thumbnail));
        allVideos.add(new VideoItem("Самые ожидаемые фильмы 2025 года", 
                "4,2 млн просмотров • 5 дней назад", R.drawable.movies_thumbnail));
        allVideos.add(new VideoItem("Стрим Doom 3", 
                "2 просмотра • 4 дня назад", R.drawable.doom_thumbnail));
    }
    
    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        // Устанавливаем обработчик ошибок
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage());
                Toast.makeText(MainActivity.this, 
                        "Ошибка воспроизведения: " + error.getMessage(), 
                        Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void playVideo(String videoUrl) {
        player.stop();
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
    
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
    
    // Класс для представления видео в списке
    static class VideoItem {
        private final String title;
        private final String info;
        private final int thumbnailResId;
        
        public VideoItem(String title, String info, int thumbnailResId) {
            this.title = title;
            this.info = info;
            this.thumbnailResId = thumbnailResId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getInfo() {
            return info;
        }
        
        public int getThumbnailResId() {
            return thumbnailResId;
        }
    }
    
    // Адаптер для списка видео
    static class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        
        private final List<VideoItem> videoList;
        private final VideoClickListener clickListener;
        
        public VideoAdapter(List<VideoItem> videoList, VideoClickListener clickListener) {
            this.videoList = videoList;
            this.clickListener = clickListener;
        }
        
        @Override
        public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_item, parent, false);
            return new VideoViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(VideoViewHolder holder, int position) {
            VideoItem video = videoList.get(position);
            holder.titleTextView.setText(video.getTitle());
            holder.infoTextView.setText(video.getInfo());
            holder.thumbnailImageView.setImageResource(video.getThumbnailResId());
            
            holder.itemView.setOnClickListener(v -> clickListener.onVideoClick(video));
        }
        
        @Override
        public int getItemCount() {
            return videoList.size();
        }
        
        static class VideoViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnailImageView;
            TextView titleTextView;
            TextView infoTextView;
            
            public VideoViewHolder(View itemView) {
                super(itemView);
                thumbnailImageView = itemView.findViewById(R.id.video_thumbnail);
                titleTextView = itemView.findViewById(R.id.video_title);
                infoTextView = itemView.findViewById(R.id.video_info);
            }
        }
        
        interface VideoClickListener {
            void onVideoClick(VideoItem video);
        }
    }
} 