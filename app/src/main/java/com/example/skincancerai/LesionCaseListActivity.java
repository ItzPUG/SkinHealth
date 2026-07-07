package com.example.skincancerai;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import java.util.HashMap;
import java.util.Map;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LesionCaseListActivity extends AppCompatActivity {

    private RecyclerView recyclerCases;
    private LinearLayout layoutEmptyState;
    private TextView txtCaseCount;
    private FloatingActionButton fabScan;

    private LesionCaseAdapter adapter;
    private final List<SkinLesionCase> cases = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesion_case_list);

        Toolbar toolbar = findViewById(R.id.toolbarLesionCases);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Vùng tái kiểm tra");
        }

        recyclerCases = findViewById(R.id.recyclerLesionCases);
        layoutEmptyState = findViewById(R.id.layoutLesionEmpty);
        txtCaseCount = findViewById(R.id.txtLesionCaseCount);
        fabScan = findViewById(R.id.fabNewScanFromCases);

        adapter = new LesionCaseAdapter(this::openCaseDetail, this::confirmDeleteCase);

        recyclerCases.setLayoutManager(new LinearLayoutManager(this));
        recyclerCases.setAdapter(adapter);

        fabScan.setOnClickListener(v -> PageTransitionHelper.navigateWithLoading(
                this,
                new Intent(this, ScanActivity.class)
        ));

        loadCases();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCases();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadCases() {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            updateUi();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cases.clear();

                        for (DataSnapshot profileSnap : snapshot.getChildren()) {
                            String profileId = profileSnap.getKey();

                            MedicalProfile profile = profileSnap.getValue(MedicalProfile.class);
                            String profileName = profile != null
                                    ? TextSanitizer.sanitize(profile.fullName)
                                    : "";

                            if (profileName.trim().isEmpty()) {
                                profileName = "Hồ sơ chưa đặt tên";
                            }

                            for (DataSnapshot caseSnap : profileSnap.child("lesion_cases").getChildren()) {
                                SkinLesionCase item = caseSnap.getValue(SkinLesionCase.class);

                                if (item == null) {
                                    continue;
                                }

                                if (isBlank(item.id)) {
                                    item.id = caseSnap.getKey();
                                }

                                if (isBlank(item.profileId)) {
                                    item.profileId = profileId;
                                }

                                if (isBlank(item.profileName)) {
                                    item.profileName = profileName;
                                }

                                item.title = TextSanitizer.sanitize(item.title);
                                item.bodyPart = TextSanitizer.sanitize(item.bodyPart);
                                item.latestRiskLabel = TextSanitizer.normalizeResultLabel(item.latestRiskLabel);

                                cases.add(item);
                            }
                        }

                        Collections.sort(cases, new Comparator<SkinLesionCase>() {
                            @Override
                            public int compare(SkinLesionCase o1, SkinLesionCase o2) {
                                long t1 = o1 != null ? Math.max(o1.updatedAt, o1.lastScanAt) : 0L;
                                long t2 = o2 != null ? Math.max(o2.updatedAt, o2.lastScanAt) : 0L;
                                return Long.compare(t2, t1);
                            }
                        });

                        updateUi();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Utils.toast(LesionCaseListActivity.this, "Không thể tải danh sách theo dõi");
                        updateUi();
                    }
                });
    }

    private void updateUi() {
        adapter.setItems(cases);

        boolean empty = cases.isEmpty();

        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerCases.setVisibility(empty ? View.GONE : View.VISIBLE);

        txtCaseCount.setText(cases.size() + " case đang theo dõi");
    }

    private void openCaseDetail(SkinLesionCase item) {
        if (item == null || isBlank(item.id) || isBlank(item.profileId)) {
            return;
        }

        Intent intent = new Intent(this, LesionCaseDetailActivity.class);
        intent.putExtra("profileId", item.profileId);
        intent.putExtra("profileName", TextSanitizer.sanitize(item.profileName));
        intent.putExtra("caseId", item.id);

        PageTransitionHelper.navigateWithLoading(this, intent);
    }
    private void confirmDeleteCase(SkinLesionCase item) {
        if (item == null || isBlank(item.id) || isBlank(item.profileId)) {
            Utils.toast(this, "Không tìm thấy hồ sơ theo dõi để xóa");
            return;
        }

        String title = TextSanitizer.sanitize(item.title);

        if (title.trim().isEmpty()) {
            title = "hồ sơ theo dõi này";
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa hồ sơ theo dõi?")
                .setMessage(
                        "Bạn có chắc muốn xóa \"" + title + "\"?\n\n" +
                                "Lịch sử quét cũ vẫn được giữ lại, nhưng sẽ không còn nằm trong timeline theo dõi này."
                )
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteCaseFromList(item))
                .show();
    }

    private void deleteCaseFromList(SkinLesionCase item) {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || item == null || isBlank(item.id) || isBlank(item.profileId)) {
            Utils.toast(this, "Không thể xóa hồ sơ theo dõi");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(item.profileId)
                .child("skin_checks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> updates = new HashMap<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        SkinCheck check = child.getValue(SkinCheck.class);

                        if (check == null) {
                            continue;
                        }

                        if (item.id.equals(check.lesionCaseId)) {
                            updates.put(child.getKey() + "/lesionCaseId", "");
                            updates.put(child.getKey() + "/lesionCaseTitle", "");
                        }
                    }

                    if (updates.isEmpty()) {
                        removeCaseNode(uid, item);
                        return;
                    }

                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .child("medical_profiles")
                            .child(item.profileId)
                            .child("skin_checks")
                            .updateChildren(updates)
                            .addOnSuccessListener(unused -> removeCaseNode(uid, item))
                            .addOnFailureListener(e ->
                                    Utils.toast(this, "Không thể gỡ liên kết lịch sử quét"));
                })
                .addOnFailureListener(e ->
                        Utils.toast(this, "Không thể tải lịch sử quét"));
    }

    private void removeCaseNode(String uid, SkinLesionCase item) {
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(item.profileId)
                .child("lesion_cases")
                .child(item.id)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Utils.toast(this, "Đã xóa hồ sơ theo dõi");
                    loadCases();
                })
                .addOnFailureListener(e ->
                        Utils.toast(this, "Xóa hồ sơ theo dõi thất bại"));
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
