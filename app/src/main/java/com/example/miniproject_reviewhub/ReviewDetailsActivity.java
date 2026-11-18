package com.example.miniproject_reviewhub;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReviewDetailsActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView authorTextView;
    private TextView ratingTextView;
    private TextView reviewerTextView;
    private TextView dateTextView;
    private TextView reviewTextView;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_details);

        titleTextView = findViewById(R.id.detailTitleTextView);
        authorTextView = findViewById(R.id.detailAuthorTextView);
        ratingTextView = findViewById(R.id.detailRatingTextView);
        reviewerTextView = findViewById(R.id.detailReviewerTextView);
        dateTextView = findViewById(R.id.detailDateTextView);
        reviewTextView = findViewById(R.id.detailReviewTextView);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        // Get the selected review's document ID
        String reviewId = getIntent().getStringExtra("reviewId");

        if (reviewId == null || reviewId.isEmpty()) {
            Toast.makeText(this, "Review not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadReview(reviewId);
    }

    private void loadReview(String reviewId) {

        // Pass unique document UID to Firestore
        db.collection("reviews").document(reviewId).get().addOnSuccessListener(document -> {
            if (!document.exists()) {
                Toast.makeText(this, "Review not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            displayReview(document);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Could not load review", Toast.LENGTH_LONG).show();
            android.util.Log.e("ReviewHub", "Error loading review details", e);
        });
    }

    private void displayReview(DocumentSnapshot document) {
        String title = document.getString("itemTitle");
        String author = document.getString("itemDetail");
        String reviewer = document.getString("reviewerName");
        String reviewText = document.getString("reviewText");

        Long rating = document.getLong("rating");
        Timestamp createdAt = document.getTimestamp("createdAt");

        titleTextView.setText(title != null ? title : "");
        authorTextView.setText(author != null ? author : "");

        if (rating != null) {
            ratingTextView.setText("Rating: " + rating + "/5 stars");
        } else {
            ratingTextView.setText("Rating unavailable");
        }

        reviewerTextView.setText("Reviewed by " + (reviewer != null ? reviewer : "Unknown"));

        if (createdAt != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            dateTextView.setText(dateFormat.format(createdAt.toDate()));
        } else {
            dateTextView.setText("Date unavailable");
        }
        reviewTextView.setText(reviewText != null ? reviewText : "");
    }
}
