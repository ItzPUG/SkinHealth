package com.example.skincancerai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        View itemSupport = findViewById(R.id.itemSupport);
        View itemAbout = findViewById(R.id.itemAbout);
        View itemChangePassword = findViewById(R.id.itemChangePassword);

        itemSupport.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:support@skincancerai.app"));
            i.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ SkinCancer");
            startActivity(i);
        });

        itemAbout.setOnClickListener(v ->
                Toast.makeText(this, "SkinCancer v1.0", Toast.LENGTH_SHORT).show()
        );

        itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
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

                    String email = user.getEmail();
                    AuthCredential credential = EmailAuthProvider.getCredential(email, currentPass);
                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused ->
                                    user.updatePassword(newPass)
                                            .addOnSuccessListener(v ->
                                                    Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Không thể đổi mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            )
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show()
                            );
                })
                .show();
    }
}
