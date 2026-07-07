package com.example.skincancerai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtPassword;
    private MaterialButton btnLogin;
    private TextView txtCreate;
    private TextView txtForgot;
    private FirebaseAuth auth;

    // Giới hạn số lần đăng nhập
    private int loginAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtCreate = findViewById(R.id.txtCreate);
        txtForgot = findViewById(R.id.txtForgot);

        btnLogin.setOnClickListener(v -> login());

        txtCreate.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, RegisterActivity.class)
                ));

        txtForgot.setOnClickListener(v -> showForgotPasswordDialog());
        View btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            PageTransitionHelper.navigateWithLoading(
                    this,
                    new Intent(this, OnboardingAuthActivity.class),
                    true
            );
        });
    }

    private void login() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();

        // Reset lỗi
        edtEmail.setError(null);
        edtPassword.setError(null);

        if (email.isEmpty()) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        if (pass.length() < 6) {
            edtPassword.setError("Mật khẩu phải ≥ 6 ký tự");
            edtPassword.requestFocus();
            return;
        }

        // Nếu bị khóa do nhập sai nhiều lần
        if (loginAttempts >= MAX_ATTEMPTS) {
            Toast.makeText(this, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau!", Toast.LENGTH_LONG).show();
            return;
        }

        // Disable button tránh spam
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    loginAttempts = 0;

                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");

                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, MainActivity.class),
                            true
                    );
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");

                    loginAttempts++;
                    handleLoginError(e);
                });
    }

    private void handleLoginError(Exception e) {
        String email = edtEmail.getText().toString().trim();

        if (e instanceof FirebaseNetworkException) {
            Toast.makeText(
                    this,
                    "Lỗi mạng, vui lòng kiểm tra kết nối Internet",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            checkEmailExistsThenShowError(email);
            return;
        }

        Toast.makeText(
                this,
                "Đăng nhập thất bại, vui lòng thử lại",
                Toast.LENGTH_SHORT
        ).show();
    }
    private void checkEmailExistsThenShowError(String email) {
        auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    if (result.getSignInMethods() == null || result.getSignInMethods().isEmpty()) {
                        edtEmail.setError("Email chưa được đăng ký");
                        edtEmail.requestFocus();
                    } else {
                        edtPassword.setError("Sai mật khẩu");
                        edtPassword.requestFocus();
                    }
                })
                .addOnFailureListener(error -> {
                    edtPassword.setError("Sai email hoặc mật khẩu");
                    edtPassword.requestFocus();
                });
    }
    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_forgot_password, null, false);

        EditText edtForgotEmail = dialogView.findViewById(R.id.edtForgotEmail);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelForgot);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSendForgot);

        // Autofill email nếu đã nhập
        String currentEmail = edtEmail.getText() != null
                ? edtEmail.getText().toString().trim()
                : "";
        edtForgotEmail.setText(currentEmail);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String email = edtForgotEmail.getText() != null
                    ? edtForgotEmail.getText().toString().trim()
                    : "";

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtForgotEmail.setError("Email không hợp lệ");
                edtForgotEmail.requestFocus();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Đã gửi email đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Không thể gửi email: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        dialog.show();
    }
}
