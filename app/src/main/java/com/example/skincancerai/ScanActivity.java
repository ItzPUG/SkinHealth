package com.example.skincancerai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";

    private static final String PREF_SCAN = "scan_prefs";
    private static final String KEY_LAST_PROFILE_ID = "last_profile_id";
    private static final String KEY_LAST_PROFILE_NAME = "last_profile_name";

    private static final int REQ_PICK_IMAGE = 1001;
    private static final int REQ_CROP_IMAGE = 1002;
    private static final int REQ_CAMERA_PERMISSION = 1003;
    private static final int REQ_CAMERA_CAPTURE = 1004;

    private static final int CLASSIFY_AVERAGED_RUNS = 1;
    private static final int MIN_ANALYSIS_DISPLAY_MS = 1200;

    private String currentProfileId = null;
    private String currentProfileName = null;

    private boolean isFollowUpMode = false;
    private String followUpSourceCheckId = null;
    private SkinCheck pendingFollowUpCheckForCompare = null;
    private SkinCheck pendingSavedCheckForDetail = null;

    private String trackingCaseId = null;
    private String trackingCaseTitle = null;

    private PreviewView previewView;
    private CameraOverlayView overlayView;

    private Button btnCamera, btnGallery, btnCapture;
    private Button btnResultOk, btnChangeProfile;

    private ImageView imgCroppedPreview;
    private LinearLayout placeholderNoImage;
    private LinearLayout resultPanel;
    private LinearLayout layoutThreat;
    private LinearLayout controlPanel;
    private ImageView btnBack;

    private TextView tvThreatLevel, tvThreatDesc, tvConclusion, tvConfidence, tvAdvice, tvWarning;
    private TextView tvSelectedProfile, tvSelectedProfileSub;
    private View viewThreatBar;

    private ImageCapture imageCapture;
    private SkinCancerClassifier classifier;
    private SkinInputValidator skinInputValidator;
    private ProcessCameraProvider cameraProvider;
    private boolean isCameraReady = false;

    private Bitmap lastAnalyzedBitmap;
    private boolean cropSourceIsCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);

        bindViews();
        if (!AppPreferences.isDataProcessingConsentGranted(this)) {
            Toast.makeText(this, "Vui lòng cấp quyền xử lý dữ liệu để sử dụng tính năng này", Toast.LENGTH_LONG).show();
            finish(); // Đóng màn hình ngay lập tức
            return;
        }

        classifier = new SkinCancerClassifier(this);
        skinInputValidator = new SkinInputValidator(this);

        isFollowUpMode = getIntent().getBooleanExtra("followUpMode", false);
        followUpSourceCheckId = getIntent().getStringExtra("followUpSourceCheckId");

        trackingCaseId = getIntent().getStringExtra("trackingCaseId");
        trackingCaseTitle = getIntent().getStringExtra("trackingCaseTitle");

        String fromIntent = getIntent().getStringExtra("profileId");
        if (fromIntent != null && !fromIntent.isEmpty()) {
            currentProfileId = fromIntent;
            loadSelectedProfileInfo(fromIntent);
        } else if (!restoreLastSelectedProfile()) {
            checkAndSelectProfile();
        }

        btnCamera.setOnClickListener(v -> {
            // 1. Kiểm tra xem đã chọn hồ sơ y tế chưa (Logic cũ của bạn)
            if (!ensureProfileSelected()) return;

            // 2. Kiểm tra quyền đồng ý xử lý dữ liệu (Logic mới)
            boolean isConsentGranted = AppPreferences.isDataProcessingConsentGranted(this);

            if (isConsentGranted) {
                // Nếu đã đồng ý, tiến hành mở camera như bình thường
                openCameraCaptureScreen();
            } else {
                // Nếu chưa đồng ý, thông báo và hướng dẫn người dùng bật lại
                showConsentRequiredDialog();
            }
        });

        btnGallery.setOnClickListener(v -> {
            if (!ensureProfileSelected()) return;
            openGallery();
        });

        btnCapture.setOnClickListener(v -> {
            if (!ensureProfileSelected()) return;
            captureFromCamera();
        });

        btnBack.setOnClickListener(v -> finish());
        btnChangeProfile.setOnClickListener(v -> checkAndSelectProfile());

        btnResultOk.setOnClickListener(v -> {
            resultPanel.setVisibility(View.GONE);
            controlPanel.setVisibility(View.VISIBLE);
            setActionButtonsEnabled(true);
            updateCaptureButtonState();

            if (pendingSavedCheckForDetail != null) {
                SkinCheck check = pendingSavedCheckForDetail;
                pendingSavedCheckForDetail = null;
                askOpenDetailNow(check);
                return;
            }

            if (pendingFollowUpCheckForCompare != null) {
                SkinCheck newCheck = pendingFollowUpCheckForCompare;
                pendingFollowUpCheckForCompare = null;
                askCompareWithSourceCheck(newCheck);
            }
        });
        applyIdleSurfaceState();
        updateSelectedProfileUI();
        Log.d(TAG, "ScanActivity onCreate completed");
    }
    private void showConsentRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền riêng tư")
                .setMessage("Để sử dụng chức năng phân tích hình ảnh bằng AI, bạn cần cấp quyền 'Xử lý dữ liệu' trong phần Cài đặt của trang Cá nhân.")
                .setPositiveButton("Đi đến Cài đặt", (d, w) -> {
                    // Chuyển hướng người dùng sang trang Profile hoặc mở trực tiếp Settings Panel
                    Intent intent = new Intent(this, ProfileActivity.class);
                    // Bạn có thể thêm Flag để ProfileActivity biết cần mở ngay Settings Panel
                    intent.putExtra("OPEN_SETTINGS", true);
                    startActivity(intent);
                })
                .setNegativeButton("Để sau", null)
                .show();
    }
    private void bindViews() {
        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);

        imgCroppedPreview = findViewById(R.id.imgCroppedPreview);
        placeholderNoImage = findViewById(R.id.placeholderNoImage);

        btnBack = findViewById(R.id.btnBack);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        btnCapture = findViewById(R.id.btnCapture);

        controlPanel = findViewById(R.id.controlPanel);
        resultPanel = findViewById(R.id.resultPanel);

        tvThreatLevel = findViewById(R.id.tvThreatLevel);
        tvThreatDesc = findViewById(R.id.tvThreatDesc);
        tvConclusion = findViewById(R.id.tvConclusion);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvAdvice = findViewById(R.id.tvAdvice);
        tvWarning = findViewById(R.id.tvWarning);

        viewThreatBar = findViewById(R.id.viewThreatBar);
        layoutThreat = findViewById(R.id.layoutThreat);

        btnResultOk = findViewById(R.id.btnResultOk);

        tvSelectedProfile = findViewById(R.id.tvSelectedProfile);
        tvSelectedProfileSub = findViewById(R.id.tvSelectedProfileSub);
        btnChangeProfile = findViewById(R.id.btnChangeProfile);
    }

    private void applyIdleSurfaceState() {
        leaveCameraMode();
        if (lastAnalyzedBitmap != null) {
            showBitmapPreview(lastAnalyzedBitmap);
        } else {
            placeholderNoImage.setVisibility(View.VISIBLE);
            imgCroppedPreview.setVisibility(View.GONE);
            imgCroppedPreview.setImageBitmap(null);
        }
        updateCaptureButtonState();
    }

    private void showBitmapPreview(Bitmap bitmap) {
        placeholderNoImage.setVisibility(View.GONE);
        previewView.setVisibility(View.GONE);
        overlayView.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        imgCroppedPreview.setVisibility(View.VISIBLE);
        imgCroppedPreview.setImageBitmap(bitmap);
    }

    private void leaveCameraMode() {
        previewView.setVisibility(View.GONE);
        overlayView.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        btnCapture.setEnabled(false);
        btnCapture.setAlpha(0.6f);
        isCameraReady = false;

        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.w(TAG, "leaveCameraMode unbindAll failed", e);
            }
        }
    }

    private void setActionButtonsEnabled(boolean enabled) {
        setViewEnabled(btnCamera, enabled);
        setViewEnabled(btnGallery, enabled);
        setViewEnabled(btnChangeProfile, enabled);
        if (btnCapture.getVisibility() == View.VISIBLE) {
            btnCapture.setEnabled(enabled && isCameraReady);
            btnCapture.setAlpha((enabled && isCameraReady) ? 1f : 0.6f);
        }
    }

    private void setViewEnabled(View view, boolean enabled) {
        if (view == null) return;
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.6f);
    }

    private void updateCaptureButtonState() {
        if (btnCapture.getVisibility() != View.VISIBLE) return;
        btnCapture.setEnabled(isCameraReady);
        btnCapture.setAlpha(isCameraReady ? 1f : 0.6f);
        btnCapture.setText(isCameraReady ? "Quét ngay" : "Đang mở camera...");
    }

    private boolean ensureProfileSelected() {
        if (currentProfileId == null || currentProfileId.trim().isEmpty()) {
            Utils.toast(this, getString(R.string.scan_select_profile_first));
            checkAndSelectProfile();
            return false;
        }
        return true;
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

    private void checkAndSelectProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Utils.toast(this, getString(R.string.scan_no_logged_account));
            finish();
            return;
        }

        DatabaseReference profileRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles");

        profileRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.scan_no_profile_title)
                        .setMessage(R.string.scan_no_profile_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.scan_create_profile, (d, w) -> {
                            Intent i = new Intent(this, MedicalProfileEditActivity.class);
                            startActivity(i);
                            finish();
                        })
                        .show();
                return;
            }

            List<String> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();

            for (DataSnapshot child : snapshot.getChildren()) {
                MedicalProfile p = child.getValue(MedicalProfile.class);
                if (p != null) {
                    ids.add(child.getKey());
                    names.add(p.fullName != null ? p.fullName : getString(R.string.profile_unnamed));
                }
            }

            showProfilePicker(ids, names);
        }).addOnFailureListener(e -> {
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    getString(R.string.error_title_common),
                    getString(R.string.error_load_profiles_message),
                    getString(R.string.feedback_retry),
                    this::checkAndSelectProfile,
                    getString(R.string.feedback_close),
                    this::finish
            );
        });
    }

    private void showProfilePicker(List<String> ids, List<String> names) {
        if (ids == null || ids.isEmpty()) return;

        String[] items = names.toArray(new String[0]);

        int checkedIndex = -1;
        if (currentProfileId != null) {
            checkedIndex = ids.indexOf(currentProfileId);
        }

        final int[] selectedIndex = {checkedIndex};

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Chọn hồ sơ để quét")
                .setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton("Chọn", (dialog, which) -> {
                    int index = selectedIndex[0];
                    if (index >= 0 && index < ids.size()) {
                        currentProfileId = ids.get(index);
                        currentProfileName = names.get(index);
                        updateSelectedProfileUI();
                        saveLastSelectedProfile(currentProfileId, currentProfileName);
                        Utils.toast(this, "Đã chọn hồ sơ: " + currentProfileName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Tạo hồ sơ", (dialog, which) -> {
                    Intent i = new Intent(this, MedicalProfileEditActivity.class);
                    startActivity(i);
                })
                .show();
    }

    private void loadSelectedProfileInfo(String profileId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || profileId == null || profileId.trim().isEmpty()) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(profileId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        currentProfileId = null;
                        currentProfileName = null;
                        clearLastSelectedProfile();
                        updateSelectedProfileUI();
                        return;
                    }

                    MedicalProfile p = snapshot.getValue(MedicalProfile.class);
                    if (p != null) {
                        currentProfileName = p.fullName;
                        updateSelectedProfileUI();
                        saveLastSelectedProfile(profileId, currentProfileName);
                    }
                });
    }

    private void openCameraCaptureScreen() {
        Intent intent = new Intent(this, CameraCaptureActivity.class);
        startActivityForResult(intent, REQ_CAMERA_CAPTURE);
    }
    private void openCropScreen(Uri imageUri, boolean fromCamera) {
        if (imageUri == null) {
            Utils.toast(this, "Không thể mở ảnh để cắt vùng cần phân tích.");
            return;
        }

        cropSourceIsCamera = fromCamera;

        Intent cropIntent = new Intent(this, ImageCropActivity.class);
        cropIntent.putExtra(
                ImageCropActivity.EXTRA_IMAGE_URI,
                imageUri.toString()
        );

        // Cho phép ImageCropActivity đọc ảnh chụp từ cache
        cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(cropIntent, REQ_CROP_IMAGE);
    }

    private void updateSelectedProfileUI() {
        if (tvSelectedProfile == null || tvSelectedProfileSub == null || btnChangeProfile == null) return;

        if (currentProfileName != null && !currentProfileName.trim().isEmpty()) {
            tvSelectedProfile.setText(TextSanitizer.sanitize(currentProfileName));
            if (isFollowUpMode) {
                tvSelectedProfileSub.setText(R.string.scan_profile_ready_followup);
            } else {
                tvSelectedProfileSub.setText(R.string.scan_profile_ready_normal);
            }
            btnChangeProfile.setText("Đổi hồ sơ");
        } else {
            tvSelectedProfile.setText("Chưa chọn hồ sơ");
            tvSelectedProfileSub.setText(R.string.scan_profile_not_selected);
            btnChangeProfile.setText("Chọn hồ sơ");
        }
    }

    private void openCameraMode() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }

        resultPanel.setVisibility(View.GONE);
        controlPanel.setVisibility(View.VISIBLE);

        placeholderNoImage.setVisibility(View.GONE);
        imgCroppedPreview.setVisibility(View.GONE);
        imgCroppedPreview.setImageBitmap(null);

        previewView.setVisibility(View.VISIBLE);
        overlayView.setVisibility(View.VISIBLE);
        btnCapture.setVisibility(View.VISIBLE);
        btnCapture.setEnabled(false);
        btnCapture.setAlpha(0.6f);
        isCameraReady = false;
        updateCaptureButtonState();

        startCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraMode();
            } else {
                UiFeedback.showActionDialog(
                        this,
                        R.drawable.ic_status_warning,
                        getString(R.string.error_camera_permission_title),
                        getString(R.string.error_camera_permission_message),
                        getString(R.string.feedback_retry),
                        this::requestCameraPermission,
                        getString(R.string.feedback_close),
                        null
                );
            }
        }
    }

    private void startCamera() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }
        imageCapture = null;
        isCameraReady = false;
        updateCaptureButtonState();

        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    if (!hasCameraPermission()) {
                        Utils.toast(this, "Quyền camera chưa sẵn sàng");
                        return;
                    }

                    cameraProvider = cameraProviderFuture.get();
                    if (cameraProvider == null) {
                        Log.e(TAG, "Camera provider is null");
                        Utils.toast(this, "Không thể khởi tạo camera");
                        return;
                    }

                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    imageCapture = new ImageCapture.Builder()
                            .setTargetResolution(new Size(1024, 1024))
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build();

                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(
                            this,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                    );

                    isCameraReady = true;
                    updateCaptureButtonState();
                    Log.d(TAG, "Camera started successfully");
                } catch (SecurityException se) {
                    isCameraReady = false;
                    updateCaptureButtonState();
                    Log.e(TAG, "SecurityException when binding camera", se);
                    Utils.toast(this, "Ứng dụng không có quyền dùng camera");
                } catch (Exception e) {
                    isCameraReady = false;
                    updateCaptureButtonState();
                    Log.e(TAG, "Failed to start camera: " + e.getMessage(), e);
                    Utils.toast(this, "Lỗi khởi tạo camera: " + e.getMessage());
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException in startCamera()", se);
            Utils.toast(this, "Không thể truy cập camera");
        } catch (Exception e) {
            Log.e(TAG, "startCamera() failed", e);
            Utils.toast(this, "Không thể mở camera");
        }
    }

    private void captureFromCamera() {
        Log.d(TAG, "captureFromCamera() called");

        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }

        if (!isCameraReady) {
            Utils.toast(this, "Camera đang khởi tạo, vui lòng chờ một chút.");
            startCamera();
            return;
        }

        if (imageCapture == null) {
            Log.w(TAG, "imageCapture is null");
            Utils.toast(this, getString(R.string.scan_camera_not_ready));
            startCamera();
            return;
        }

        try {
            Utils.toast(this, getString(R.string.scan_capturing));
            setActionButtonsEnabled(false);

            imageCapture.takePicture(
                    ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            try {
                                Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
                                image.close();

                                if (bitmap == null) {
                                    runOnUiThread(() -> {
                                        setActionButtonsEnabled(true);
                                        updateCaptureButtonState();
                                        UiFeedback.showActionDialog(
                                                ScanActivity.this,
                                                R.drawable.ic_status_warning,
                                                getString(R.string.error_capture_failed_title),
                                                getString(R.string.error_capture_failed_message),
                                                getString(R.string.feedback_retry_capture),
                                                ScanActivity.this::captureFromCamera,
                                                getString(R.string.feedback_close),
                                                null
                                        );
                                    });
                                    return;
                                }

                                Bitmap cropped = cropToFocusArea(bitmap);

                                runOnUiThread(() -> {
                                    leaveCameraMode();
                                    showBitmapPreview(cropped);
                                    setActionButtonsEnabled(true);
                                    runModel(cropped);
                                });

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing captured image", e);
                                runOnUiThread(() -> {
                                    setActionButtonsEnabled(true);
                                    UiFeedback.showActionDialog(
                                            ScanActivity.this,
                                            R.drawable.ic_status_warning,
                                            getString(R.string.error_capture_failed_title),
                                            getString(R.string.error_capture_failed_message),
                                            getString(R.string.feedback_retry_capture),
                                            ScanActivity.this::captureFromCamera,
                                            getString(R.string.feedback_close),
                                            null
                                    );
                                });
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Image capture failed", exception);
                            runOnUiThread(() -> {
                                setActionButtonsEnabled(true);
                                updateCaptureButtonState();
                                UiFeedback.showActionDialog(
                                        ScanActivity.this,
                                        R.drawable.ic_status_warning,
                                        getString(R.string.error_capture_failed_title),
                                        getString(R.string.error_capture_failed_message),
                                        getString(R.string.feedback_retry_capture),
                                        ScanActivity.this::captureFromCamera,
                                        getString(R.string.feedback_close),
                                        null
                                );
                            });
                        }
                    }
            );
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException when calling takePicture()", se);
            setActionButtonsEnabled(true);
            Utils.toast(this, "Ứng dụng không có quyền chụp ảnh");
        } catch (Exception e) {
            Log.e(TAG, "Exception when calling takePicture()", e);
            setActionButtonsEnabled(true);
            Utils.toast(this, "Lỗi khi chụp ảnh: " + e.getMessage());
        }
    }

    private Bitmap cropToFocusArea(Bitmap bitmap) {
        int viewW = previewView.getWidth();
        int viewH = previewView.getHeight();

        if (viewW <= 0 || viewH <= 0) {
            int sizeCenter = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int cx = (bitmap.getWidth() - sizeCenter) / 2;
            int cy = (bitmap.getHeight() - sizeCenter) / 2;
            return Bitmap.createBitmap(bitmap, cx, cy, sizeCenter, sizeCenter);
        }

        RectF rect = overlayView.getFocusRect();
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
            int sizeView = Math.min(viewW, viewH) * 2 / 3;
            int leftView = (viewW - sizeView) / 2;
            int topView = (viewH - sizeView) / 2;
            rect = new RectF(leftView, topView, leftView + sizeView, topView + sizeView);
        }

        float scaleX = (float) bitmap.getWidth() / (float) viewW;
        float scaleY = (float) bitmap.getHeight() / (float) viewH;

        int left = (int) (rect.left * scaleX);
        int top = (int) (rect.top * scaleY);
        int size = (int) (rect.width() * scaleX);

        left = Math.max(0, left);
        top = Math.max(0, top);
        size = Math.min(size,
                Math.min(bitmap.getWidth() - left, bitmap.getHeight() - top));

        if (size <= 0) {
            int sizeCenter = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int cx = (bitmap.getWidth() - sizeCenter) / 2;
            int cy = (bitmap.getHeight() - sizeCenter) / 2;
            return Bitmap.createBitmap(bitmap, cx, cy, sizeCenter, sizeCenter);
        }

        return Bitmap.createBitmap(bitmap, left, top, size, size);
    }

    private void openGallery() {
        leaveCameraMode();
        resultPanel.setVisibility(View.GONE);
        controlPanel.setVisibility(View.VISIBLE);
        setActionButtonsEnabled(true);

        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_PICK_IMAGE);
        } catch (Exception e) {
            Log.e(TAG, "openGallery failed", e);
            Utils.toast(this, "Không thể mở thư viện ảnh");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // =========================================================
        // 1. Ảnh vừa chụp từ camera
        // =========================================================
        if (requestCode == REQ_CAMERA_CAPTURE
                && resultCode == RESULT_OK
                && data != null) {

            try {
                String imagePath = data.getStringExtra(
                        CameraCaptureActivity.EXTRA_CAPTURED_IMAGE_PATH
                );

                if (imagePath == null || imagePath.trim().isEmpty()) {
                    UiFeedback.showActionDialog(
                            this,
                            R.drawable.ic_status_warning,
                            getString(R.string.error_capture_failed_title),
                            getString(R.string.error_capture_failed_message),
                            "Thử chụp lại",
                            this::openCameraCaptureScreen,
                            getString(R.string.feedback_close),
                            null
                    );
                    return;
                }

                File capturedFile = new File(imagePath);

                if (!capturedFile.exists() || !capturedFile.isFile()) {
                    Utils.toast(this, "Ảnh chụp không hợp lệ.");
                    return;
                }

                // Chuyển đường dẫn file thành content:// URI an toàn
                Uri capturedUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        capturedFile
                );

                // Chuyển sang màn hình crop trước khi chạy mô hình
                openCropScreen(capturedUri, true);

            } catch (Exception e) {
                Log.e(TAG, "CameraCaptureActivity result failed", e);
                Utils.toast(this, "Không thể đọc ảnh vừa chụp");
            }
        }

        // =========================================================
        // 2. Ảnh được chọn từ thư viện
        // =========================================================
        if (requestCode == REQ_PICK_IMAGE
                && resultCode == RESULT_OK
                && data != null) {

            try {
                openCropScreen(data.getData(), false);
            } catch (Exception e) {
                Log.e(TAG, "Open crop activity failed", e);
                Utils.toast(this, "Không thể mở ảnh từ thư viện");
            }
        }

        // =========================================================
        // 3. Nhận ảnh sau khi người dùng crop
        // =========================================================
        if (requestCode == REQ_CROP_IMAGE
                && resultCode == RESULT_OK
                && data != null) {

            try {
                byte[] jpeg = data.getByteArrayExtra(
                        ImageCropActivity.EXTRA_CROPPED_JPEG
                );

                if (jpeg == null) {
                    UiFeedback.showActionDialog(
                            this,
                            R.drawable.ic_status_warning,
                            getString(R.string.error_capture_failed_title),
                            getString(R.string.error_capture_failed_message),
                            getString(R.string.feedback_retry),

                            // Crop ảnh chụp bị lỗi thì mở lại camera.
                            // Crop ảnh thư viện bị lỗi thì mở lại thư viện.
                            cropSourceIsCamera
                                    ? this::openCameraCaptureScreen
                                    : this::openGallery,

                            getString(R.string.feedback_close),
                            null
                    );
                    return;
                }

                Bitmap cropped = BitmapFactory.decodeByteArray(
                        jpeg,
                        0,
                        jpeg.length
                );

                if (cropped == null) {
                    Utils.toast(this, "Ảnh crop không hợp lệ.");
                    return;
                }

                // Chỉ chạy mô hình sau khi người dùng xác nhận vùng crop
                showBitmapPreview(cropped);
                runModel(cropped);

            } catch (Exception e) {
                Log.e(TAG, "onActivityResult crop failed", e);
                Utils.toast(this, "Không thể xử lý ảnh sau khi crop");
            }
        }
    }
    private Bitmap decodeBitmapFromPath(String path) {
        try {
            if (path == null || path.trim().isEmpty()) return null;

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inSampleSize = calculateInSampleSize(bounds, 1600, 1600);

            return BitmapFactory.decodeFile(path, opts);
        } catch (Exception e) {
            Log.e(TAG, "decodeBitmapFromPath failed", e);
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return Math.max(1, inSampleSize);
    }
    private void runModel(Bitmap bitmap) {
        Log.d(TAG, "runModel() called");

        if (!AppPreferences.isDataProcessingConsentGranted(this)) {
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    "Quyen rieng tu",
                    "Ban da rut lai su dong y xu ly du lieu. Ung dung se khong phan tich/luu du lieu scan moi cho den khi ban dong y lai trong phan cai dat.",
                    "Da hieu",
                    null,
                    null,
                    null
            );
            return;
        }

        if (bitmap == null) {
            Utils.toast(this, "Ảnh không hợp lệ");
            return;
        }

        lastAnalyzedBitmap = bitmap;
        setActionButtonsEnabled(false);

        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Đang kiểm tra ảnh và phân tích AI...");
        progress.setCancelable(false);
        progress.show();

        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            AnalysisPayload payload = new AnalysisPayload();

            try {
                payload.skinValidation = skinInputValidator.analyze(bitmap);

                if (payload.skinValidation != null && payload.skinValidation.isSkin) {
                    payload.diseaseResult = classifier.classifyAveraged(bitmap, CLASSIFY_AVERAGED_RUNS);
                }
            } catch (Exception e) {
                payload.error = e;
                Log.e(TAG, "Error in runModel", e);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = Math.max(0, MIN_ANALYSIS_DISPLAY_MS - elapsed);

            runOnUiThread(() -> {
                if (remaining > 0) {
                    new android.os.Handler(getMainLooper()).postDelayed(
                            () -> dismissAndHandleAnalysis(progress, payload),
                            remaining
                    );
                } else {
                    dismissAndHandleAnalysis(progress, payload);
                }
            });
        }).start();
    }

    private void dismissAndHandleAnalysis(android.app.ProgressDialog progress, AnalysisPayload payload) {
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (Exception e) {
            Log.w(TAG, "Dismiss progress failed", e);
        }

        setActionButtonsEnabled(true);
        updateCaptureButtonState();

        if (payload == null) {
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    getString(R.string.error_title_common),
                    getString(R.string.error_analyze_failed_message),
                    getString(R.string.feedback_retry_analyze),
                    () -> {
                        if (lastAnalyzedBitmap != null) runModel(lastAnalyzedBitmap);
                    },
                    getString(R.string.feedback_close),
                    null
            );
            return;
        }

        if (payload.error != null) {
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    getString(R.string.error_title_common),
                    getString(R.string.error_analyze_failed_message),
                    getString(R.string.feedback_retry_analyze),
                    () -> {
                        if (lastAnalyzedBitmap != null) runModel(lastAnalyzedBitmap);
                    },
                    getString(R.string.feedback_close),
                    null
            );
            return;
        }

        if (payload.skinValidation == null) {
            Utils.toast(this, "Không thể kiểm tra ảnh đầu vào");
            return;
        }

        if (!payload.skinValidation.isSkin) {
            showNonSkinDialog(payload.skinValidation);
            return;
        }

        if (payload.diseaseResult == null) {
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    getString(R.string.error_title_common),
                    getString(R.string.error_classify_failed_message),
                    getString(R.string.feedback_retry_analyze),
                    () -> {
                        if (lastAnalyzedBitmap != null) runModel(lastAnalyzedBitmap);
                    },
                    getString(R.string.feedback_close),
                    null
            );
            return;
        }

        if (currentProfileId == null || currentProfileId.trim().isEmpty()) {
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    getString(R.string.error_save_without_profile_title),
                    getString(R.string.error_save_without_profile_message),
                    getString(R.string.feedback_select_profile),
                    this::checkAndSelectProfile,
                    getString(R.string.feedback_close),
                    null
            );
            return;
        }

        String imageBase64 = encodeBitmapToBase64(lastAnalyzedBitmap);
        SkinCheck savedCheck = saveSkinCheck(currentProfileId, payload.diseaseResult, false, imageBase64);
        pendingSavedCheckForDetail = savedCheck;

        if (isFollowUpMode
                && followUpSourceCheckId != null
                && !followUpSourceCheckId.trim().isEmpty()) {
            pendingFollowUpCheckForCompare = savedCheck;
        }

        showResult(payload.diseaseResult);
    }

    private void showNonSkinDialog(SkinInputValidator.ValidationResult validation) {
        String title = getString(R.string.scan_input_invalid_title);
        String message = getString(R.string.scan_input_invalid_message);
        String tips = getString(R.string.scan_input_tips);

        if (validation != null && validation.reason != null) {
            if ("Khong phat hien du vung da".equals(validation.reason)) {
                title = getString(R.string.scan_input_reason_not_skin_title);
                message = getString(R.string.scan_input_reason_not_skin_message);
            } else if ("Anh qua toi hoac qua sang".equals(validation.reason)) {
                title = getString(R.string.scan_input_reason_light_title);
                message = getString(R.string.scan_input_reason_light_message);
            } else if ("Anh bi mo, vui long chup ro hon".equals(validation.reason)) {
                title = getString(R.string.scan_input_reason_blur_title);
                message = getString(R.string.scan_input_reason_blur_message);
            }
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_quality, null, false);
        TextView txtTitle = dialogView.findViewById(R.id.txtDialogTitle);
        TextView txtMessage = dialogView.findViewById(R.id.txtDialogMessage);
        TextView txtTips = dialogView.findViewById(R.id.txtDialogTips);
        View btnClose = dialogView.findViewById(R.id.btnDialogClose);
        View btnRetry = dialogView.findViewById(R.id.btnDialogRetry);

        txtTitle.setText(title);
        txtMessage.setText(message);
        txtTips.setText(tips);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRetry.setOnClickListener(v -> {
            dialog.dismiss();
            openCameraCaptureScreen();
        });

        dialog.show();
    }

    private void showResult(Result result) {
        controlPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);

        String normalizedLabel = TextSanitizer.normalizeResultLabel(result.label);
        int riskLevel = TextSanitizer.riskLevel(normalizedLabel);

        String threat;
        String desc;
        String advice;
        int color;
        int bg;

        if (riskLevel == TextSanitizer.RISK_HIGH) {
            threat = getString(R.string.scan_threat_high);
            desc = getString(R.string.scan_threat_high_desc);
            advice = getString(R.string.scan_threat_high_advice);
            color = android.graphics.Color.parseColor("#DC2626");
            bg = android.graphics.Color.parseColor("#FEF2F2");
        } else if (riskLevel == TextSanitizer.RISK_MEDIUM) {
            threat = getString(R.string.scan_threat_medium);
            desc = getString(R.string.scan_threat_medium_desc);
            advice = getString(R.string.scan_threat_medium_advice);
            color = android.graphics.Color.parseColor("#D97706");
            bg = android.graphics.Color.parseColor("#FFF7ED");
        } else {
            threat = getString(R.string.scan_threat_low);
            desc = getString(R.string.scan_threat_low_desc);
            advice = getString(R.string.scan_threat_low_advice);
            color = android.graphics.Color.parseColor("#2563EB");
            bg = android.graphics.Color.parseColor("#EEF7FF");
        }

        if (isFollowUpMode) {
            advice = advice + "\n\nĐây là lần tái quét. Bạn có thể so sánh với ảnh trước đó sau khi đóng kết quả này.";
        }

        tvThreatLevel.setText(threat);
        tvThreatLevel.setTextColor(color);
        tvThreatDesc.setText(desc);
        tvConclusion.setText(getString(R.string.scan_conclusion_format, normalizedLabel));
        tvConfidence.setText(getString(R.string.scan_confidence_format, result.confidence * 100f));
        tvAdvice.setText(advice);

        if (tvWarning != null) {
            if (riskLevel == TextSanitizer.RISK_HIGH) {
                tvWarning.setText("Kết quả sàng lọc cho thấy mức nguy cơ cao. Khuyến nghị đi khám bác sĩ chuyên khoa da liễu sớm nhất có thể.");
            } else if (riskLevel == TextSanitizer.RISK_MEDIUM) {
                tvWarning.setText("Kết quả sàng lọc cho thấy mức nguy cơ trung bình. Nên theo dõi sát và kiểm tra lại nếu tổn thương thay đổi.");
            } else {
                tvWarning.setText("Kết quả sàng lọc cho thấy mức nguy cơ thấp. Tiếp tục theo dõi, và kiểm tra lại nếu tổn thương thay đổi kích thước hoặc màu sắc.");
            }
        }

        viewThreatBar.setBackgroundColor(color);
        layoutThreat.setBackgroundColor(bg);
    }

    private SkinCheck saveSkinCheck(String profileId,
                                    Result result,
                                    boolean reminder,
                                    String imageBase64) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Utils.toast(this, "Không tìm thấy tài khoản đăng nhập");
            return null;
        }

        if (profileId == null || profileId.trim().isEmpty()) {
            Utils.toast(this, "Chưa chọn hồ sơ để lưu kết quả");
            return null;
        }

        String checkId = String.valueOf(System.currentTimeMillis());

        SkinCheck check = new SkinCheck(
                checkId,
                TextSanitizer.normalizeResultLabel(result.label),
                result.confidence,
                imageBase64
        );

        check.createdAt = System.currentTimeMillis();
        check.reminderEnabled = reminder;
        check.reminderAt = 0L;
        check.isFollowUp = isFollowUpMode;
        check.followUpFromId = followUpSourceCheckId != null ? followUpSourceCheckId : "";
        check.lesionCaseId = trackingCaseId != null ? trackingCaseId : "";
        check.lesionCaseTitle = trackingCaseTitle != null ? trackingCaseTitle : "";
        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("medical_profiles")
                        .child(profileId)
                        .child("skin_checks")
                        .child(checkId);

        ref.setValue(check).addOnSuccessListener(unused -> updateTrackingCaseSummary(profileId, check));
        return check;
    }
    private void updateTrackingCaseSummary(String profileId, SkinCheck check) {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null
                || profileId == null
                || check == null
                || check.lesionCaseId == null
                || check.lesionCaseId.trim().isEmpty()) {
            return;
        }

        DatabaseReference caseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(profileId)
                .child("lesion_cases")
                .child(check.lesionCaseId);

        caseRef.child("updatedAt").setValue(System.currentTimeMillis());
        caseRef.child("lastScanAt").setValue(check.createdAt);
        caseRef.child("latestCheckId").setValue(check.id);
        caseRef.child("latestRiskLabel").setValue(TextSanitizer.normalizeResultLabel(check.resultLabel));
        caseRef.child("latestConfidence").setValue(check.confidence);
        caseRef.child("coverImageBase64").setValue(check.imageBase64);

        recalculateTrackingCaseScanCount(profileId, check.lesionCaseId);
    }

    private void recalculateTrackingCaseScanCount(String profileId, String caseId) {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null
                || profileId == null
                || caseId == null
                || caseId.trim().isEmpty()) {
            return;
        }

        DatabaseReference profileRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(profileId);

        profileRef.child("skin_checks").get().addOnSuccessListener(snapshot -> {
            int count = 0;

            for (DataSnapshot child : snapshot.getChildren()) {
                SkinCheck sc = child.getValue(SkinCheck.class);

                if (sc != null && caseId.equals(sc.lesionCaseId)) {
                    count++;
                }
            }

            profileRef.child("lesion_cases")
                    .child(caseId)
                    .child("scanCount")
                    .setValue(count);
        });
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        try {
            int maxSize = 512;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            float scale = 1f;
            if (width > height && width > maxSize) {
                scale = maxSize / (float) width;
            } else if (height >= width && height > maxSize) {
                scale = maxSize / (float) height;
            }

            Bitmap scaled = bitmap;
            if (scale < 1f) {
                int newW = Math.round(width * scale);
                int newH = Math.round(height * scale);
                scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] bytes = baos.toByteArray();

            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.w(TAG, "encodeBitmapToBase64 failed", e);
            return null;
        }
    }

    private void askCompareWithSourceCheck(SkinCheck newCheck) {
        if (!isFollowUpMode
                || followUpSourceCheckId == null
                || followUpSourceCheckId.trim().isEmpty()) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || currentProfileId == null) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("skin_checks")
                .child(followUpSourceCheckId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    SkinCheck oldCheck = snapshot.getValue(SkinCheck.class);
                    if (oldCheck == null || newCheck == null) return;

                    new AlertDialog.Builder(this)
                            .setTitle("So sánh lần quét")
                            .setMessage("Bạn có muốn so sánh với lần quét trước đó không?")
                            .setPositiveButton("So sánh",
                                    (d, w) -> openCompareActivity(oldCheck, newCheck))
                            .setNegativeButton("Để sau", null)
                            .show();
                });
    }

    private void askOpenDetailNow(SkinCheck check) {
        if (check == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Xem chi tiết kết quả?")
                .setMessage("Bạn có muốn mở ngay màn hình chi tiết của lần quét vừa lưu không?")
                .setPositiveButton("Xem ngay", (d, w) -> openHistoryDetail(check))
                .setNegativeButton("Để sau", (d, w) -> {
                    if (pendingFollowUpCheckForCompare != null) {
                        SkinCheck newCheck = pendingFollowUpCheckForCompare;
                        pendingFollowUpCheckForCompare = null;
                        askCompareWithSourceCheck(newCheck);
                    }
                })
                .show();
    }

    private void openHistoryDetail(SkinCheck check) {
        if (check == null) return;

        Intent intent = new Intent(this, HistoryDetailActivity.class);
        intent.putExtra("profileId", currentProfileId);
        intent.putExtra("profileName", TextSanitizer.sanitize(currentProfileName));
        intent.putExtra("checkId", check.id);
        intent.putExtra("resultLabel", TextSanitizer.sanitize(check.resultLabel));
        intent.putExtra("confidence", check.confidence);
        intent.putExtra("createdAt", check.createdAt);
        intent.putExtra("imageBase64", check.imageBase64);
        intent.putExtra("note", check.note);
        intent.putExtra("reminderEnabled", check.reminderEnabled);
        intent.putExtra("reminderDays", check.reminderDays);
        intent.putExtra("reminderAt", check.reminderAt);
        intent.putExtra("isFollowUp", check.isFollowUp);
        intent.putExtra("followUpFromId", check.followUpFromId);
        intent.putExtra("bodyPart", check.bodyPart);
        intent.putExtra("lesionCaseId", check.lesionCaseId);
        intent.putExtra("lesionCaseTitle", check.lesionCaseTitle);
        PageTransitionHelper.navigateWithLoading(this, intent);
    }

    private void openCompareActivity(SkinCheck oldCheck, SkinCheck newCheck) {
        Intent intent = new Intent(this, CompareScanActivity.class);

        intent.putExtra("profileName", currentProfileName);

        intent.putExtra("oldResultLabel", TextSanitizer.normalizeResultLabel(oldCheck.resultLabel));
        intent.putExtra("oldConfidence", oldCheck.confidence);
        intent.putExtra("oldCreatedAt", oldCheck.createdAt);
        intent.putExtra("oldImageBase64", oldCheck.imageBase64);

        intent.putExtra("newResultLabel", TextSanitizer.normalizeResultLabel(newCheck.resultLabel));
        intent.putExtra("newConfidence", newCheck.confidence);
        intent.putExtra("newCreatedAt", newCheck.createdAt);
        intent.putExtra("newImageBase64", newCheck.imageBase64);

        startActivity(intent);
    }
    private void saveLastSelectedProfile(String profileId, String profileName) {
        getSharedPreferences(PREF_SCAN, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_PROFILE_ID, profileId)
                .putString(KEY_LAST_PROFILE_NAME, profileName)
                .apply();
    }

    private boolean restoreLastSelectedProfile() {
        String savedId = getSharedPreferences(PREF_SCAN, MODE_PRIVATE)
                .getString(KEY_LAST_PROFILE_ID, null);
        String savedName = getSharedPreferences(PREF_SCAN, MODE_PRIVATE)
                .getString(KEY_LAST_PROFILE_NAME, null);

        if (savedId == null || savedId.trim().isEmpty()) {
            return false;
        }

        currentProfileId = savedId;
        currentProfileName = savedName;
        updateSelectedProfileUI();
        loadSelectedProfileInfo(savedId);
        return true;
    }

    private void clearLastSelectedProfile() {
        getSharedPreferences(PREF_SCAN, MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_PROFILE_ID)
                .remove(KEY_LAST_PROFILE_NAME)
                .apply();
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

        if (classifier != null) {
            classifier.close();
        }

        if (skinInputValidator != null) {
            skinInputValidator.close();
        }
    }

    private static class AnalysisPayload {
        SkinInputValidator.ValidationResult skinValidation;
        Result diseaseResult;
        Exception error;
    }
}
