package com.example.skincancerai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class ImageCropActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_CROPPED_JPEG = "cropped_jpeg";

    private ImageView imageView;
    private CameraOverlayView overlayView;
    private Bitmap bitmap;

    private final Matrix matrix = new Matrix();
    private final Matrix inverse = new Matrix();
    private ScaleGestureDetector scaleDetector;
    private float lastX, lastY;
    private boolean dragging;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        imageView = findViewById(R.id.imageView);
        overlayView = findViewById(R.id.overlayView);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnUse = findViewById(R.id.btnUse);

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                imageView.setImageMatrix(matrix);
                return true;
            }
        });

        Uri uri = null;
        String uriStr = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriStr != null) uri = Uri.parse(uriStr);

        bitmap = loadBitmap(uri, 2048);
        if (bitmap == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        imageView.setImageBitmap(bitmap);
        imageView.setImageMatrix(matrix);
        imageView.post(this::fitCenter);

        imageView.setOnTouchListener((v, e) -> {
            scaleDetector.onTouchEvent(e);
            if (scaleDetector.isInProgress()) return true;

            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = e.getX();
                    lastY = e.getY();
                    dragging = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!dragging) return true;
                    matrix.postTranslate(e.getX() - lastX, e.getY() - lastY);
                    imageView.setImageMatrix(matrix);
                    lastX = e.getX();
                    lastY = e.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    return true;
            }
            return true;
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnUse.setOnClickListener(v -> {
            Bitmap cropped = cropToOverlay();
            if (cropped == null) {
                Utils.toast(this, "Không lấy được vùng crop. Hãy zoom/kéo ảnh rồi thử lại.");
                return;
            }

            Bitmap scaled = scaleMax(cropped, 1024);
            byte[] jpeg = Utils.bitmapToJpegBytes(scaled, 92);

            Intent data = new Intent();
            data.putExtra(EXTRA_CROPPED_JPEG, jpeg);
            setResult(RESULT_OK, data);

            if (scaled != cropped) scaled.recycle();
            cropped.recycle();
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
    }

    private void fitCenter() {
        if (bitmap == null) return;
        float vw = imageView.getWidth();
        float vh = imageView.getHeight();
        if (vw <= 0 || vh <= 0) return;

        float bw = bitmap.getWidth();
        float bh = bitmap.getHeight();
        float s = Math.min(vw / bw, vh / bh);
        float dx = (vw - bw * s) / 2f;
        float dy = (vh - bh * s) / 2f;

        matrix.reset();
        matrix.postScale(s, s);
        matrix.postTranslate(dx, dy);
        imageView.setImageMatrix(matrix);
    }

    private Bitmap cropToOverlay() {
        RectF overlay = overlayView.getFocusRect();
        if (overlay == null || overlay.width() <= 0 || overlay.height() <= 0) return null;
        if (bitmap == null) return null;
        if (!matrix.invert(inverse)) return null;

        RectF r = new RectF(overlay);
        inverse.mapRect(r);

        int left = clamp((int) Math.floor(r.left), 0, bitmap.getWidth() - 1);
        int top = clamp((int) Math.floor(r.top), 0, bitmap.getHeight() - 1);
        int right = clamp((int) Math.ceil(r.right), left + 1, bitmap.getWidth());
        int bottom = clamp((int) Math.ceil(r.bottom), top + 1, bitmap.getHeight());

        int w = right - left;
        int h = bottom - top;
        int size = Math.min(w, h);
        return Bitmap.createBitmap(bitmap, left, top, size, size);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Bitmap scaleMax(Bitmap src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        int max = Math.max(w, h);
        if (max <= maxSide) return src;
        float s = (float) maxSide / (float) max;
        return Bitmap.createScaledBitmap(src, Math.round(w * s), Math.round(h * s), true);
    }

    private Bitmap loadBitmap(Uri uri, int maxSide) {
        if (uri == null) return null;
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            byte[] bytes = Utils.readAllBytes(is);
            Bitmap raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (raw == null) return null;
            Bitmap scaled = scaleMax(raw, maxSide);
            if (scaled != raw) raw.recycle();
            return scaled;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

