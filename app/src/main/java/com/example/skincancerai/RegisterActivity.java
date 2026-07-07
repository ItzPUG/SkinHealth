package com.example.skincancerai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName;
    private EditText edtEmail;
    private EditText edtPassword;
    private MaterialButton btnRegister;
    private TextView txtLogin;
    private FirebaseAuth auth;
    private CheckBox cbConsent;
    private static final String DB_URL =
            "https://skincancerai-6c951-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);
        cbConsent = findViewById(R.id.cbConsent);
        btnRegister.setOnClickListener(v -> register());

        txtLogin.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, LoginActivity.class),
                        true
                )
        );
        View btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            PageTransitionHelper.navigateWithLoading(
                    this,
                    new Intent(this, OnboardingAuthActivity.class),
                    true
            );
        });
    }

    private void register() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();

        // ===== VALIDATE =====
        if (TextUtils.isEmpty(name)) {
            edtName.setError("Vui lòng nhập họ tên");
            edtName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        if (pass.length() < 6) {
            edtPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            edtPassword.requestFocus();
            return;
        }
        if (!cbConsent.isChecked()) {
            Toast.makeText(this, "Bạn phải đồng ý chính sách dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }
        // ===== REGISTER =====
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {

                    if (result.getUser() == null) return;

                    String uid = result.getUser().getUid();

                    // 🔥 1. Lưu vào FirebaseAuth
                    UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();

                    result.getUser().updateProfile(req);

                    // 🔥 2. Lưu vào Realtime Database (QUAN TRỌNG)
                    DatabaseReference ref = FirebaseDatabase
                            .getInstance(DB_URL)
                            .getReference("users")
                            .child(uid)
                            .child("profile");

                    Map<String, Object> data = new HashMap<>();
                    data.put("displayName", DataCipher.encrypt(name));
                    data.put("email", DataCipher.encrypt(email));
                    data.put("createdAt", System.currentTimeMillis());

                    // ✅ CONSENT
                    data.put("isConsent", true);
                    data.put("consentAt", System.currentTimeMillis());
                    data.put("termsVersion", "v1.0");

                    ref.setValue(data)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();

                                PageTransitionHelper.navigateWithLoading(
                                        this,
                                        new Intent(this, LoginActivity.class),
                                        true
                                );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi lưu hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
