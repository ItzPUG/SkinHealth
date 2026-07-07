package com.example.skincancerai;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MedicalProfileEditActivity extends AppCompatActivity {

    private EditText edtName;
    private EditText etDateOfBirth;
    private EditText edtHistory;
    private EditText edtNote;
    private MaterialAutoCompleteTextView edtGender;

    private TextView txtHeaderTitle;
    private TextView txtHeaderSub;

    private DatabaseReference ref;
    private String profileId;
    private boolean isEditMode;

    private final Calendar selectedDob = Calendar.getInstance();
    private final SimpleDateFormat dobFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        profileId = getIntent().getStringExtra("profileId");
        isEditMode = profileId != null && !profileId.trim().isEmpty();

        // Cách 1: cùng 1 Activity, đổi layout theo mode
        setContentView(isEditMode
                ? R.layout.activity_medical_profile_update
                : R.layout.activity_medical_profile_edit);

        bindViews();
        setupToolbar();
        setupHeader();
        setupGenderDropdown();
        setupDobPicker();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles");

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        if (isEditMode) {
            loadProfile(profileId);
        }
    }

    private void bindViews() {
        edtName = findViewById(R.id.edtName);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        edtGender = findViewById(R.id.edtGender);
        edtHistory = findViewById(R.id.edtHistory);
        edtNote = findViewById(R.id.edtNote);

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        txtHeaderSub = findViewById(R.id.txtHeaderSub);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupHeader() {
        if (txtHeaderTitle == null || txtHeaderSub == null) return;

        if (isEditMode) {
            txtHeaderTitle.setText("Cập nhật hồ sơ hiện có");
            txtHeaderSub.setText("Bạn có thể chỉnh lại thông tin cơ bản, ngày sinh, giới tính và ghi chú để hồ sơ luôn chính xác trong các lần quét sau.");
        } else {
            txtHeaderTitle.setText("Tạo hồ sơ mới");
            txtHeaderSub.setText("Điền thông tin cơ bản để dùng hồ sơ này cho scan, lịch sử và theo dõi về sau.");
        }
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

            // Không cho ngày sinh vượt quá hiện tại
            if (dob.after(today)) {
                return null;
            }

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH)
                    || (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            // Trẻ em vẫn hợp lệ, kể cả tuổi 0
            return Math.max(age, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadProfile(String profileId) {
        ref.child(profileId).get().addOnSuccessListener(snapshot -> {
            MedicalProfile p = snapshot.getValue(MedicalProfile.class);
            if (p == null) return;

            if (p.fullName != null) edtName.setText(p.fullName);
            if (p.dateOfBirth != null && !p.dateOfBirth.trim().isEmpty()) {
                etDateOfBirth.setText(p.dateOfBirth);
            }
            if (p.gender != null && !p.gender.trim().isEmpty()) {
                edtGender.setText(p.gender, false);
            }
            if (p.skinHistory != null) edtHistory.setText(p.skinHistory);
            if (p.note != null) edtNote.setText(p.note);
        });
    }

    private void save() {
        String name = textOf(edtName);
        String dob = textOf(etDateOfBirth);
        String gender = textOf(edtGender);
        String history = textOf(edtHistory);
        String note = textOf(edtNote);

        if (name.isEmpty()) {
            edtName.setError("Vui lòng nhập họ tên");
            edtName.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            etDateOfBirth.setError("Vui lòng chọn ngày sinh");
            etDateOfBirth.requestFocus();
            openDobPicker();
            return;
        }

        Integer age = calculateAgeFromDob(dob);
        if (age == null || age > 120) {
            etDateOfBirth.setError("Ngày sinh không được lớn hơn ngày hiện tại");
            etDateOfBirth.requestFocus();
            return;
        }

        if (!gender.equals("Nam") && !gender.equals("Nữ")) {
            edtGender.setError("Vui lòng chọn giới tính");
            edtGender.requestFocus();
            edtGender.showDropDown();
            return;
        }

        if (profileId == null || profileId.trim().isEmpty()) {
            profileId = ref.push().getKey();
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("id", profileId);
        updates.put("fullName", name);
        updates.put("dateOfBirth", dob);
        updates.put("age", age);
        updates.put("gender", gender);
        updates.put("skinHistory", history);
        updates.put("note", note);

        ref.child(profileId).updateChildren(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(
                            this,
                            isEditMode ? "Đã cập nhật hồ sơ" : "Đã lưu hồ sơ",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi lưu dữ liệu", Toast.LENGTH_SHORT).show()
                );
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String textOf(MaterialAutoCompleteTextView editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
