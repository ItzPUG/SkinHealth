package com.example.skincancerai;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtCurrentPassword;
    private TextInputEditText edtNewPassword;
    private TextInputEditText edtConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtCurrentPassword = findViewById(R.id.edtCurrentPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        edtCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        findViewById(R.id.btnSavePassword).setOnClickListener(v -> changePassword());
    }

    private void focusInput(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
        editText.setSelection(editText.getText() == null ? 0 : editText.getText().length());
        editText.post(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void clearErrors() {
        edtCurrentPassword.setError(null);
        edtNewPassword.setError(null);
        edtConfirmPassword.setError(null);
    }

    private void changePassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tài khoản đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        clearErrors();

        String current = textOf(edtCurrentPassword);
        String newer = textOf(edtNewPassword);
        String confirm = textOf(edtConfirmPassword);

        if (current.isEmpty()) {
            focusInput(edtCurrentPassword, "Vui lòng nhập mật khẩu hiện tại");
            return;
        }

        if (newer.isEmpty()) {
            focusInput(edtNewPassword, "Vui lòng nhập mật khẩu mới");
            return;
        }

        if (confirm.isEmpty()) {
            focusInput(edtConfirmPassword, "Vui lòng nhập lại mật khẩu mới");
            return;
        }

        if (newer.length() < 6) {
            focusInput(edtNewPassword, "Mật khẩu mới tối thiểu 6 ký tự");
            return;
        }

        if (!newer.equals(confirm)) {
            focusInput(edtConfirmPassword, "Mật khẩu xác nhận không khớp");
            return;
        }

        if (current.equals(newer)) {
            focusInput(edtNewPassword, "Mật khẩu mới phải khác mật khẩu hiện tại");
            return;
        }

        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), current))
                .addOnSuccessListener(unused ->
                        user.updatePassword(newer)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Không thể đổi mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                )
                )
                .addOnFailureListener(e -> {
                    focusInput(edtCurrentPassword, "Mật khẩu hiện tại không đúng");
                    Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                });
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
