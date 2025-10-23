package com.example.imageviewdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FullscreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        ImageView imageView = findViewById(R.id.fullscreenImageView);
        TextView titleView = findViewById(R.id.fullscreenTitle);
        TextView contentView = findViewById(R.id.fullscreenContent);
        TextView authorView = findViewById(R.id.fullscreenAuthor);

        String key = getIntent().getStringExtra("image_key");
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String author = getIntent().getStringExtra("author");

        Bitmap bitmap = ImageStore.take(key);

        if (bitmap == null) {
            Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageView.setImageBitmap(bitmap);
        titleView.setText(title);
        contentView.setText(content);

        if (author == null || author.isEmpty()) {
            authorView.setText("");
        } else {
            authorView.setText(author);
        }
    }
}
