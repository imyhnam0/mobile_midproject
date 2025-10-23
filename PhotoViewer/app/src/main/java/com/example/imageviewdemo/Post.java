package com.example.imageviewdemo;

import android.graphics.Bitmap;

/**
 * Simple value object to hold post metadata and bitmap.
 */
public class Post {
    private final String title;
    private final String content;
    private final String author;
    private final String imageUrl;
    private final Bitmap bitmap;

    public Post(String title, String content, String author, String imageUrl, Bitmap bitmap) {
        this.title = title != null ? title : "";
        this.content = content != null ? content : "";
        this.author = author != null ? author : "";
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.bitmap = bitmap;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
