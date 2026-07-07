package com.example.skincancerai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LesionCaseDetailActivity extends AppCompatActivity {

    private TextView txtTitle;
    private TextView txtSubtitle;
    private TextView txtTrendStatus;
    private TextView txtTrendSummary;
    private TextView txtStats;
    private TextView txtEmptyTimeline;
    private RecyclerView recyclerTimeline;
    private MaterialButton btnScanFollowUp;

    private LesionTimelineAdapter adapter;

    private String profileId;
    private String profileName;
    private String caseId;

    private SkinLesionCase currentCase;
    private final List<SkinCheck> timeline = new ArrayList<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesion_case_detail);

        profileId = getIntent().getStringExtra("profileId");
        profileName = getIntent().getStringExtra("profileName");
        caseId = getIntent().getStringExtra("caseId");

        Toolbar toolbar = findViewById(R.id.toolbarLesionCaseDetail);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết theo dõi");
        }

        txtTitle = findViewById(R.id.txtLesionDetailTitle);
        txtSubtitle = findViewById(R.id.txtLesionDetailSubtitle);
        txtTrendStatus = findViewById(R.id.txtLesionTrendStatus);
        txtTrendSummary = findViewById(R.id.txtLesionTrendSummary);
        txtStats = findViewById(R.id.txtLesionStats);
        txtEmptyTimeline = findViewById(R.id.txtLesionTimelineEmpty);
        recyclerTimeline = findViewById(R.id.recyclerLesionTimeline);
        btnScanFollowUp = findViewById(R.id.btnLesionFollowUpScan);

        adapter = new LesionTimelineAdapter(this::openScanDetail);

        recyclerTimeline.setLayoutManager(new LinearLayoutManager(this));
        recyclerTimeline.setAdapter(adapter);

        btnScanFollowUp.setOnClickListener(v -> openFollowUpScan());

        loadCaseAndTimeline();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCaseAndTimeline();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadCaseAndTimeline() {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || isBlank(profileId) || isBlank(caseId)) {
            renderEmpty();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(profileId)
                .child("lesion_cases")
                .child(caseId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentCase = snapshot.getValue(SkinLesionCase.class);

                        if (currentCase != null) {
                            if (isBlank(currentCase.id)) {
                                currentCase.id = caseId;
                            }

                            if (isBlank(currentCase.profileId)) {
                                currentCase.profileId = profileId;
                            }

                            if (isBlank(currentCase.profileName)) {
                                currentCase.profileName = profileName;
                            }
                        }

                        loadTimeline();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Utils.toast(LesionCaseDetailActivity.this, "Không thể tải case theo dõi");
                        renderEmpty();
                    }
                });
    }

    private void loadTimeline() {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || isBlank(profileId) || isBlank(caseId)) {
            renderEmpty();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(profileId)
                .child("skin_checks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        timeline.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            SkinCheck check = child.getValue(SkinCheck.class);

                            if (check == null) {
                                continue;
                            }

                            if (isBlank(check.id)) {
                                check.id = child.getKey();
                            }

                            if (!caseId.equals(check.lesionCaseId)) {
                                continue;
                            }

                            check.resultLabel = TextSanitizer.normalizeResultLabel(check.resultLabel);
                            timeline.add(check);
                        }

                        Collections.sort(timeline, new Comparator<SkinCheck>() {
                            @Override
                            public int compare(SkinCheck o1, SkinCheck o2) {
                                return Long.compare(o1.createdAt, o2.createdAt);
                            }
                        });

                        render();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        timeline.clear();
                        render();
                    }
                });
    }

    private void render() {
        if (currentCase == null) {
            renderEmpty();
            return;
        }

        txtTitle.setText(nonBlank(currentCase.title, "Vùng da chưa đặt tên"));

        String subtitle = nonBlank(currentCase.profileName, nonBlank(profileName, "Không rõ hồ sơ"));

        if (!isBlank(currentCase.bodyPart)) {
            subtitle += " • " + TextSanitizer.sanitize(currentCase.bodyPart);
        }

        txtSubtitle.setText(subtitle);

        adapter.setItems(timeline);

        boolean empty = timeline.isEmpty();

        txtEmptyTimeline.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerTimeline.setVisibility(empty ? View.GONE : View.VISIBLE);

        txtStats.setText(buildStatsText());
        updateTrend();
    }

    private void renderEmpty() {
        adapter.setItems(new ArrayList<>());

        txtTitle.setText("Chưa có case theo dõi");
        txtSubtitle.setText("Không tìm thấy dữ liệu cho case này.");
        txtStats.setText("0 lần quét");
        txtTrendStatus.setText("Chưa có dữ liệu");
        txtTrendSummary.setText("Hãy gắn ít nhất một kết quả quét vào case theo dõi.");
        txtEmptyTimeline.setVisibility(View.VISIBLE);
        recyclerTimeline.setVisibility(View.GONE);
    }

    private String buildStatsText() {
        if (timeline.isEmpty()) {
            return "0 lần quét";
        }

        SkinCheck first = timeline.get(0);
        SkinCheck last = timeline.get(timeline.size() - 1);

        return timeline.size() + " lần quét • từ "
                + dateFormat.format(new Date(first.createdAt))
                + " đến "
                + dateFormat.format(new Date(last.createdAt));
    }

    private void updateTrend() {
        if (timeline.isEmpty()) {
            txtTrendStatus.setText("Chưa đủ dữ liệu");
            txtTrendSummary.setText("Case này chưa có mốc quét nào để phân tích xu hướng.");
            txtTrendStatus.setTextColor(Color.parseColor("#475569"));
            return;
        }

        if (timeline.size() == 1) {
            SkinCheck only = timeline.get(0);

            txtTrendStatus.setText("Mốc nền ban đầu");
            txtTrendSummary.setText(
                    "Đã có mốc đầu tiên: "
                            + TextSanitizer.normalizeResultLabel(only.resultLabel)
                            + ". Hãy tái kiểm tra để thấy xu hướng thay đổi."
            );
            txtTrendStatus.setTextColor(Color.parseColor("#2563EB"));
            return;
        }

        SkinCheck previous = timeline.get(timeline.size() - 2);
        SkinCheck current = timeline.get(timeline.size() - 1);

        int prevRisk = TextSanitizer.riskLevel(previous.resultLabel);
        int curRisk = TextSanitizer.riskLevel(current.resultLabel);

        if (curRisk > prevRisk) {
            txtTrendStatus.setText("Xu hướng tăng nguy cơ");
            txtTrendSummary.setText(
                    "Mốc mới nhất có mức nguy cơ cao hơn mốc trước. " +
                            "Nên theo dõi sát và cân nhắc đi khám nếu có thay đổi thực tế."
            );
            txtTrendStatus.setTextColor(Color.parseColor("#DC2626"));
        } else if (curRisk < prevRisk) {
            txtTrendStatus.setText("Xu hướng cải thiện");
            txtTrendSummary.setText(
                    "Mốc mới nhất có mức nguy cơ thấp hơn mốc trước. " +
                            "Vẫn nên tiếp tục theo dõi định kỳ."
            );
            txtTrendStatus.setTextColor(Color.parseColor("#16A34A"));
        } else if (curRisk == TextSanitizer.RISK_HIGH) {
            txtTrendStatus.setText("Vẫn ở nhóm nguy cơ cao");
            txtTrendSummary.setText(
                    "Các mốc gần đây vẫn nằm trong nhóm nguy cơ cao. " +
                            "App chỉ hỗ trợ sàng lọc, không thay thế bác sĩ."
            );
            txtTrendStatus.setTextColor(Color.parseColor("#DC2626"));
        } else if (curRisk == TextSanitizer.RISK_MEDIUM) {
            txtTrendStatus.setText("Cần tiếp tục theo dõi sát");
            txtTrendSummary.setText("Các mốc gần đây duy trì ở nhóm nguy cơ trung bình.");
            txtTrendStatus.setTextColor(Color.parseColor("#D97706"));
        } else {
            txtTrendStatus.setText("Xu hướng ổn định");
            txtTrendSummary.setText("Các mốc gần đây đang ổn định ở nhóm nguy cơ thấp.");
            txtTrendStatus.setTextColor(Color.parseColor("#2563EB"));
        }
    }

    private void openFollowUpScan() {
        Intent intent = new Intent(this, ScanActivity.class);

        intent.putExtra("profileId", profileId);
        intent.putExtra("trackingCaseId", caseId);
        intent.putExtra("trackingCaseTitle", currentCase != null ? currentCase.title : "");
        intent.putExtra("followUpMode", true);

        if (!timeline.isEmpty()) {
            SkinCheck latest = timeline.get(timeline.size() - 1);
            intent.putExtra("followUpSourceCheckId", latest.id);
        }

        PageTransitionHelper.navigateWithLoading(this, intent);
    }

    private void openScanDetail(SkinCheck check) {
        if (check == null) {
            return;
        }

        Intent intent = new Intent(this, HistoryDetailActivity.class);

        intent.putExtra("profileId", profileId);
        intent.putExtra("profileName", nonBlank(profileName, currentCase != null ? currentCase.profileName : ""));
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

    private String nonBlank(String value, String fallback) {
        String clean = TextSanitizer.sanitize(value);
        return clean.trim().isEmpty() ? fallback : clean;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
