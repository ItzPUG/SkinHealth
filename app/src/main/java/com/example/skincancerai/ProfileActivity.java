package com.example.skincancerai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtName;
    private TextView txtSub;
    private ImageView imgAvatar;
    private ImageView btnChangeAvatar;
    private ImageView btnSettings;
    private LinearLayout navHome;
    private LinearLayout navHistory;
    private LinearLayout navProfile;
    private LinearLayout navHealth;
    private FloatingActionButton fabCamera;

    private FirebaseUser user;
    private FirebaseAuth auth;
    private DatabaseReference profileRef;

    private static final String DB_URL =
            "https://skincancerai-6c951-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final ActivityResultLauncher<String> pickAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickedAvatar);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            PageTransitionHelper.navigateWithLoading(
                    this,
                    new Intent(this, LoginActivity.class),
                    true
            );
            return;
        }

        auth = FirebaseAuth.getInstance();
        profileRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(user.getUid())
                .child("profile");

        bindViews();
        setupToolbar();
        setupActions();
        setupBottomNav();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void bindViews() {
        txtName = findViewById(R.id.txtName);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnSettings = findViewById(R.id.btnSettings);

        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navHealth = findViewById(R.id.navHealth);
        navProfile = findViewById(R.id.navProfile);
        fabCamera = findViewById(R.id.fabCamera);

        txtSub = findViewById(R.id.txtSub);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupActions() {
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.signOut();
            PageTransitionHelper.navigateWithLoading(
                    ProfileActivity.this,
                    new Intent(ProfileActivity.this, LoginActivity.class),
                    true
            );
        });

        btnChangeAvatar.setOnClickListener(v -> openGallery());

        findViewById(R.id.layoutPersonalInfo).setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        ProfileActivity.this,
                        new Intent(ProfileActivity.this, PersonalInfoActivity.class)
                )
        );

        btnSettings.setOnClickListener(v -> showSettingsPanel());

//        findViewById(R.id.layoutSettingsCard).setOnClickListener(v -> showSettingsPanel());

        findViewById(R.id.layoutMedicalProfile).setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        ProfileActivity.this,
                        new Intent(ProfileActivity.this, MedicalProfileListActivity.class)
                )
        );

        findViewById(R.id.layoutTermsService).setOnClickListener(v -> showTermsDialog());

        findViewById(R.id.layoutFeedback).setOnClickListener(v -> showFeedbackDialog());
    }

    private void setupBottomNav() {
        setupBottomNavLabelsAndIcons();

        navHome.setOnClickListener(v -> {
            PageTransitionHelper.navigateWithLoading(
                    ProfileActivity.this,
                    new Intent(ProfileActivity.this, MainActivity.class),
                    true
            );
        });

        navHistory.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        ProfileActivity.this,
                        new Intent(ProfileActivity.this, HistoryActivity.class)
                )
        );

        navHealth.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        ProfileActivity.this,
                        new Intent(ProfileActivity.this, MedicalProfileListActivity.class)
                )
        );

        fabCamera.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        ProfileActivity.this,
                        new Intent(ProfileActivity.this, ScanActivity.class)
                )
        );

        navProfile.setOnClickListener(v -> {
            // current screen
        });
    }
    private void updateConsentOnFirebase(boolean isGranted) {
        // 1. Cập nhật Local để dùng ngay lập tức
        AppPreferences.setDataProcessingConsent(this, isGranted);

        // 2. Chuẩn bị dữ liệu cập nhật theo yêu cầu của Thầy
        Map<String, Object> updates = new HashMap<>();
        updates.put("isConsent", isGranted);                  // Trạng thái đồng ý
        updates.put("consentAt", System.currentTimeMillis());   // Thời điểm thực hiện
        updates.put("termsVersion", "v1.0");                   // Phiên bản điều khoản

        profileRef.updateChildren(updates).addOnSuccessListener(unused -> {
            String msg = isGranted ? "Đã đồng ý xử lý dữ liệu" : "Đã từ chối xử lý dữ liệu";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
    private void setupBottomNavLabelsAndIcons() {
        updateBottomNavItem(navHealth, R.drawable.ic_profile, getString(R.string.bottom_label_profile));
        updateBottomNavItem(navProfile, R.drawable.ic_account, getString(R.string.bottom_label_account));
    }

    private void updateBottomNavItem(LinearLayout navItem, int iconRes, String label) {
        if (navItem == null || navItem.getChildCount() < 2) return;
        if (navItem.getChildAt(0) instanceof ImageView) {
            ((ImageView) navItem.getChildAt(0)).setImageResource(iconRes);
        }
        if (navItem.getChildAt(1) instanceof TextView) {
            ((TextView) navItem.getChildAt(1)).setText(label);
        }
    }

    private void showSettingsPanel() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_profile_settings, null, false);

        MaterialSwitch switchApp = view.findViewById(R.id.switchAppNotifications);
        MaterialSwitch switchSound = view.findViewById(R.id.switchReminderSound);
        MaterialSwitch switchVibrate = view.findViewById(R.id.switchReminderVibrate);
        MaterialSwitch switchNews = view.findViewById(R.id.switchNews);
        MaterialButton btnChangePassword = view.findViewById(R.id.btnChangePasswordInline);
        MaterialSwitch switchConsent = view.findViewById(R.id.switchDataConsent);
        MaterialButton btnClose = view.findViewById(R.id.btnCloseSettings);
        ImageView btnCloseIcon = view.findViewById(R.id.btnCloseSettingsDialog);

        switchApp.setChecked(AppPreferences.isAppNotificationsEnabled(this));
        switchSound.setChecked(AppPreferences.isReminderSoundEnabled(this));
        switchVibrate.setChecked(AppPreferences.isReminderVibrateEnabled(this));
        switchNews.setChecked(AppPreferences.isNewsNotificationsEnabled(this));

        switchApp.setOnCheckedChangeListener((buttonView, checked) -> {
            AppPreferences.setAppNotificationsEnabled(this, checked);
            if (!checked) {
                switchSound.setChecked(false);
                switchVibrate.setChecked(false);
            } else {
                if (!switchSound.isChecked() && !switchVibrate.isChecked()) {
                    switchSound.setChecked(true);
                }
            }
            switchSound.setEnabled(checked);
            switchVibrate.setEnabled(checked);
        });

        switchSound.setEnabled(switchApp.isChecked());
        switchVibrate.setEnabled(switchApp.isChecked());

        switchSound.setOnCheckedChangeListener((buttonView, checked) ->
                AppPreferences.setReminderSoundEnabled(this, checked)
        );

        switchVibrate.setOnCheckedChangeListener((buttonView, checked) ->
                AppPreferences.setReminderVibrateEnabled(this, checked)
        );

        switchNews.setOnCheckedChangeListener((buttonView, checked) ->
                AppPreferences.setNewsNotificationsEnabled(this, checked)
        );
// Lấy trạng thái hiện tại từ máy lên giao diện
        switchConsent.setChecked(AppPreferences.isDataProcessingConsentGranted(this));

        switchConsent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                // Nếu gạt OFF -> Hiện Alert xác nhận rồi mới lưu false
                new AlertDialog.Builder(this)
                        .setTitle("Cảnh báo")
                        .setMessage("Bạn sẽ không thể sử dụng chức năng quét nếu tắt quyền này.")
                        .setPositiveButton("Tắt", (d, w) -> updateConsentOnFirebase(false))
                        .setNegativeButton("Hủy", (d, w) -> switchConsent.setChecked(true))
                        .setCancelable(false)
                        .show();
            } else {
                // Nếu gạt ON -> Lưu true luôn
                updateConsentOnFirebase(true);
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCloseIcon.setOnClickListener(v -> dialog.dismiss());
        btnChangePassword.setOnClickListener(v -> {
            dialog.dismiss();
            PageTransitionHelper.navigateWithLoading(
                    ProfileActivity.this,
                    new Intent(ProfileActivity.this, ChangePasswordActivity.class)
            );
        });
        dialog.show();
    }

    private void showPrivacyConsentDialog() {
        boolean granted = AppPreferences.isDataProcessingConsentGranted(this);
        String status = granted ? "Đang cho phép xử lý dữ liệu." : "Đã rút lại sự đồng ý xử lý dữ liệu.";

        String[] actions = granted
                ? new String[]{"Rút lại sự đồng ý"}
                : new String[]{"Đồng ý lại xử lý dữ liệu"};

        new AlertDialog.Builder(this)
                .setTitle("Quản lý quyền riêng tư")
                .setMessage(
                        status + "\n\n" +
                                "Theo yêu cầu pháp lý, bạn có thể rút lại hoặc cấp lại sự đồng ý bất cứ lúc nào."
                )
                .setItems(actions, (d, which) -> {
                    if (granted) {
                        confirmWithdrawConsent();
                    } else {
                        AppPreferences.setDataProcessingConsent(this, true);
                        Toast.makeText(this, "Đã ghi nhận đồng ý xử lý dữ liệu.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void confirmWithdrawConsent() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận rút lại sự đồng ý")
                .setMessage(
                        "Sau khi rút lại, ứng dụng sẽ ngừng xử lý và lưu dữ liệu mới cho chức năng scan. " +
                                "Bạn có thể bật lại bất cứ lúc nào trong mục quyền riêng tư."
                )
                .setPositiveButton("Rút lại", (d, w) -> {
                    AppPreferences.setDataProcessingConsent(this, false);
                    AppPreferences.setNewsNotificationsEnabled(this, false);
                    Toast.makeText(this, "Đã rút lại sự đồng ý xử lý dữ liệu.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showChangePasswordDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(currentUser.getEmail())) {
            Toast.makeText(this, "Không tìm thấy tài khoản đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(p, p, p, p);

        EditText edtCurrent = new EditText(this);
        edtCurrent.setHint("Mật khẩu hiện tại");
        edtCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(edtCurrent);

        EditText edtNew = new EditText(this);
        edtNew.setHint("Mật khẩu mới (ít nhất 6 ký tự)");
        edtNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtNew.setPadding(0, p / 2, 0, 0);
        container.addView(edtNew);

        EditText edtConfirm = new EditText(this);
        edtConfirm.setHint("Nhập lại mật khẩu mới");
        edtConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtConfirm.setPadding(0, p / 2, 0, 0);
        container.addView(edtConfirm);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(container)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    String currentPass = edtCurrent.getText() != null ? edtCurrent.getText().toString().trim() : "";
                    String newPass = edtNew.getText() != null ? edtNew.getText().toString().trim() : "";
                    String confirmPass = edtConfirm.getText() != null ? edtConfirm.getText().toString().trim() : "";

                    if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Mật khẩu mới tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String email = currentUser.getEmail();
                    AuthCredential credential = EmailAuthProvider.getCredential(email, currentPass);
                    currentUser.reauthenticate(credential)
                            .addOnSuccessListener(unused -> currentUser.updatePassword(newPass)
                                    .addOnSuccessListener(v -> Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Không thể đổi mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show())
                            )
                            .addOnFailureListener(e -> Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    private void showTermsDialog() {
        String terms = "Điều khoản dịch vụ (Tóm tắt)\n\n"
                + "1. Ứng dụng chỉ hỗ trợ sàng lọc ban đầu, không thay thế chẩn đoán y khoa.\n"
                + "2. Người dùng chịu trách nhiệm kiểm tra và đối chiếu thông tin với bác sĩ chuyên khoa.\n"
                + "3. Dữ liệu ảnh và hồ sơ được lưu để phục vụ theo dõi lịch sử quét.\n"
                + "4. Không sử dụng ứng dụng cho tình huống cấp cứu y tế.\n"
                + "5. Khi có dấu hiệu bất thường (đau, chảy máu, loét, lan rộng), cần đi khám sớm.\n\n"
                + "Bằng việc tiếp tục sử dụng, bạn đồng ý với các điều khoản trên.";

        new AlertDialog.Builder(this)
                .setTitle("Điều khoản dịch vụ")
                .setMessage(terms)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void showFeedbackDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập phản hồi của bạn...");
        input.setMinLines(4);
        input.setMaxLines(6);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("Gửi phản hồi")
                .setMessage("Ý kiến của bạn giúp ứng dụng cải thiện tốt hơn.")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (d, w) -> {
                    String message = input.getText() != null ? input.getText().toString().trim() : "";
                    if (message.isEmpty()) {
                        UiFeedback.showActionDialog(
                                this,
                                R.drawable.ic_status_warning,
                                getString(R.string.error_title_common),
                                getString(R.string.error_feedback_empty_message),
                                getString(R.string.feedback_understood),
                                null,
                                null,
                                null
                        );
                        return;
                    }
                    submitFeedback(message);
                })
                .show();
    }

    private void submitFeedback(String message) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    getString(R.string.error_title_common),
                    getString(R.string.error_need_login_message),
                    getString(R.string.feedback_understood),
                    null,
                    null,
                    null
            );
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(uid)
                .child("feedback")
                .push();

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("createdAt", System.currentTimeMillis());
        payload.put("source", "profile");

        ref.setValue(payload)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Đã gửi phản hồi, cảm ơn bạn!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        UiFeedback.showActionDialog(
                                this,
                                R.drawable.ic_status_warning,
                                getString(R.string.error_title_common),
                                getString(R.string.error_feedback_send_message),
                                getString(R.string.feedback_retry),
                                () -> submitFeedback(message),
                                getString(R.string.feedback_close),
                                null
                        )
                );
    }

    private void openGallery() {
        pickAvatarLauncher.launch("image/*");
    }

    private void handlePickedAvatar(Uri uri) {
        if (uri == null) return;

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Bitmap circleBitmap = cropCircle(bitmap);
            imgAvatar.setImageBitmap(circleBitmap);

            String base64 = bitmapToBase64(circleBitmap);
            profileRef.child("avatarBase64").setValue(base64)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Đổi avatar thành công", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            UiFeedback.showActionDialog(
                                    this,
                                    R.drawable.ic_status_warning,
                                    getString(R.string.error_title_common),
                                    getString(R.string.error_avatar_save_message),
                                    getString(R.string.feedback_retry),
                                    () -> handlePickedAvatar(uri),
                                    getString(R.string.feedback_close),
                                    null
                            )
                    );
        } catch (Exception e) {
            e.printStackTrace();
            UiFeedback.showActionDialog(
                    this,
                    R.drawable.ic_status_warning,
                    getString(R.string.error_title_common),
                    getString(R.string.error_avatar_pick_message),
                    getString(R.string.feedback_retry),
                    this::openGallery,
                    getString(R.string.feedback_close),
                    null
            );
        }
    }

    private void loadProfile() {
        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean firebaseConsent = snapshot.child("isConsent").getValue(Boolean.class);

                if (firebaseConsent != null) {
                    // Cập nhật lại vào máy để các màn hình khác (như Scan) đồng bộ theo
                    AppPreferences.setDataProcessingConsent(ProfileActivity.this, firebaseConsent);
                }
                if (!snapshot.exists()) {
                    txtName.setText("Chưa cập nhật");
                    txtSub.setText("Quản lý tài khoản và cài đặt cá nhân");
                    return;
                }

                UserProfile profile = snapshot.getValue(UserProfile.class);
                if (profile == null) return;

                String displayName = safeDecrypt(profile.displayName);

                if (!TextUtils.isEmpty(displayName)) {
                    txtName.setText(displayName);
                } else if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
                    txtName.setText(user.getDisplayName());
                } else {
                    txtName.setText(getFriendlyNameFromEmail(user != null ? user.getEmail() : null));
                }
                String ageText = profile.age > 0 ? profile.age + " tuổi" : null;
                String genderText = !TextUtils.isEmpty(profile.gender) ? profile.gender : null;
                StringBuilder sub = new StringBuilder();
                if (genderText != null) sub.append(genderText);
                if (ageText != null) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append(ageText);
                }

                if (sub.length() > 0) {
                    txtSub.setText(sub.toString());
                } else {
                    txtSub.setText("Quản lý tài khoản và cài đặt cá nhân");
                }

                if (!TextUtils.isEmpty(profile.avatarBase64)) {
                    Bitmap avatar = base64ToBitmap(profile.avatarBase64);
                    if (avatar != null) {
                        imgAvatar.setImageBitmap(cropCircle(avatar));
                    }
                }
                AppPreferences.setDataProcessingConsent(ProfileActivity.this, profile.isConsent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                UiFeedback.showActionDialog(
                        ProfileActivity.this,
                        R.drawable.ic_status_warning,
                        getString(R.string.error_title_common),
                        getString(R.string.error_profile_load_message),
                        getString(R.string.feedback_retry),
                        ProfileActivity.this::loadProfile,
                        getString(R.string.feedback_close),
                        null
                );
            }
        });
    }

    private Bitmap cropCircle(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        Bitmap squared = Bitmap.createBitmap(src,
                (src.getWidth() - size) / 2,
                (src.getHeight() - size) / 2,
                size,
                size);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);

        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFFFFFFFF);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(squared, rect, rect, paint);
        return output;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos); // 🔥 đổi JPEG -> PNG
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    private String getFriendlyNameFromEmail(String email) {
        if (email == null || email.trim().isEmpty()) return "Người dùng";
        int at = email.indexOf("@");
        if (at > 0) return email.substring(0, at);
        return email;
    }
    private Bitmap base64ToBitmap(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }
    private String safeDecrypt(String value) {
        if (TextUtils.isEmpty(value)) return "";

        String decrypted = DataCipher.decrypt(value);

        if (TextUtils.isEmpty(decrypted)) return "";

        // Nếu giải mã thất bại thì DataCipher đang trả lại nguyên chuỗi ENC::
        // Không được hiển thị chuỗi này lên giao diện.
        if (decrypted.startsWith("ENC::")) {
            return "";
        }

        return decrypted;
    }
}
