package com.example.miniproject_reviewhub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends ArrayAdapter<Review> {

    public ReviewAdapter(Context context, List<Review> reviews) {
        super(context, 0, reviews);
    }

    // Display a reveiw row
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_review, parent, false);
        }

        Review review = getItem(position);

        TextView titleTextView = convertView.findViewById(R.id.reviewTitleTextView);
        TextView nameTextView = convertView.findViewById(R.id.reviewerNameTextView);
        TextView ratingTextView = convertView.findViewById(R.id.reviewRatingTextView);
        TextView dateTextView = convertView.findViewById(R.id.reviewDateTextView);

        if (review != null) {
            titleTextView.setText(review.getItemTitle());
            nameTextView.setText("By " + review.getReviewerName());
            ratingTextView.setText("Rating: " + review.getRating() + "/5 stars");

            if (review.getCreatedAt() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String date = dateFormat.format(review.getCreatedAt().toDate());
                dateTextView.setText(date);
            } else {
                dateTextView.setText("Date unavailable");
            }
        }
        return convertView;
    }
}