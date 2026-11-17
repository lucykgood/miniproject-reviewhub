package com.example.miniproject_reviewhub;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Welcome to ReviewHub!");
        textView.setTextSize(24);
        textView.setPadding(40, 100, 40, 40);

        setContentView(textView);
    }
}
