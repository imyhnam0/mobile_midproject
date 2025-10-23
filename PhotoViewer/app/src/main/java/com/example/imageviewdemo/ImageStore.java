package com.example.imageviewdemo;

import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple in-memory cache for passing bitmaps between activities.
 */
public final class ImageStore {
    private static final Map<String, Bitmap> CACHE = new HashMap<>();

    private ImageStore() {
        // no-op
    }

    public static String put(Bitmap bitmap) {
        String key = UUID.randomUUID().toString();
        CACHE.put(key, bitmap);
        return key;
    }

    public static Bitmap take(String key) {
        if (key == null) {
            return null;
        }
        return CACHE.remove(key);
    }
}
