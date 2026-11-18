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
    private static final String INTERACTIONS_URL =
            "https://generativelanguage.googleapis.com/v1beta/interactions";


    public static String analyzeBook(Context context, Uri photoUri) throws Exception {

        // Read the photograph
        Bitmap bitmap;

        try (InputStream input = context.getContentResolver().openInputStream(photoUri)) {
            bitmap = BitmapFactory.decodeStream(input);
        }

        if (bitmap == null) {
            throw new Exception("Could not read image");
        }

        // Resize image
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > 1024 || height > 1024) {
            float scale = Math.min(1024f / width, 1024f / height);

            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
        }

        // Convert image to Base64
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, imageStream);
        String imageBase64 = Base64.encodeToString(imageStream.toByteArray(), Base64.NO_WRAP);

        // Create the prompt
        String prompt =
                "Look at this book cover. Identify the book title "
                        + "and author. Do not guess information that is "
                        + "not identifiable. Return only a JSON object "
                        + "with two string fields: title and author. "
                        + "Use empty strings for unknown information.";

        // Build the API JSON input
        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image");
        imagePart.put("mime_type", "image/jpeg");
        imagePart.put("data", imageBase64);

        JSONArray inputParts = new JSONArray();
        inputParts.put(textPart);
        inputParts.put(imagePart);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gemini-3.5-flash-lite");
        requestBody.put("input", inputParts);

        // Send the request
        HttpURLConnection connection = null;

        try {
            URL url = new URL(INTERACTIONS_URL);
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

            // Read  response
            int responseCode = connection.getResponseCode();
            InputStream responseStream = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();

            if (responseStream == null) {
                throw new Exception("Gemini returned HTTP " + responseCode + " without a response body");
            }

            StringBuilder responseText = new StringBuilder();

            try (java.io.BufferedReader reader =
                         new java.io.BufferedReader(
                                 new java.io.InputStreamReader(
                                         responseStream,
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }
            }

            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception("Gemini request failed: HTTP " + responseCode + " - " + responseText);
            }

            // Extract model's response
            JSONObject response = new JSONObject(responseText.toString());
            JSONArray steps = response.getJSONArray("steps");
            StringBuilder answer = new StringBuilder();

            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.getJSONObject(i);

                if ("model_output".equals(step.optString("type"))) {
                    JSONArray content = step.getJSONArray("content");

                    for (int j = 0; j < content.length(); j++) {
                        JSONObject part = content.getJSONObject(j);

                        if ("text".equals(part.optString("type"))) {
                            answer.append(part.getString("text"));
                        }
                    }
                }
            }

            if (answer.length() == 0) {
                throw new Exception("Gemini returned no book information");
            }

            // Validate that Gemini returned JSON
            String result = answer.toString().trim();

            // Remove Markdown fences if Gemini includes them
            if (result.startsWith("```")) {
                result = result.replaceFirst("^```(?:json)?\\s*", "");
                result = result.replaceFirst("\\s*```$", "");
            }

            JSONObject bookInfo = new JSONObject(result);

            // Ensure both expected fields exist
            JSONObject cleanResult = new JSONObject();
            cleanResult.put("title", bookInfo.optString("title", ""));
            cleanResult.put("author", bookInfo.optString("author", ""));

            return cleanResult.toString();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    public static String polishReview(String reviewText, int rating) throws Exception {

        // Choose prompt tone based on  rating
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

        // Create prompt JSON
        String prompt =
                "Polish the following book review. "
                        + toneInstruction
                        + " Preserve the writer's original meaning and opinions. "
                        + "Do not invent facts or new opinions. "
                        + "Return only the revised review text.\n\n"
                        + reviewText;

        // Build API request
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gemini-3.5-flash-lite");
        requestBody.put("input", prompt);

        HttpURLConnection connection = null;

        try {
            // Open connection
            URL url = new URL(INTERACTIONS_URL);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);

            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);

            // Send request
            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            // Read response + error details
            int responseCode = connection.getResponseCode();

            InputStream responseStream = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();

            if (responseStream == null) {
                throw new Exception("Gemini returned HTTP " + responseCode + " without a response body");
            }

            StringBuilder responseText = new StringBuilder();

            try (java.io.BufferedReader reader =
                         new java.io.BufferedReader(
                                 new java.io.InputStreamReader(
                                         responseStream,
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }
            }

            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception("Gemini request failed: HTTP " + responseCode + " - " + responseText);
            }

            // Parse response
            JSONObject response = new JSONObject(responseText.toString());
            JSONArray steps = response.getJSONArray("steps");

            StringBuilder answer = new StringBuilder();

            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.getJSONObject(i);

                if ("model_output".equals(step.optString("type"))) {
                    JSONArray content = step.getJSONArray("content");

                    for (int j = 0; j < content.length(); j++) {
                        JSONObject part = content.getJSONObject(j);

                        if ("text".equals(part.optString("type"))) {
                            answer.append(part.getString("text"));
                        }
                    }
                }
            }

            if (answer.length() == 0) {
                throw new Exception("Gemini returned no usable review text");
            }

            return answer.toString().trim();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
