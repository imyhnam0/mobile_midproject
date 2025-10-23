package com.example.imageviewdemo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String AUTH_TOKEN = "87af38f0adbac939bfd69b8063bb674cc9b85b5c";
    private static final float TOTAL_CAPACITY_MB = 1024f;
    private static final String PREFS_NAME = "photo_viewer_prefs";
    private static final String PREF_FAVORITE_KEYS = "favorite_post_keys";
    private static final String CURRENT_USERNAME = "yunhyungnam"; // TODO: replace with dynamic user lookup if available.

    private final String siteUrl = "https://yunhyungnam.pythonanywhere.com";
    private final String postUrl = siteUrl + "/api_root/Post/";

    private TextView statusView;
    private TextView storageInfoView;
    private EditText searchView;
    private CheckBox favoritesOnlyCheck;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;

    private final List<Post> postList = new ArrayList<>();
    private final List<Post> filteredPosts = new ArrayList<>();
    private final Set<String> favoriteKeys = new HashSet<>();

    private boolean showFavoritesOnly = false;
    private String currentQuery = "";
    private Uri selectedImageUri;
    private String inputTitle;
    private String inputText;

    private CloadImage taskDownload;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> storedFavorites = sharedPreferences.getStringSet(PREF_FAVORITE_KEYS, new HashSet<>());
        if (storedFavorites != null) {
            favoriteKeys.addAll(storedFavorites);
        }

        statusView = findViewById(R.id.textView);
        storageInfoView = findViewById(R.id.textStorageInfo);
        searchView = findViewById(R.id.editSearch);
        favoritesOnlyCheck = findViewById(R.id.checkFavoritesOnly);
        recyclerView = findViewById(R.id.recyclerView);

        imageAdapter = new ImageAdapter(new ImageAdapter.Listener() {
            @Override
            public void onItemClick(Post post) {
                openFullscreen(post);
            }

            @Override
            public void onToggleFavorite(Post post) {
                toggleFavorite(post);
            }

            @Override
            public void onDelete(Post post) {
                confirmDelete(post);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(imageAdapter);

        if (searchView != null) {
            searchView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // no-op
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterPosts(s != null ? s.toString() : "");
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // no-op
                }
            });
        }

        if (favoritesOnlyCheck != null) {
            favoritesOnlyCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                showFavoritesOnly = isChecked;
                filterPosts(searchView != null ? searchView.getText().toString() : "");
            });
        }

        updateStorageInfoFromPosts();
        loadPosts();
    }

    private void loadPosts() {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(postUrl);
        if (statusView != null) {
            statusView.setText("이미지를 불러오는 중...");
        }
    }

    public void onClickUpload(View v) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload, null);
        EditText dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        EditText dialogText = dialogView.findViewById(R.id.dialogText);
        Button selectImage = dialogView.findViewById(R.id.dialogSelectImage);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("새 게시글 작성")
                .setView(dialogView)
                .setPositiveButton("업로드", null)
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .create();

        dialog.show();

        selectImage.setOnClickListener(v2 -> {
            inputTitle = dialogTitle.getText().toString().trim();
            inputText = dialogText.getText().toString().trim();

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "사진 선택"), PICK_IMAGE_REQUEST);
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
            inputTitle = dialogTitle.getText().toString().trim();
            inputText = dialogText.getText().toString().trim();

            if (selectedImageUri == null) {
                Toast.makeText(this, "사진을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputTitle == null || inputTitle.isEmpty() || inputText == null || inputText.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            new UploadTask().execute();
            dialog.dismiss();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                Toast.makeText(this, "사진이 선택되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClickDownload(View v) {
        loadPosts();
    }

    private void filterPosts(String query) {
        currentQuery = query != null ? query.trim() : "";
        String lowerQuery = currentQuery.toLowerCase();

        filteredPosts.clear();
        for (Post post : postList) {
            if (showFavoritesOnly && !post.isFavorite()) {
                continue;
            }
            if (!lowerQuery.isEmpty()) {
                if (!(containsIgnoreCase(post.getTitle(), lowerQuery)
                        || containsIgnoreCase(post.getContent(), lowerQuery)
                        || containsIgnoreCase(post.getAuthor(), lowerQuery))) {
                    continue;
                }
            }
            filteredPosts.add(post);
        }

        imageAdapter.submitList(filteredPosts);
        updateStatusText();
    }

    private void updateStatusText() {
        if (statusView == null) {
            return;
        }

        if (filteredPosts.isEmpty()) {
            if (postList.isEmpty()) {
                statusView.setText("불러올 이미지가 없습니다.");
            } else {
                statusView.setText("검색/필터 결과가 없습니다.");
            }
        } else {
            statusView.setText("총 " + filteredPosts.size() + "개의 사진");
        }
    }

    private boolean containsIgnoreCase(String source, String query) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(query);
    }

    private void openFullscreen(Post post) {
        if (post == null || post.getBitmap() == null) {
            Toast.makeText(this, "이미지를 표시할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = ImageStore.put(post.getBitmap());
        Intent intent = new Intent(this, FullscreenImageActivity.class);
        intent.putExtra("image_key", key);
        intent.putExtra("title", post.getTitle());
        intent.putExtra("content", post.getContent());
        intent.putExtra("author", post.getAuthor());
        startActivity(intent);
    }

    private void toggleFavorite(Post post) {
        if (post == null) {
            return;
        }
        boolean newState = !post.isFavorite();
        post.setFavorite(newState);

        String key = keyForPost(post);
        if (newState) {
            favoriteKeys.add(key);
        } else {
            favoriteKeys.remove(key);
        }
        persistFavorites();
        filterPosts(currentQuery);
    }

    private void persistFavorites() {
        sharedPreferences.edit()
                .putStringSet(PREF_FAVORITE_KEYS, new HashSet<>(favoriteKeys))
                .apply();
    }

    private void confirmDelete(Post post) {
        if (post == null) return;  // ✅ 권한 제한 제거

        new AlertDialog.Builder(this)
                .setTitle("게시글 삭제")
                .setMessage("정말 이 게시글을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (d, w) -> new DeletePostTask(post).execute())
                .setNegativeButton("취소", null)
                .show();
    }


    private void updateStorageInfoFromPosts() {
        if (storageInfoView == null) {
            return;
        }

        float usedMb = 0f;
        for (Post post : postList) {
            Bitmap bitmap = post.getBitmap();
            if (bitmap != null) {
                usedMb += bitmap.getByteCount() / (1024f * 1024f);
            }
        }
        if (usedMb < 0f) {
            usedMb = 0f;
        }
        float remainingMb = Math.max(TOTAL_CAPACITY_MB - usedMb, 0f);

        storageInfoView.setText(String.format(Locale.getDefault(),
                "서버 용량: %.1fMB 사용 / %.1fMB 남음 (총 %.1fMB)",
                usedMb, remainingMb, TOTAL_CAPACITY_MB));
    }

    private String keyForPost(Post post) {
        if (post.getId() >= 0) {
            return "id_" + post.getId();
        }
        return "url_" + post.getImageUrl().hashCode();
    }

    private class UploadTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            DataOutputStream out = null;
            InputStream inputStream = null;
            try {
                String boundary = "----AndroidBoundary";
                String LINE_FEED = "\r\n";

                conn = (HttpURLConnection) new URL(postUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setDoOutput(true);

                out = new DataOutputStream(conn.getOutputStream());

                out.writeBytes("--" + boundary + LINE_FEED);
                out.writeBytes("Content-Disposition: form-data; name=\"title\"" + LINE_FEED);
                out.writeBytes(LINE_FEED);
                out.writeBytes(inputTitle + LINE_FEED);

                out.writeBytes("--" + boundary + LINE_FEED);
                out.writeBytes("Content-Disposition: form-data; name=\"text\"" + LINE_FEED);
                out.writeBytes(LINE_FEED);
                out.writeBytes(inputText + LINE_FEED);

                out.writeBytes("--" + boundary + LINE_FEED);
                out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"" + LINE_FEED);
                out.writeBytes("Content-Type: image/jpeg" + LINE_FEED);
                out.writeBytes(LINE_FEED);

                inputStream = getContentResolver().openInputStream(selectedImageUri);
                if (inputStream == null) {
                    return false;
                }
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.writeBytes(LINE_FEED + "--" + boundary + "--" + LINE_FEED);
                out.flush();

                int responseCode = conn.getResponseCode();
                if (responseCode != 201 && responseCode != 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    reader.close();
                    Log.e("UploadResponse", "Error Body: " + errorResponse);
                }

                return responseCode == 201 || responseCode == 200;

            } catch (Exception e) {
                Log.e("UploadError", "Exception: " + e.getMessage(), e);
                return false;
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e) {
                    Log.e("UploadError", "Stream close error", e);
                }
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception e) {
                    Log.e("UploadError", "Output close error", e);
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Toast.makeText(MainActivity.this,
                    success ? "업로드 성공" : "업로드 실패",
                    Toast.LENGTH_SHORT).show();
            if (success) {
                selectedImageUri = null;
                loadPosts();
            }
        }
    }

    private class DeletePostTask extends AsyncTask<Void, Void, Boolean> {
        private final Post targetPost;

        DeletePostTask(Post targetPost) {
            this.targetPost = targetPost;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (targetPost.getId() < 0) {
                return false;
            }

            HttpURLConnection conn = null;
            try {
                URL url = new URL(postUrl + targetPost.getId() + "/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
                conn.connect();

                int responseCode = conn.getResponseCode();
                return responseCode == 204 || responseCode == 200;

            } catch (Exception e) {
                Log.e("DeleteError", "Delete failed", e);
                return false;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Toast.makeText(MainActivity.this,
                    success ? "삭제 성공" : "삭제 실패",
                    Toast.LENGTH_SHORT).show();
            if (success) {
                favoriteKeys.remove(keyForPost(targetPost));
                persistFavorites();
                loadPosts();
            }
        }
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Post>> {
        @Override
        protected List<Post> doInBackground(String... urls) {
            List<Post> posts = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                String apiUrl = urls[0];
                URL urlAPI = new URL(apiUrl);
                conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
                conn.setRequestMethod("GET");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isCancelled()) {
                        reader.close();
                        is.close();
                        return posts;
                    }
                    result.append(line);
                }
                reader.close();
                is.close();

                JSONArray aryJson = new JSONArray(result.toString());
                for (int i = 0; i < aryJson.length(); i++) {
                    if (isCancelled()) {
                        break;
                    }
                    JSONObject postJson = aryJson.getJSONObject(i);

                    String imageUrl = postJson.optString("image", "");
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        continue;
                    }

                    if (!imageUrl.startsWith("http")) {
                        imageUrl = siteUrl + imageUrl;
                    }

                    URL myImageUrl = new URL(imageUrl);
                    HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                    InputStream imgStream = imgConn.getInputStream();
                    Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                    imgStream.close();
                    imgConn.disconnect();

                    int id = postJson.optInt("id", -1);
                    String title = postJson.optString("title", "");
                    String content = postJson.optString("text", postJson.optString("content", ""));
                    String author = resolveAuthor(postJson);
                    boolean ownedByUser = isOwnedByCurrentUser(author, postJson);
                    boolean isFavorite = favoriteKeys.contains(id >= 0 ? "id_" + id : "url_" + imageUrl.hashCode());

                    posts.add(new Post(id, title, content, author, imageUrl, imageBitmap, ownedByUser, isFavorite));
                }

            } catch (Exception e) {
                Log.e("DownloadError", "Failed to load posts", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return posts;
        }

        @Override
        protected void onPostExecute(List<Post> posts) {
            postList.clear();
            if (posts != null) {
                postList.addAll(posts);
            }
            updateStorageInfoFromPosts();
            filterPosts(currentQuery);
        }
    }

    private boolean isOwnedByCurrentUser(String author, JSONObject postJson) {
        if (author != null && !author.isEmpty()) {
            return true;
        }
        return postJson.optBoolean("is_author", false) || postJson.optBoolean("is_owner", false);
    }

    private String resolveAuthor(JSONObject postJson) {
        String author = postJson.optString("author", "");
        if (author == null || author.isEmpty()) {
            author = postJson.optString("username", "");
        }
        if (author == null || author.isEmpty()) {
            author = postJson.optString("writer", "");
        }
        return author;
    }
}
