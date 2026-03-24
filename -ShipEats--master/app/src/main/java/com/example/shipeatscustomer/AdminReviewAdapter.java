package com.example.shipeatscustomer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;

    public AdminReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_admin, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        holder.tvCustomerName.setText(review.getCustomerName());
        holder.tvComment.setText(review.getComment());
        holder.ratingBar.setRating(review.getRating());

        // Convert timestamp (long) to a readable date string
        if (review.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String dateString = sdf.format(new Date(review.getTimestamp()));
            holder.tvDate.setText(dateString);
        } else {
            holder.tvDate.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvComment, tvDate;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tv_admin_customer_name);
            tvComment = itemView.findViewById(R.id.tv_admin_review_comment);
            tvDate = itemView.findViewById(R.id.tv_admin_review_date);
            ratingBar = itemView.findViewById(R.id.admin_review_rating_bar);
        }
    }
}