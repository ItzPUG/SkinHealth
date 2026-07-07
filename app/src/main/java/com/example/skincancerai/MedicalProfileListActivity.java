package com.example.skincancerai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MedicalProfileListActivity extends AppCompatActivity {

    private DatabaseReference ref;
    private final List<MedicalProfile> list = new ArrayList<>();
    private MedicalProfileAdapter adapter;

    private RecyclerView rvProfiles;
    private TextView txtProfileCount;
    private TextView txtProfileSummary;
    private LinearLayout layoutEmptyState;

    private LinearLayout navHome;
    private LinearLayout navHistory;
    private LinearLayout navHealth;
    private LinearLayout navProfile;
    private FloatingActionButton fabCamera;
    private FloatingActionButton fabAddProfile;
    private ValueEventListener profilesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_profile_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> PageTransitionHelper.finishWithAnimation(this));

        txtProfileCount = findViewById(R.id.txtProfileCount);
        txtProfileSummary = findViewById(R.id.txtProfileSummary);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        rvProfiles = findViewById(R.id.rvProfiles);

        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navHealth = findViewById(R.id.navHealth);
        navProfile = findViewById(R.id.navProfile);
        fabCamera = findViewById(R.id.fabCamera);
        fabAddProfile = findViewById(R.id.fabAddProfile);

        setupBottomNavActions();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.trim().isEmpty()) {
            PageTransitionHelper.navigateWithLoading(
                    this,
                    new Intent(this, LoginActivity.class),
                    true
            );
            return;
        }

        ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles");

        rvProfiles.setLayoutManager(new LinearLayoutManager(this));
        rvProfiles.setHasFixedSize(true);
        adapter = new MedicalProfileAdapter(list, this, ref);
        rvProfiles.setAdapter(adapter);

        if (fabAddProfile != null) {
            fabAddProfile.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, MedicalProfileEditActivity.class)
                    )
            );
        }

        findViewById(R.id.btnEmptyAdd).setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, MedicalProfileEditActivity.class)
                )
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachProfilesListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachProfilesListener();
    }

    private void setupBottomNavActions() {
        if (navHome != null) {
            navHome.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, MainActivity.class),
                            true
                    )
            );
        }

        if (navHistory != null) {
            navHistory.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, HistoryActivity.class),
                            true
                    )
            );
        }

        if (navHealth != null) {
            navHealth.setOnClickListener(v -> {
                // đang ở màn này
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, ProfileActivity.class),
                            true
                    )
            );
        }

        if (fabCamera != null) {
            fabCamera.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, ScanActivity.class)
                    )
            );
        }
    }

    private void attachProfilesListener() {
        if (ref == null || profilesListener != null) {
            updateHeaderAndState();
            return;
        }

        profilesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                list.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    MedicalProfile p = s.getValue(MedicalProfile.class);
                    if (p != null) {
                        if (p.id == null || p.id.trim().isEmpty()) {
                            p.id = s.getKey();
                        }
                        list.add(p);
                    }
                }

                adapter.notifyDataSetChanged();
                updateHeaderAndState();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                updateHeaderAndState();
            }
        };
        ref.addValueEventListener(profilesListener);
    }

    private void detachProfilesListener() {
        if (ref != null && profilesListener != null) {
            ref.removeEventListener(profilesListener);
            profilesListener = null;
        }
    }

    private void updateHeaderAndState() {
        int count = list.size();
        txtProfileCount.setText(String.valueOf(count));

        if (count == 0) {
            txtProfileSummary.setText("Bạn chưa có hồ sơ người dùng nào. Hãy tạo hồ sơ đầu tiên để bắt đầu quét và lưu lịch sử.");
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvProfiles.setVisibility(View.GONE);
        } else if (count == 1) {
            txtProfileSummary.setText("Bạn đang quản lý 1 hồ sơ người dùng để liên kết với các lần quét.");
            layoutEmptyState.setVisibility(View.GONE);
            rvProfiles.setVisibility(View.VISIBLE);
        } else {
            txtProfileSummary.setText("Bạn đang quản lý " + count + " hồ sơ người dùng để theo dõi từng lần quét riêng biệt.");
            layoutEmptyState.setVisibility(View.GONE);
            rvProfiles.setVisibility(View.VISIBLE);
        }
    }
}
