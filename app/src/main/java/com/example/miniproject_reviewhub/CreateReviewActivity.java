package com.example.miniproject_reviewhub;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class CreateReviewActivity extends AppCompatActivity {

    private ImageView bookImageView;
    private Button takePhotoButton;
    private Button analyzePhotoButton;

    private Uri photoUri;
    private Uri previousPhotoUri;

    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_review);

        bookImageView = findViewById(R.id.bookImageView);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        analyzePhotoButton = findViewById(R.id.analyzePhotoButton);


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
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (photoUri != null) {
            outState.putString("photoUri", photoUri.toString());
        }
    }
}

