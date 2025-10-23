package com.example.imageviewdemo;

import android.graphics.Bitmap;

/**
 * Simple value object to hold post metadata and bitmap.
 */
public class Post {
    private final int id;
    private final String title;
    private final String content;
    private final String author;
    private final String imageUrl;
    private final Bitmap bitmap;
    private final boolean ownedByUser;
    private boolean favorite;

    public Post(int id,
                String title,
                String content,
                String author,
                String imageUrl,
                Bitmap bitmap,
                boolean ownedByUser,
                boolean favorite) {
        this.id = id;
        this.title = title != null ? title : "";
        this.content = content != null ? content : "";
        this.author = author != null ? author : "";
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.bitmap = bitmap;
        this.ownedByUser = ownedByUser;
        this.favorite = favorite;
    }

    public int getId() {
        return id;
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

    public boolean isOwnedByUser() {
        return ownedByUser;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
