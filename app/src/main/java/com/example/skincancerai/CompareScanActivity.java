package com.example.skincancerai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CompareScanActivity extends AppCompatActivity {

    private static final int RISK_LOW = 0;
    private static final int RISK_MEDIUM = 1;
    private static final int RISK_HIGH = 2;

    private ImageView imgOld, imgNew;
    private TextView txtOldResult, txtOldConfidence, txtOldDate;
    private TextView txtNewResult, txtNewConfidence, txtNewDate;
    private TextView txtProfileCompare;
    private TextView txtCompareTrend;
    private TextView txtCompareDelta;
    private CardView cardOld, cardNew;

    private String oldImageBase64;
    private String newImageBase64;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_scan);

        Toolbar toolbar = findViewById(R.id.toolbarCompare);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("So sánh lần quét");
        }

        imgOld = findViewById(R.id.imgOld);
        imgNew = findViewById(R.id.imgNew);

        txtOldResult = findViewById(R.id.txtOldResult);
        txtOldConfidence = findViewById(R.id.txtOldConfidence);
        txtOldDate = findViewById(R.id.txtOldDate);

        txtNewResult = findViewById(R.id.txtNewResult);
        txtNewConfidence = findViewById(R.id.txtNewConfidence);
        txtNewDate = findViewById(R.id.txtNewDate);

        txtProfileCompare = findViewById(R.id.txtProfileCompare);
        txtCompareTrend = findViewById(R.id.txtCompareTrend);
        txtCompareDelta = findViewById(R.id.txtCompareDelta);

        cardOld = findViewById(R.id.cardOldContainer);
        cardNew = findViewById(R.id.cardNewContainer);

        bindData();
        setupImageZoom();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void bindData() {
        String profileName = getIntent().getStringExtra("profileName");

        String oldLabelRaw = getIntent().getStringExtra("oldResultLabel");
        float oldConfidence = getIntent().getFloatExtra("oldConfidence", 0f);
        long oldCreatedAt = getIntent().getLongExtra("oldCreatedAt", 0L);
        oldImageBase64 = getIntent().getStringExtra("oldImageBase64");

        String newLabelRaw = getIntent().getStringExtra("newResultLabel");
        float newConfidence = getIntent().getFloatExtra("newConfidence", 0f);
        long newCreatedAt = getIntent().getLongExtra("newCreatedAt", 0L);
        newImageBase64 = getIntent().getStringExtra("newImageBase64");

        String oldLabel = normalizeRiskLabel(oldLabelRaw);
        String newLabel = normalizeRiskLabel(newLabelRaw);

        txtProfileCompare.setText(
                profileName != null && !profileName.trim().isEmpty()
                        ? "Hồ sơ: " + profileName
                        : "So sánh hai lần quét cùng hồ sơ"
        );

        txtOldResult.setText(oldLabel);
        txtOldConfidence.setText("Độ tin cậy: " + String.format(Locale.getDefault(), "%.2f%%", oldConfidence * 100f));
        txtOldDate.setText(dateFormat.format(new Date(oldCreatedAt > 0 ? oldCreatedAt : System.currentTimeMillis())));

        txtNewResult.setText(newLabel);
        txtNewConfidence.setText("Độ tin cậy: " + String.format(Locale.getDefault(), "%.2f%%", newConfidence * 100f));
        txtNewDate.setText(dateFormat.format(new Date(newCreatedAt > 0 ? newCreatedAt : System.currentTimeMillis())));

        setImage(imgOld, oldImageBase64);
        setImage(imgNew, newImageBase64);

        applyResultStyle(txtOldResult, cardOld, oldLabel);
        applyResultStyle(txtNewResult, cardNew, newLabel);
        applyConfidenceStyle(txtOldConfidence, oldConfidence);
        applyConfidenceStyle(txtNewConfidence, newConfidence);

        updateTrendSummary(oldLabel, newLabel, oldConfidence, newConfidence);
    }

    private void updateTrendSummary(String oldLabel, String newLabel, float oldConfidence, float newConfidence) {
        int oldRisk = riskLevel(oldLabel);
        int newRisk = riskLevel(newLabel);

        if (newRisk > oldRisk) {
            txtCompareTrend.setText("Xu hướng tăng mức nguy cơ");
            txtCompareTrend.setTextColor(Color.parseColor("#B45309"));
        } else if (newRisk < oldRisk) {
            txtCompareTrend.setText("Xu hướng cải thiện");
            txtCompareTrend.setTextColor(Color.parseColor("#166534"));
        } else if (newRisk == RISK_HIGH) {
            txtCompareTrend.setText("Nguy cơ vẫn ở mức cao");
            txtCompareTrend.setTextColor(Color.parseColor("#B45309"));
        } else if (newRisk == RISK_MEDIUM) {
            txtCompareTrend.setText("Cần tiếp tục theo dõi sát");
            txtCompareTrend.setTextColor(Color.parseColor("#D97706"));
        } else {
            txtCompareTrend.setText("Xu hướng ổn định");
            txtCompareTrend.setTextColor(Color.parseColor("#1D4ED8"));
        }

        float delta = (newConfidence - oldConfidence) * 100f;
        String sign = delta >= 0f ? "+" : "";
        txtCompareDelta.setText("Chênh lệch độ tin cậy: "
                + sign + String.format(Locale.getDefault(), "%.2f", delta)
                + "%");
    }

    private void setupImageZoom() {
        imgOld.setOnClickListener(v -> showZoomImageDialog(oldImageBase64, "Ảnh lần trước"));
        imgNew.setOnClickListener(v -> showZoomImageDialog(newImageBase64, "Ảnh lần mới"));
    }

    private void showZoomImageDialog(String imageBase64, String title) {
        Bitmap bmp = decodeBase64ToBitmap(imageBase64);
        if (bmp == null) {
            return;
        }

        ImageView zoomImage = new ImageView(this);
        zoomImage.setImageBitmap(bmp);
        zoomImage.setAdjustViewBounds(true);
        zoomImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        zoomImage.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(zoomImage)
                .setPositiveButton("Đóng", null)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    private Bitmap decodeBase64ToBitmap(String imageBase64) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return null;
        }

        try {
            byte[] bytes = Base64.decode(imageBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void setImage(ImageView imageView, String imageBase64) {
        Bitmap bmp = decodeBase64ToBitmap(imageBase64);
        if (bmp != null) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(bmp);
        } else {
            imageView.setImageResource(R.drawable.ic_camera);
        }
    }

    private void applyResultStyle(TextView resultView, CardView container, String label) {
        int risk = riskLevel(label);

        if (risk == RISK_HIGH) {
            resultView.setTextColor(Color.parseColor("#DC2626"));
            container.setCardBackgroundColor(Color.parseColor("#FFF7F7"));
        } else if (risk == RISK_MEDIUM) {
            resultView.setTextColor(Color.parseColor("#D97706"));
            container.setCardBackgroundColor(Color.parseColor("#FFF9F0"));
        } else {
            resultView.setTextColor(Color.parseColor("#2563EB"));
            container.setCardBackgroundColor(Color.parseColor("#F4F9FF"));
        }
    }

    private void applyConfidenceStyle(TextView confidenceView, float confidence) {
        if (confidence >= 0.8f) {
            confidenceView.setTextColor(Color.parseColor("#16A34A"));
        } else if (confidence >= 0.6f) {
            confidenceView.setTextColor(Color.parseColor("#D97706"));
        } else {
            confidenceView.setTextColor(Color.parseColor("#475569"));
        }
    }

    private String normalizeRiskLabel(String label) {
        if (label == null) return "Không rõ";
        String clean = TextSanitizer.sanitize(label);
        String lower = clean.toLowerCase(Locale.getDefault());

        if (lower.contains("nguy cơ cao") || lower.contains("nguy co cao")
                || lower.contains("ác tính") || lower.contains("ac tinh")
                || lower.contains("malignant") || lower.contains("suspicious")) {
            return "Nguy cơ cao";
        }

        if (lower.contains("nguy cơ trung bình") || lower.contains("nguy co trung binh")
                || lower.contains("medium")) {
            return "Nguy cơ trung bình";
        }

        if (lower.contains("nguy cơ thấp") || lower.contains("nguy co thap")
                || lower.contains("lành tính") || lower.contains("lanh tinh")
                || lower.contains("benign") || lower.contains("normal")) {
            return "Nguy cơ thấp";
        }

        return clean;
    }

    private int riskLevel(String label) {
        String lower = normalizeRiskLabel(label).toLowerCase(Locale.getDefault());
        if (lower.contains("nguy cơ cao")) return RISK_HIGH;
        if (lower.contains("nguy cơ trung bình")) return RISK_MEDIUM;
        return RISK_LOW;
    }
}
