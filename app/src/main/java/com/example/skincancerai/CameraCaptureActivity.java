package com.example.skincancerai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class CameraCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_CAPTURED_IMAGE_PATH = "captured_image_path";

    private static final String TAG = "CameraCaptureActivity";
    private static final int REQ_CAMERA_PERMISSION = 5001;

    private PreviewView previewView;
    private ImageView btnBack;
    private ImageButton btnCapture;
    private MaterialButton btnFlash;
    private SeekBar seekZoom;
    private TextView txtZoomLabel;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;

    private boolean torchEnabled = false;
    private float minZoomRatio = 1f;
    private float maxZoomRatio = 1f;
    private float currentZoomRatio = 1f;

    private ScaleGestureDetector scaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture);

        previewView = findViewById(R.id.previewView);
        btnBack = findViewById(R.id.btnBack);
        btnCapture = findViewById(R.id.btnCapture);
        btnFlash = findViewById(R.id.btnFlash);
        seekZoom = findViewById(R.id.seekZoom);
        txtZoomLabel = findViewById(R.id.txtZoomLabel);

        btnBack.setOnClickListener(v -> finish());
        btnCapture.setOnClickListener(v -> captureImage());
        btnFlash.setOnClickListener(v -> toggleTorch());

        setupZoomControls();

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (camera == null) return false;

                float scaleFactor = detector.getScaleFactor();
                float newZoom = currentZoomRatio * scaleFactor;
                applyZoom(newZoom);
                syncSeekBarWithZoom();
                return true;
            }
        });

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void setupZoomControls() {
        seekZoom.setMax(100);
        seekZoom.setProgress(0);
        updateZoomText(1f);

        seekZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || camera == null) return;

                float ratio = minZoomRatio + ((maxZoomRatio - minZoomRatio) * progress / 100f);
                applyZoom(ratio);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                REQ_CAMERA_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Utils.toast(this, "Bạn cần cấp quyền camera để chụp ảnh");
                finish();
            }
        }
    }
    private String saveBitmapToCache(Bitmap bitmap) throws Exception {
        java.io.File dir = new java.io.File(getCacheDir(), "captured_images");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        java.io.File file = new java.io.File(
                dir,
                "cap_" + System.currentTimeMillis() + ".jpg"
        );

        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos);
            fos.flush();
        } finally {
            fos.close();
        }

        return file.getAbsolutePath();
    }
    private Bitmap resizeIfNeeded(Bitmap bitmap, int maxSize) {
        if (bitmap == null) return null;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (w <= maxSize && h <= maxSize) {
            return bitmap;
        }

        float scale = Math.min(maxSize / (float) w, maxSize / (float) h);
        int newW = Math.max(1, Math.round(w * scale));
        int newH = Math.max(1, Math.round(h * scale));

        return Bitmap.createScaledBitmap(bitmap, newW, newH, true);
    }
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(new Size(1280, 1280))
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

                setupZoomState();
                setupTorchState();

                Log.d(TAG, "Camera started");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start camera", e);
                Utils.toast(this, "Không thể mở camera");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupZoomState() {
        if (camera == null) return;

        camera.getCameraInfo().getZoomState().observe(this, zoomState -> {
            if (zoomState == null) return;

            minZoomRatio = zoomState.getMinZoomRatio();
            maxZoomRatio = zoomState.getMaxZoomRatio();
            currentZoomRatio = zoomState.getZoomRatio();

            if (maxZoomRatio < 1f) maxZoomRatio = 1f;
            if (minZoomRatio < 1f) minZoomRatio = 1f;

            syncSeekBarWithZoom();
            updateZoomText(currentZoomRatio);
        });
    }

    private void setupTorchState() {
        if (camera == null) return;

        boolean hasFlash = camera.getCameraInfo().hasFlashUnit();
        btnFlash.setEnabled(hasFlash);
        btnFlash.setAlpha(hasFlash ? 1f : 0.5f);

        if (!hasFlash) {
            btnFlash.setText("Không có flash");
            return;
        }

        camera.getCameraInfo().getTorchState().observe(this, torchState -> {
            torchEnabled = torchState != null && torchState == androidx.camera.core.TorchState.ON;
            btnFlash.setText(torchEnabled ? "Flash bật" : "Flash tắt");
        });
    }

    private void toggleTorch() {
        if (camera == null) return;
        if (!camera.getCameraInfo().hasFlashUnit()) {
            Utils.toast(this, "Thiết bị này không hỗ trợ flash");
            return;
        }
        camera.getCameraControl().enableTorch(!torchEnabled);
    }

    private void applyZoom(float requestedRatio) {
        if (camera == null) return;

        float clamped = Math.max(minZoomRatio, Math.min(requestedRatio, maxZoomRatio));
        currentZoomRatio = clamped;
        camera.getCameraControl().setZoomRatio(clamped);
        updateZoomText(clamped);
    }

    private void syncSeekBarWithZoom() {
        if (maxZoomRatio <= minZoomRatio) {
            seekZoom.setProgress(0);
            return;
        }

        int progress = Math.round(((currentZoomRatio - minZoomRatio) / (maxZoomRatio - minZoomRatio)) * 100f);
        progress = Math.max(0, Math.min(progress, 100));
        seekZoom.setProgress(progress);
    }

    private void updateZoomText(float ratio) {
        txtZoomLabel.setText(String.format(Locale.getDefault(), "%.1fx", ratio));
    }

    private void captureImage() {
        if (imageCapture == null) {
            Utils.toast(this, "Camera chưa sẵn sàng");
            return;
        }

        btnCapture.setEnabled(false);

        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        try {
                            Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
                            image.close();

                            if (bitmap == null) {
                                btnCapture.setEnabled(true);
                                Utils.toast(CameraCaptureActivity.this, "Ảnh chụp không hợp lệ");
                                return;
                            }

                            Bitmap finalBitmap = resizeIfNeeded(bitmap, 1600);
                            String imagePath = saveBitmapToCache(finalBitmap);

                            Intent data = new Intent();
                            data.putExtra(EXTRA_CAPTURED_IMAGE_PATH, imagePath);
                            setResult(RESULT_OK, data);
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "capture processing failed", e);
                            btnCapture.setEnabled(true);
                            Utils.toast(CameraCaptureActivity.this, "Không thể xử lý ảnh chụp");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "capture failed", exception);
                        btnCapture.setEnabled(true);
                        Utils.toast(CameraCaptureActivity.this, "Chụp ảnh thất bại");
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.w(TAG, "unbindAll failed", e);
            }
        }
    }
}
