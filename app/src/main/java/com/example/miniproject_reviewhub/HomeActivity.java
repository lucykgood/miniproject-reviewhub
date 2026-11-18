package com.example.miniproject_reviewhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private TextView nameTextView;
    private TextView emailTextView;
    private TextView bioTextView;
    private ImageView profileImageView;
    private ProgressBar profileProgressBar;
    private Button addReviewButton;
    private Button logoutButton;
    private ListView reviewsListView;
    private CheckBox myReviewsCheckBox;
    private ProgressBar reviewsProgressBar;
    private TextView emptyReviewsTextView;

    private ArrayList<Review> allReviews;
    private ArrayList<Review> displayedReviews;
    private ReviewAdapter reviewAdapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameTextView = findViewById(R.id.nameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        bioTextView = findViewById(R.id.bioTextView);
        profileImageView = findViewById(R.id.profileImageView);
        profileProgressBar = findViewById(R.id.profileProgressBar);
        addReviewButton = findViewById(R.id.addReviewButton);
        logoutButton = findViewById(R.id.logoutButton);
        reviewsListView = findViewById(R.id.reviewsListView);
        myReviewsCheckBox = findViewById(R.id.myReviewsCheckBox);
        reviewsProgressBar = findViewById(R.id.reviewsProgressBar);
        emptyReviewsTextView = findViewById(R.id.emptyReviewsTextView);

        allReviews = new ArrayList<>();
        displayedReviews = new ArrayList<>();

        reviewAdapter = new ReviewAdapter(this, displayedReviews);
        reviewsListView.setAdapter(reviewAdapter);

        // Make review clickable
        reviewsListView.setOnItemClickListener((parent, view, position, id) -> {
            Review selectedReview = displayedReviews.get(position);

            Intent intent = new Intent(HomeActivity.this, ReviewDetailsActivity.class);
            intent.putExtra("reviewId", selectedReview.getId());
            startActivity(intent);
        });


        myReviewsCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> filterReviews()
        );

        logoutButton.setOnClickListener(v -> logoutUser());
        addReviewButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CreateReviewActivity.class);
            startActivity(intent);
        });

        loadUserProfile();
        loadReviews();
    }

    private void loadReviews() {
        reviewsProgressBar.setVisibility(View.VISIBLE);
        emptyReviewsTextView.setVisibility(View.GONE);

        // Sorts and retrieves needed documents
        db.collection("reviews").orderBy("createdAt", Query.Direction.DESCENDING).get().addOnSuccessListener(querySnapshot -> {
            reviewsProgressBar.setVisibility(View.GONE);
            allReviews.clear();

            for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot.getDocuments()) {
                Review review = document.toObject(Review.class);

                if (review != null) {
                    review.setId(document.getId());
                    allReviews.add(review);
                }
            }
            filterReviews();
        }).addOnFailureListener(e -> {
            reviewsProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
        });
    }

    private void filterReviews() {
        displayedReviews.clear();
        boolean myReviewsOnly = myReviewsCheckBox.isChecked();
        String currentUserId = auth.getCurrentUser().getUid();

        for (Review review : allReviews) {
            if (!myReviewsOnly || review.getUserId().equals(currentUserId)) {
                displayedReviews.add(review);
            }
        }

        reviewAdapter.notifyDataSetChanged();

        if (displayedReviews.isEmpty()) {
            emptyReviewsTextView.setVisibility(View.VISIBLE);
        } else {
            emptyReviewsTextView.setVisibility(View.GONE);
        }
    }

    // Refresh reviews
    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && auth.getCurrentUser() != null) {
            loadReviews();
        }
    }

    private void loadUserProfile() {
        // Get authenticated user
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            returnToLogin();
            return;
        }

        // Fetch correct profile
        String uid = user.getUid();

        profileProgressBar.setVisibility(View.VISIBLE);

        // Async requests for the user profile
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
                    profileProgressBar.setVisibility(View.GONE);

                    if (document.exists()) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        String bio = document.getString("bio");
                        String photoName = document.getString("photoName");

                        nameTextView.setText(name);
                        emailTextView.setText(email);
                        bioTextView.setText(bio);

                        if (photoName != null) {
                            int imageId = getResources().getIdentifier(photoName, "drawable", getPackageName());

                            if (imageId != 0) {
                                profileImageView.setImageResource(imageId);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    profileProgressBar.setVisibility(View.GONE);

                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    // Logout
    private void logoutUser() {
        auth.signOut();
        returnToLogin();
    }

    private void returnToLogin() {
        // Return to Login screen
        Intent intent = new Intent(this, MainActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}