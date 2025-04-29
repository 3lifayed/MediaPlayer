package com.example.alifa.mediaplayer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity {

    Button btn_next, btn_previous, btn_pause, btn_shuffle;
    TextView songTextLabel, timeElapsed, timeRemaining;
    ImageView albumArtImage;
    SeekBar songSeekbar;
    static MediaPlayer myMediaPlayer;
    Thread updateseekbar;
    String sname;
    static ArrayList<Uri> mySongs;
    static int position;
    boolean isShuffleEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        btn_next = findViewById(R.id.next);
        btn_previous = findViewById(R.id.previous);
        btn_pause = findViewById(R.id.pause);
        btn_shuffle = findViewById(R.id.shuffle);
        albumArtImage = findViewById(R.id.albumArtImage);
        songTextLabel = findViewById(R.id.songLabel);
        songSeekbar = findViewById(R.id.seekBar);
        timeElapsed = findViewById(R.id.time_elapsed);
        timeRemaining = findViewById(R.id.time_remaining);

        getSupportActionBar().setTitle("Now Playing");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (myMediaPlayer != null) {
            myMediaPlayer.stop();
            myMediaPlayer.release();
        }

        Intent i = getIntent();
        String songUri = i.getStringExtra("uri");
        sname = i.getStringExtra("songname");

        mySongs = (ArrayList<Uri>) i.getSerializableExtra("songs");
        position = i.getIntExtra("pos", 0);

        Uri uri = Uri.parse(songUri);
        myMediaPlayer = MediaPlayer.create(getApplicationContext(), uri);

        songTextLabel.setText(sname);
        songTextLabel.setSelected(true);

        updateAlbumArt(uri);

        myMediaPlayer.start();
        songSeekbar.setMax(myMediaPlayer.getDuration());

        updateseekbar = new Thread(() -> {
            int totalDuration = myMediaPlayer.getDuration();
            while (myMediaPlayer != null && myMediaPlayer.isPlaying()) {
                try {
                    Thread.sleep(500);
                    int currentPosition = myMediaPlayer.getCurrentPosition();
                    runOnUiThread(() -> {
                        songSeekbar.setProgress(currentPosition);
                        timeElapsed.setText(formatTime(currentPosition));
                        timeRemaining.setText("-" + formatTime(totalDuration - currentPosition));
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        updateseekbar.start();

        songSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                myMediaPlayer.seekTo(seekBar.getProgress());
            }
        });

        btn_pause.setOnClickListener(v -> {
            songSeekbar.setMax(myMediaPlayer.getDuration());
            if (myMediaPlayer.isPlaying()) {
                btn_pause.setBackgroundResource(R.drawable.icon_play);
                myMediaPlayer.pause();
            } else {
                btn_pause.setBackgroundResource(R.drawable.icon_pause);
                myMediaPlayer.start();
            }
        });

        btn_next.setOnClickListener(v -> playNext());
        btn_previous.setOnClickListener(v -> playPrevious());

        btn_shuffle.setOnClickListener(v -> {
            isShuffleEnabled = !isShuffleEnabled;
            if (isShuffleEnabled) {
                btn_shuffle.setBackgroundResource(R.drawable.ic_shuffle_on);
                Toast.makeText(this, "Shuffle On", Toast.LENGTH_SHORT).show();
            } else {
                btn_shuffle.setBackgroundResource(R.drawable.ic_shuffle);
                Toast.makeText(this, "Shuffle Off", Toast.LENGTH_SHORT).show();
            }
        });

        myMediaPlayer.setOnCompletionListener(mp -> playNext());
    }

    private void playNext() {
        myMediaPlayer.stop();
        myMediaPlayer.release();
        if (isShuffleEnabled) {
            position = new Random().nextInt(mySongs.size());
        } else {
            position = (position + 1) % mySongs.size();
        }
        playSongAtPosition();
    }

    private void playPrevious() {
        myMediaPlayer.stop();
        myMediaPlayer.release();
        position = (position - 1) < 0 ? (mySongs.size() - 1) : (position - 1);
        playSongAtPosition();
    }

    private void playSongAtPosition() {
        Uri uri = mySongs.get(position);
        myMediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        sname = uri.getLastPathSegment();
        songTextLabel.setText(sname.replace(".mp3", "").replace(".m4a", ""));
        songTextLabel.setSelected(true);
        updateAlbumArt(uri);
        myMediaPlayer.start();
        songSeekbar.setMax(myMediaPlayer.getDuration());
        myMediaPlayer.setOnCompletionListener(mp -> playNext());
    }

    private void updateAlbumArt(Uri uri) {
        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(getApplicationContext(), uri);
            byte[] artBytes = mmr.getEmbeddedPicture();
            if (artBytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
                albumArtImage.setImageBitmap(bitmap);
            } else {
                albumArtImage.setImageResource(R.drawable.cover_art);
            }
        } catch (Exception e) {
            albumArtImage.setImageResource(R.drawable.cover_art);
        }
    }

    private String formatTime(int millis) {
        int minutes = millis / 1000 / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
