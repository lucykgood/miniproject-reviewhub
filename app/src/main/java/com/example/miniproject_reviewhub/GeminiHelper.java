package com.example.miniproject_reviewhub;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiHelper {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/"
                    + "models/gemini-3.8-flash:generateContent";

    public static String analyzeBook(Context context, Uri photoUri) throws Exception {
        // Read and resize the photograph
        Bitmap bitmap;

        try (InputStream input = context.getContentResolver().openInputStream(photoUri)) {
            bitmap = BitmapFactory.decodeStream(input);
        }

        if (bitmap == null) {
            throw new Exception("Could not read image");
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > 1024 || height > 1024) {
            float scale = Math.min(1024f / width, 1024f / height);

            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
        }

        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, imageStream);

        // Coonvert image bytes -> text for JSON request
        String imageBase64 = Base64.encodeToString(imageStream.toByteArray(), Base64.NO_WRAP);

        // Build the Gemini request
        String prompt =
                "Look at this book cover. Identify the book title "
                        + "and author. Do not guess information that is "
                        + "not identifiable. Return only a JSON object "
                        + "with two string fields: title and author. "
                        + "Use empty strings for unknown information.";

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONObject imageData = new JSONObject();
        imageData.put("mime_type", "image/jpeg");
        imageData.put("data", imageBase64);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inline_data", imageData);

        JSONArray parts = new JSONArray();
        parts.put(textPart);
        parts.put(imagePart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contents);

        JSONObject config = new JSONObject();
        config.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", config);

        // Send the HTTP request
        HttpURLConnection connection = null;

        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                String errorMessage = "";

                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        try (java.util.Scanner scanner =
                                     new java.util.Scanner(errorStream, "UTF-8")
                                             .useDelimiter("\\A")) {
                            errorMessage = scanner.hasNext()
                                    ? scanner.next() : "";
                        }
                    }
                }

                throw new Exception(
                        "Gemini request failed: HTTP "
                                + responseCode + " - " + errorMessage
                );
            }

            String responseText;

            try (InputStream input = connection.getInputStream();
                 java.util.Scanner scanner = new java.util.Scanner(input, "UTF-8").useDelimiter("\\A")) {

                responseText = scanner.hasNext() ? scanner.next() : "";
            }

            JSONObject response = new JSONObject(responseText);

            return response.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String polishReview(String reviewText, int rating) throws Exception {

        // Determine how to customize the prompt
        String toneInstruction;

        if (rating == 0) {
            toneInstruction = "Improve grammar and clarity without changing the tone.";
        } else if (rating <= 2) {
            toneInstruction = "Keep the review critical and constructive.";
        } else if (rating == 3) {
            toneInstruction = "Keep the review balanced, acknowledging both positives and negatives.";
        } else {
            toneInstruction = "Keep the review positive and enthusiastic.";
        }

        // Build the JSON request
        String prompt =
                "Polish the following book review. "
                        + toneInstruction
                        + " Preserve the writer's original meaning and opinions. "
                        + "Do not invent facts or new opinions. "
                        + "Return only the revised review text.\n\n"
                        + reviewText;

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray parts = new JSONArray();
        parts.put(textPart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contents);

        HttpURLConnection connection = null;

        // Request the LLM
        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                throw new Exception("Gemini request failed: HTTP " + responseCode);
            }

            String responseText;

            try (InputStream input = connection.getInputStream();
                 java.util.Scanner scanner =
                         new java.util.Scanner(input, "UTF-8")
                                 .useDelimiter("\\A")) {

                responseText = scanner.hasNext()
                        ? scanner.next() : "";
            }

            JSONObject response = new JSONObject(responseText);

            return response.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
