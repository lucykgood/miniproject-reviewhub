package com.example.miniproject_reviewhub;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

public class CreateReviewActivity extends AppCompatActivity {

    private ImageView bookImageView;
    private Button takePhotoButton;
    private Button analyzePhotoButton;
    private Button polishButton;
    private Button saveReviewButton;
    private EditText titleEditText;
    private EditText authorEditText;
    private ProgressBar llmProgressBar;
    private EditText reviewEditText;
    private RatingBar ratingBar;

    private Uri photoUri;
    private Uri previousPhotoUri;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Uri> cameraLauncher;

    // Run network request on background thread
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_review);

        bookImageView = findViewById(R.id.bookImageView);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        analyzePhotoButton = findViewById(R.id.analyzePhotoButton);
        polishButton = findViewById(R.id.polishButton);
        saveReviewButton = findViewById(R.id.saveReviewButton);
        titleEditText = findViewById(R.id.titleEditText);
        authorEditText = findViewById(R.id.authorEditText);
        llmProgressBar = findViewById(R.id.llmProgressBar);
        reviewEditText = findViewById(R.id.reviewEditText);
        ratingBar = findViewById(R.id.ratingBar);



        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && photoUri != null) {
                bookImageView.setImageURI(photoUri);
                analyzePhotoButton.setEnabled(true);

                Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show();

            } else {
                photoUri = previousPhotoUri;

                if (photoUri != null) {
                    bookImageView.setImageURI(photoUri);
                    analyzePhotoButton.setEnabled(true);
                }

                Toast.makeText(this, "Photo capture canceled", Toast.LENGTH_SHORT).show();
            }
        });


        takePhotoButton.setOnClickListener(v -> takePhoto());
        analyzePhotoButton.setOnClickListener(v -> analyzePhoto());
        polishButton.setOnClickListener(v -> polishReview());
        saveReviewButton.setOnClickListener(v -> saveReview());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (savedInstanceState != null) {
            String savedUri = savedInstanceState.getString("photoUri");

            if (savedUri != null) {
                photoUri = Uri.parse(savedUri);
                bookImageView.setImageURI(photoUri);
                analyzePhotoButton.setEnabled(true);
            }
        }
    }

    private void takePhoto() {
        previousPhotoUri = photoUri;

        try {
            File imageDirectory = new File(getCacheDir(), "images");

            if (!imageDirectory.exists()) {
                imageDirectory.mkdirs();
            }

            // Create unique file in cache to save photo
            File photoFile = File.createTempFile("book_", ".jpg", imageDirectory);

            // URI for the file
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            analyzePhotoButton.setEnabled(false);

            // Opens camera + requests that photo be saved @URI
            cameraLauncher.launch(photoUri);

        } catch (IOException | IllegalArgumentException e) {
            photoUri = previousPhotoUri;
            analyzePhotoButton.setEnabled(photoUri != null);

            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
    }


    private void analyzePhoto() {
        if (photoUri == null) {
            Toast.makeText(this, "Please take a photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri imageToAnalyze = photoUri;

        llmProgressBar.setVisibility(View.VISIBLE);
        analyzePhotoButton.setEnabled(false);
        takePhotoButton.setEnabled(false);
        saveReviewButton.setEnabled(false);

        executor.execute(() -> {
            try {
                String result = GeminiHelper.analyzeBook(getApplicationContext(), imageToAnalyze);

                JSONObject bookInfo = new JSONObject(result);

                String title = bookInfo.optString("title", "");
                String author = bookInfo.optString("author", "");

                runOnUiThread(() -> {
                    titleEditText.setText(title);
                    authorEditText.setText(author);

                    llmProgressBar.setVisibility(View.GONE);
                    analyzePhotoButton.setEnabled(true);
                    takePhotoButton.setEnabled(true);
                    saveReviewButton.setEnabled(true);

                    Toast.makeText(this, "Photo analyzed", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                android.util.Log.e("ReviewHub", "Image analysis failed", e);

                runOnUiThread(() -> {
                    llmProgressBar.setVisibility(View.GONE);
                    analyzePhotoButton.setEnabled(true);
                    takePhotoButton.setEnabled(true);
                    saveReviewButton.setEnabled(true);

                    Toast.makeText(this, "Could not analyze photo. Enter details manually.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void polishReview() {

        String originalText = reviewEditText.getText().toString().trim();

        if (originalText.isEmpty()) {
            Toast.makeText(this, "Please write a review first", Toast.LENGTH_SHORT).show();
            return;
        }

        int rating = (int) ratingBar.getRating();

        llmProgressBar.setVisibility(View.VISIBLE);
        polishButton.setEnabled(false);
        analyzePhotoButton.setEnabled(false);
        takePhotoButton.setEnabled(false);
        saveReviewButton.setEnabled(false);

        executor.execute(() -> {
            try {
                String polishedText = GeminiHelper.polishReview(originalText, rating);

                runOnUiThread(() -> {
                    String currentText = reviewEditText.getText().toString().trim();
                    if (currentText.equals(originalText)) {
                        reviewEditText.setText(polishedText);
                    } else {
                        Toast.makeText(this, "Review changed during processing. AI result not applied.", Toast.LENGTH_LONG).show();
                    }

                    llmProgressBar.setVisibility(View.GONE);
                    polishButton.setEnabled(true);
                    analyzePhotoButton.setEnabled(photoUri != null);
                    takePhotoButton.setEnabled(true);
                    saveReviewButton.setEnabled(true);

                    Toast.makeText(this,
                            "Review polished",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                android.util.Log.e("ReviewHub", "Review polishing failed", e
                );

                runOnUiThread(() -> {
                    llmProgressBar.setVisibility(View.GONE);
                    polishButton.setEnabled(true);
                    analyzePhotoButton.setEnabled(photoUri != null);
                    takePhotoButton.setEnabled(true);
                    saveReviewButton.setEnabled(true);

                    Toast.makeText(this, "Could not polish review. Your text was preserved.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    private void saveReview() {

        // Read the form fields
        String title = titleEditText.getText().toString().trim();
        String author = authorEditText.getText().toString().trim();
        String reviewText = reviewEditText.getText().toString().trim();
        int rating = (int) ratingBar.getRating();

        // Validate required fields
        if (title.isEmpty()) {
            titleEditText.setError("Book title is required");
            titleEditText.requestFocus();
            return;
        }

        if (author.isEmpty()) {
            authorEditText.setError("Author is required");
            authorEditText.requestFocus();
            return;
        }

        if (reviewText.isEmpty()) {
            reviewEditText.setError("Review text is required");
            reviewEditText.requestFocus();
            return;
        }

        if (rating < 1 || rating > 5) {
            Toast.makeText(this, "Please select a rating from 1 to 5", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check authentication
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "You must be logged in to save a review", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prevent duplicate submissions
        saveReviewButton.setEnabled(false);

        // Look up the user's display name
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        saveReviewButton.setEnabled(true);

                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String reviewerName = document.getString("name");

                    if (reviewerName == null || reviewerName.trim().isEmpty()) {
                        saveReviewButton.setEnabled(true);

                        Toast.makeText(this, "User profile name is missing", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create the Firestore document
                    Map<String, Object> review = new HashMap<>();

                    review.put("userId", user.getUid());
                    review.put("reviewerName", reviewerName);
                    review.put("itemTitle", title);
                    review.put("itemDetail", author);
                    review.put("reviewText", reviewText);
                    review.put("rating", rating);
                    review.put("createdAt", FieldValue.serverTimestamp());

                    // Save the new review
                    db.collection("reviews")
                            .add(review)
                            .addOnSuccessListener(documentReference -> {

                                Toast.makeText(this, "Review saved successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                saveReviewButton.setEnabled(true);

                                Toast.makeText(this, "Could not save review", Toast.LENGTH_LONG).show();
                                android.util.Log.e("ReviewHub", "Error saving review", e);
                            });

                })
                .addOnFailureListener(e -> {
                    saveReviewButton.setEnabled(true);

                    Toast.makeText(this, "Could not load user profile", Toast.LENGTH_LONG).show();
                    android.util.Log.e("ReviewHub", "Error loading profile for review", e);
                });
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (photoUri != null) {
            outState.putString("photoUri", photoUri.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

