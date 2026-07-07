package com.example.skincancerai;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

public class NewsWebDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        Toolbar toolbar = findViewById(R.id.toolbarNewsDetail);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> PageTransitionHelper.finishWithAnimation(this));

        ImageView imgNewsDetail = findViewById(R.id.imgNewsDetail);
        TextView txtNewsDetailCategory = findViewById(R.id.txtNewsDetailCategory);
        TextView txtNewsDetailTitle = findViewById(R.id.txtNewsDetailTitle);
        TextView txtNewsDetailDate = findViewById(R.id.txtNewsDetailDate);
        TextView txtNewsDetailDesc = findViewById(R.id.txtNewsDetailDesc);
        TextView txtNewsDetailContent = findViewById(R.id.txtNewsDetailContent);

        String title = getIntent().getStringExtra("title");
        String summary = getIntent().getStringExtra("summary");
        String content = getIntent().getStringExtra("content");
        String date = getIntent().getStringExtra("date");
        String category = getIntent().getStringExtra("category");
        String imageUrl = getIntent().getStringExtra("imageUrl");
        int imageRes = getIntent().getIntExtra("imageRes", 0);

        txtNewsDetailTitle.setText(title == null ? "" : title);
        txtNewsDetailDesc.setText(summary == null ? "" : summary);
        txtNewsDetailContent.setText(content == null ? "" : content);
        txtNewsDetailDate.setText(date == null ? "" : date);
        txtNewsDetailCategory.setText(category == null ? "" : category);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.onboard_health_2)
                    .error(R.drawable.onboard_health_2)
                    .centerCrop()
                    .into(imgNewsDetail);
        } else {
            imgNewsDetail.setImageResource(imageRes != 0 ? imageRes : R.drawable.onboard_health_2);
        }
    }
}
