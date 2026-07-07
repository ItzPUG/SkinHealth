package com.example.skincancerai;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PersonalInfoActivity extends AppCompatActivity {

    private TextInputEditText edtDisplayName;
    private EditText etDateOfBirth;
    private MaterialAutoCompleteTextView edtGender;
    private TextInputEditText edtPhone;
    private TextInputEditText edtEmail;

    private DatabaseReference profileRef;
    private FirebaseUser currentUser;

    private final Calendar selectedDob = Calendar.getInstance();
    private final SimpleDateFormat dobFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private String currentAvatarBase64 = null;
    private String currentSkinType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            toast("Không tìm thấy tài khoản đăng nhập");
            finish();
            return;
        }

        profileRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid())
                .child("profile");

        setupGenderDropdown();
        setupDobPicker();

        edtEmail.setText(currentUser.getEmail());

        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void bindViews() {
        edtDisplayName = findViewById(R.id.edtDisplayName);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        edtGender = findViewById(R.id.edtGender);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
    }

    private void setupGenderDropdown() {
        String[] genders = {"Nam", "Nữ"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                genders
        );

        edtGender.setAdapter(adapter);
        edtGender.setInputType(InputType.TYPE_NULL);
        edtGender.setKeyListener(null);

        edtGender.setOnClickListener(v -> edtGender.showDropDown());
        edtGender.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) edtGender.showDropDown();
        });
    }

    private void setupDobPicker() {
        etDateOfBirth.setInputType(InputType.TYPE_NULL);
        etDateOfBirth.setKeyListener(null);

        etDateOfBirth.setOnClickListener(v -> openDobPicker());
        etDateOfBirth.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) openDobPicker();
        });
    }

    private void openDobPicker() {
        Calendar initial = Calendar.getInstance();

        String currentDob = textOf(etDateOfBirth);
        if (!currentDob.isEmpty()) {
            try {
                java.util.Date parsed = dobFormat.parse(currentDob);
                if (parsed != null) {
                    initial.setTime(parsed);
                }
            } catch (Exception ignored) {
            }
        } else {
            initial.set(Calendar.YEAR, 2000);
            initial.set(Calendar.MONTH, Calendar.JANUARY);
            initial.set(Calendar.DAY_OF_MONTH, 1);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDob.set(Calendar.YEAR, year);
                    selectedDob.set(Calendar.MONTH, month);
                    selectedDob.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    etDateOfBirth.setText(dobFormat.format(selectedDob.getTime()));
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private Integer calculateAgeFromDob(String dobText) {
        try {
            java.util.Date date = dobFormat.parse(dobText);
            if (date == null) return null;

            Calendar dob = Calendar.getInstance();
            dob.setTime(date);

            Calendar today = Calendar.getInstance();

            // Không cho ngày sinh lớn hơn hiện tại
            if (dob.after(today)) {
                return null;
            }

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH)
                    || (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            // Cho phép tuổi = 0
            return Math.max(age, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadProfile() {
        profileRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                if (textOf(edtDisplayName).isEmpty()) {
                    edtDisplayName.setText(getFriendlyNameFromEmail(currentUser.getEmail()));
                }
                return;
            }

            UserProfile profile = snapshot.getValue(UserProfile.class);
            if (profile == null) return;

            String displayName = safeDecrypt(profile.displayName);
            if (displayName != null && !displayName.trim().isEmpty()) {
                edtDisplayName.setText(displayName);
            } else if (currentUser != null && !TextUtils.isEmpty(currentUser.getDisplayName())) {
                edtDisplayName.setText(currentUser.getDisplayName());
            } else {
                edtDisplayName.setText(getFriendlyNameFromEmail(currentUser.getEmail()));
            }

            String dob = safeDecrypt(profile.dateOfBirth);
            if (dob != null && !dob.trim().isEmpty()) {
                etDateOfBirth.setText(dob);
            }

            if (profile.gender != null && !profile.gender.trim().isEmpty()) {
                edtGender.setText(profile.gender, false);
            }

            String phone = safeDecrypt(profile.phoneNumber);
            if (phone != null) {
                edtPhone.setText(phone);
            }

            String email = safeDecrypt(profile.email);
            if (email != null && !email.trim().isEmpty()) {
                edtEmail.setText(email);
            } else {
                edtEmail.setText(currentUser.getEmail());
            }

            currentAvatarBase64 = profile.avatarBase64;
            currentSkinType = profile.skinType;
        }).addOnFailureListener(e ->
                toast("Không thể tải thông tin cá nhân")
        );
    }

    private void saveProfile() {
        String displayName = textOf(edtDisplayName);
        String dob = textOf(etDateOfBirth);
        String gender = textOf(edtGender);
        String phone = textOf(edtPhone);
        String email = textOf(edtEmail);

        if (displayName.isEmpty()) {
            toast("Vui lòng nhập họ và tên");
            edtDisplayName.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            toast("Vui lòng chọn ngày sinh");
            etDateOfBirth.requestFocus();
            openDobPicker();
            return;
        }

        Integer age = calculateAgeFromDob(dob);
        if (age == null || age > 120) {
            toast("Ngày sinh không được lớn hơn ngày hiện tại");
            etDateOfBirth.requestFocus();
            return;
        }

        if (!gender.equals("Nam") && !gender.equals("Nữ")) {
            toast("Vui lòng chọn giới tính");
            edtGender.requestFocus();
            edtGender.showDropDown();
            return;
        }

        UserProfile profile = new UserProfile();
        profile.displayName = DataCipher.encrypt(displayName);
        profile.dateOfBirth = DataCipher.encrypt(dob);
        profile.age = age;
        profile.gender = gender;
        profile.phoneNumber = DataCipher.encrypt(phone);
        profile.email = DataCipher.encrypt(email);
        profile.skinType = currentSkinType;
        profile.avatarBase64 = currentAvatarBase64;

        profileRef.setValue(profile)
                .addOnSuccessListener(unused -> {
                    toast("Đã lưu thông tin");
                    finish();
                })
                .addOnFailureListener(e ->
                        toast("Lưu thông tin thất bại")
                );
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String textOf(MaterialAutoCompleteTextView editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String getFriendlyNameFromEmail(String email) {
        if (email == null || email.trim().isEmpty()) return "Người dùng";
        int at = email.indexOf("@");
        if (at > 0) return email.substring(0, at);
        return email;
    }
    private String safeDecrypt(String value) {
        if (TextUtils.isEmpty(value)) return "";

        String decrypted = DataCipher.decrypt(value);

        if (TextUtils.isEmpty(decrypted)) return "";

        if (decrypted.startsWith("ENC::")) {
            return "";
        }

        return decrypted;
    }
}
