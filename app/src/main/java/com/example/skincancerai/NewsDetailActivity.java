package com.example.skincancerai;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class NewsDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_DESC = "desc";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_IMAGE_RES = "image_res";

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

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String desc = getIntent().getStringExtra(EXTRA_DESC);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);
        String date = getIntent().getStringExtra(EXTRA_DATE);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        int imageRes = getIntent().getIntExtra(EXTRA_IMAGE_RES, R.drawable.onboard_health_1);

        txtNewsDetailTitle.setText(title == null ? "" : title);
        txtNewsDetailDesc.setText(desc == null ? "" : desc);
        txtNewsDetailContent.setText(content == null ? "" : content);
        txtNewsDetailDate.setText(date == null ? "" : date);
        txtNewsDetailCategory.setText(category == null ? "" : category);
        imgNewsDetail.setImageResource(imageRes);
    }
}
