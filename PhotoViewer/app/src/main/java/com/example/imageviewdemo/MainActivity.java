package com.example.imageviewdemo;
import android.util.Log;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Button;

import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import java.io.InputStream;
import java.io.DataOutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    TextView textView;
    String site_url = "https://yunhyungnam.pythonanywhere.com";
    String post_url = site_url + "/api_root/Post/";
    CloadImage taskDownload;

    // ✅ 추가
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    EditText editTitle, editText;
    String inputTitle, inputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ 초기화
        textView = findViewById(R.id.textView);


    }

    // ✅ 갤러리 열기 버튼
    public void onClickUpload(View v) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload, null);
        EditText dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        EditText dialogText = dialogView.findViewById(R.id.dialogText);
        Button selectImage = dialogView.findViewById(R.id.dialogSelectImage);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("새 게시글 작성")
                .setView(dialogView)
                .setPositiveButton("업로드", null)  // 나중에 수동으로 설정
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .create();

        dialog.show();

        // ✅ 사진 선택 버튼
        selectImage.setOnClickListener(v2 -> {
            inputTitle = dialogTitle.getText().toString().trim();
            inputText = dialogText.getText().toString().trim();

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "사진 선택"), PICK_IMAGE_REQUEST);
        });

        // ✅ 업로드 버튼 클릭 시
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
            inputTitle = dialogTitle.getText().toString().trim();
            inputText = dialogText.getText().toString().trim();

            if (selectedImageUri == null) {
                Toast.makeText(this, "사진을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputTitle.isEmpty() || inputText.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            new UploadTask().execute();
            dialog.dismiss();
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                Toast.makeText(this, "사진이 선택되었습니다.", Toast.LENGTH_SHORT).show();
                // ✅ 이제 업로드는 안 함 — 사용자가 '업로드' 버튼 눌러야 실행됨
            }
        }
    }


    // ✅ 이미지 업로드
    private class UploadTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String token = "87af38f0adbac939bfd69b8063bb674cc9b85b5c";
                String uploadUrl = post_url;
                String boundary = "----AndroidBoundary";
                String LINE_FEED = "\r\n";

                HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setDoOutput(true);

                DataOutputStream out = new DataOutputStream(conn.getOutputStream());

                // ✅ 제목(title)
                out.writeBytes("--" + boundary + LINE_FEED);
                out.writeBytes("Content-Disposition: form-data; name=\"title\"" + LINE_FEED);
                out.writeBytes(LINE_FEED);
                out.writeBytes(inputTitle + LINE_FEED);

                // ✅ 내용(text)
                out.writeBytes("--" + boundary + LINE_FEED);
                out.writeBytes("Content-Disposition: form-data; name=\"text\"" + LINE_FEED);
                out.writeBytes(LINE_FEED);
                out.writeBytes(inputText + LINE_FEED);

                // ✅ 이미지(image)
                out.writeBytes("--" + boundary + LINE_FEED);
                out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"" + LINE_FEED);
                out.writeBytes("Content-Type: image/jpeg" + LINE_FEED);
                out.writeBytes(LINE_FEED);

                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                inputStream.close();

                out.writeBytes(LINE_FEED + "--" + boundary + "--" + LINE_FEED);
                out.flush();
                out.close();

                // ✅ 응답 코드 출력
                int responseCode = conn.getResponseCode();
                String responseMsg = conn.getResponseMessage();
                Log.e("UploadResponse", "Response Code: " + responseCode + " / Message: " + responseMsg);

                // ✅ 에러 메시지 본문 읽기 (400, 403, 500 등)
                if (responseCode != 201) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    reader.close();
                    Log.e("UploadResponse", "Error Body: " + errorResponse);
                }

                return responseCode == 201;

            } catch (Exception e) {
                Log.e("UploadError", "Exception: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Toast.makeText(MainActivity.this,
                    success ? "업로드 성공" : "업로드 실패",
                    Toast.LENGTH_SHORT).show();
        }
    }


    // ✅ 기존 다운로드 기능
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(post_url);
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "87af38f0adbac939bfd69b8063bb674cc9b85b5c";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                is.close();

                JSONArray aryJson = new JSONArray(result.toString());
                for (int i = 0; i < aryJson.length(); i++) {
                    JSONObject post_json = aryJson.getJSONObject(i);
                    String imageUrl = post_json.getString("image");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        if (!imageUrl.startsWith("http")) {
                            imageUrl = site_url + imageUrl;
                        }
                        URL myImageUrl = new URL(imageUrl);
                        HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                        InputStream imgStream = imgConn.getInputStream();
                        Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                        bitmapList.add(imageBitmap);
                        imgStream.close();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(images);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }
}
