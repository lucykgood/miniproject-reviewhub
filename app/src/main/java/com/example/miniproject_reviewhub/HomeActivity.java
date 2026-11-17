package com.example.miniproject_reviewhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private TextView nameTextView;
    private TextView emailTextView;
    private TextView bioTextView;
    private ImageView profileImageView;
    private ProgressBar profileProgressBar;
    private Button logoutButton;

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
        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> logoutUser());

        loadUserProfile();
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