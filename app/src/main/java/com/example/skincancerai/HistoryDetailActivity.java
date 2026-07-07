package com.example.skincancerai;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;


import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryDetailActivity extends AppCompatActivity {

    private static final int LOW = 0;
    private static final int WATCH = 1;
    private static final int HIGH = 2;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final int REQ_POST_NOTIFICATIONS = 9001;
    private static final int REQ_EXACT_ALARM_SETTINGS = 9002;
    private static final int RISK_LOW = 0;
    private static final int RISK_MEDIUM = 1;
    private static final int RISK_HIGH = 2;

    private ImageView imgResult;
    private TextView txtResultDetail, txtConfidenceDetail, txtDateDetail, txtPercentCircle, txtResultSub;
    private TextView txtRiskExplain, txtDetailInfo, txtConfidenceLevel, txtRiskTag, txtNextAction;
    private TextView txtStickyStatus, txtStickyTitle, txtStickySub;
    private TextView txtTimelineStatus, txtTimelineSummary, txtNoteValue, txtReminderValue, txtToggleDetails;
    private LinearLayout layoutTimelineNodes;
    private MaterialButton btnPrimaryAction, btnCompareScan, btnCompareNearest, btnEditNote, btnSetReminder, btnFollowUpScan, btnAttachCase;
    private MaterialCardView cardPrediction, cardStickySummary;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final List<SkinCheck> timelineChecks = new ArrayList<>();

    private String currentProfileId, currentProfileName, currentCheckId, currentResultLabel, currentImageBase64;
    private String currentLesionCaseId = "", currentLesionCaseTitle = "";
    private String currentNote = "", currentBodyPart = "";
    private float currentConfidence;
    private long currentCreatedAt;
    private long currentReminderAt;
    private boolean currentReminderEnabled, currentIsFollowUp, detailsExpanded;
    private boolean refreshAfterAction;
    private int currentReminderDays = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết kết quả");
        }

        bindViews();
        bindActions();
        bindDataFromIntent();
        renderScreen();
        loadLatestCheckData();
        loadTimelineData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!refreshAfterAction) return;
        refreshAfterAction = false;
        loadLatestCheckData();
        loadTimelineData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void bindViews() {
        imgResult = findViewById(R.id.imgResult);
        txtResultDetail = findViewById(R.id.txtResultDetail);
        txtConfidenceDetail = findViewById(R.id.txtConfidenceDetail);
        txtDateDetail = findViewById(R.id.txtDateDetail);
        txtPercentCircle = findViewById(R.id.txtPercentCircle);
        txtResultSub = findViewById(R.id.txtResultSub);
        txtRiskExplain = findViewById(R.id.txtRiskExplain);
        txtDetailInfo = findViewById(R.id.txtDetailInfo);
        txtConfidenceLevel = findViewById(R.id.txtConfidenceLevel);
        txtRiskTag = findViewById(R.id.txtRiskTag);
        txtNextAction = findViewById(R.id.txtNextAction);
        txtStickyStatus = findViewById(R.id.txtStickyStatus);
        txtStickyTitle = findViewById(R.id.txtStickyTitle);
        txtStickySub = findViewById(R.id.txtStickySub);
        txtTimelineStatus = findViewById(R.id.txtTimelineStatus);
        txtTimelineSummary = findViewById(R.id.txtTimelineSummary);
        layoutTimelineNodes = findViewById(R.id.layoutTimelineNodes);
        txtNoteValue = findViewById(R.id.txtNoteValue);
        txtReminderValue = findViewById(R.id.txtReminderValue);
        txtToggleDetails = findViewById(R.id.txtToggleDetails);
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnCompareScan = findViewById(R.id.btnCompareScan);
        btnCompareNearest = findViewById(R.id.btnCompareNearest);
        btnEditNote = findViewById(R.id.btnEditNote);
        btnSetReminder = findViewById(R.id.btnSetReminder);
        btnFollowUpScan = findViewById(R.id.btnFollowUpScan);
        btnAttachCase = findViewById(R.id.btnAttachCase);
        cardPrediction = findViewById(R.id.cardPrediction);
        cardStickySummary = findViewById(R.id.cardStickySummary);
    }

    private void bindActions() {
        btnPrimaryAction.setOnClickListener(v -> runPrimaryAction());
        btnCompareScan.setOnClickListener(v -> loadOtherScansForCompare());
        btnCompareNearest.setOnClickListener(v -> openCompareWithNearestPrevious());
        btnEditNote.setOnClickListener(v -> showEditNoteDialog());
        btnSetReminder.setOnClickListener(v -> showReminderDialog());
        if (btnFollowUpScan != null) {
            btnFollowUpScan.setVisibility(View.GONE);
            btnFollowUpScan.setOnClickListener(v -> openFollowUpScan());
        }
        if (btnAttachCase != null) {
            btnAttachCase.setOnClickListener(v -> {
                if (!isBlank(currentLesionCaseId)) {
                    openCurrentLesionCase();
                } else {
                    showAttachCaseDialog();
                }
            });
        }
        txtToggleDetails.setOnClickListener(v -> {
            detailsExpanded = !detailsExpanded;
            txtDetailInfo.setVisibility(detailsExpanded ? View.VISIBLE : View.GONE);
            txtToggleDetails.setText(detailsExpanded ? "Thu gọn thông tin kỹ thuật" : "Xem thêm thông tin kỹ thuật");
        });
    }

    private void bindDataFromIntent() {
        currentProfileId = getIntent().getStringExtra("profileId");
        currentProfileName = getIntent().getStringExtra("profileName");
        currentCheckId = getIntent().getStringExtra("checkId");
        currentResultLabel = getIntent().getStringExtra("resultLabel");
        currentConfidence = getIntent().getFloatExtra("confidence", 0f);
        currentCreatedAt = getIntent().getLongExtra("createdAt", 0L);
        currentImageBase64 = getIntent().getStringExtra("imageBase64");
        currentNote = safe(getIntent().getStringExtra("note"));
        currentReminderEnabled = getIntent().getBooleanExtra("reminderEnabled", false);
        currentReminderDays = getIntent().getIntExtra("reminderDays", 7);
        currentReminderAt = getIntent().getLongExtra("reminderAt", 0L);
        if (currentReminderAt <= 0L && currentReminderEnabled) {
            currentReminderAt = computeLegacyReminderAt(currentCreatedAt, currentReminderDays);
        }
        currentIsFollowUp = getIntent().getBooleanExtra("isFollowUp", false);
        currentBodyPart = safe(getIntent().getStringExtra("bodyPart"));
        currentLesionCaseId = safe(getIntent().getStringExtra("lesionCaseId"));
        currentLesionCaseTitle = safe(getIntent().getStringExtra("lesionCaseTitle"));
        if (isBlank(currentResultLabel)) currentResultLabel = "Không rõ kết luận";
    }

    private void renderScreen() {
        currentResultLabel = normalizeRiskLabel(currentResultLabel);

        int state = riskState(currentResultLabel, currentConfidence);
        float percent = clamp(currentConfidence * 100f);

        txtResultDetail.setText(currentResultLabel);
        txtPercentCircle.setText(String.format(Locale.getDefault(), "%.1f%%", percent));
        txtConfidenceLevel.setText("Mức chắc chắn: " + confidenceLevel(currentConfidence));
        txtRiskTag.setText(riskTag(currentResultLabel, currentConfidence));
        txtResultSub.setText(resultSubtitle(currentResultLabel, currentConfidence));
        txtConfidenceDetail.setText("Độ tin cậy mô hình: " + String.format(Locale.getDefault(), "%.1f%%", percent));
        txtDateDetail.setText(dateFormat.format(new Date(currentCreatedAt > 0 ? currentCreatedAt : System.currentTimeMillis())));
        txtRiskExplain.setText(resultExplain(currentResultLabel, currentConfidence));
        txtNextAction.setText(nextAction(currentResultLabel, currentConfidence));
        txtNoteValue.setText(isBlank(currentNote) ? "Ghi chú: Chưa có ghi chú cho lần quét này." : "Ghi chú: " + currentNote);
        if (btnAttachCase != null) {
            btnAttachCase.setText(isBlank(currentLesionCaseId) ? "Tái kiểm tra" : "Mở hồ sơ tái kiểm tra");
        }

        if (currentReminderEnabled && currentReminderAt > 0L) {
            txtReminderValue.setText("Nhắc tái quét: " + dateFormat.format(new Date(currentReminderAt)));
        } else {
            txtReminderValue.setText("Nhắc tái quét: Chưa bật.");
        }

        applyStateStyle(state);
        updateSticky(state);
        updatePrimaryAction(state);
        updateImage();
        updateInfo();
    }

    private void applyStateStyle(int state) {
        int bg;
        int text;

        if (state == HIGH) {
            bg = Color.parseColor("#FFF1F2");
            text = Color.parseColor("#BE123C");
        } else if (state == WATCH) {
            bg = Color.parseColor("#FFF7ED");
            text = Color.parseColor("#C2410C");
        } else {
            bg = Color.parseColor("#EEF7FF");
            text = Color.parseColor("#1D4ED8");
        }

        cardPrediction.setCardBackgroundColor(bg);
        cardStickySummary.setCardBackgroundColor(bg);
        txtResultDetail.setTextColor(text);
        txtConfidenceLevel.setTextColor(text);
        txtPercentCircle.setTextColor(text);
    }

    private void updateSticky(int state) {
        float p = clamp(currentConfidence * 100f);
        int risk = riskLevel(currentResultLabel);

        if (p < 55f) {
            txtStickyStatus.setText("Cần quét lại");
            txtStickyTitle.setText("Kết quả chưa đủ chắc chắn");
            txtStickySub.setText("Hãy chụp lại ảnh rõ hơn để hệ thống đánh giá ổn định hơn.");
        } else if (risk == RISK_HIGH) {
            txtStickyStatus.setText("Ưu tiên theo dõi");
            txtStickyTitle.setText("Nguy cơ cao");
            txtStickySub.setText("Khuyến nghị đi khám bác sĩ chuyên khoa da liễu sớm nhất có thể.");
        } else if (risk == RISK_MEDIUM) {
            txtStickyStatus.setText("Cần theo dõi");
            txtStickyTitle.setText("Nguy cơ trung bình");
            txtStickySub.setText("Nên theo dõi sát sao và kiểm tra lại nếu tổn thương thay đổi.");
        } else {
            txtStickyStatus.setText("Đang ổn định");
            txtStickyTitle.setText("Nguy cơ thấp");
            txtStickySub.setText("Tiếp tục theo dõi định kỳ để kiểm tra thay đổi.");
        }
    }

    private void updatePrimaryAction(int state) {
        btnSetReminder.setText(currentReminderEnabled ? "Cập nhật nhắc" : "Nhắc tái kiểm tra");
        btnFollowUpScan.setText(currentIsFollowUp ? "Tái kiểm tra tiếp" : "Tái kiểm tra");

        if (state == HIGH) {
            btnPrimaryAction.setText(currentReminderEnabled
                    ? "Cập nhật nhắc tái kiểm tra"
                    : "Bật nhắc tái kiểm tra");
        } else if (state == WATCH) {
            btnPrimaryAction.setText("Tái kiểm tra ngay");
        } else {
            btnPrimaryAction.setText("So sánh lần trước");
        }
    }

    private void runPrimaryAction() {
        int state = riskState(currentResultLabel, currentConfidence);
        if (state == HIGH) {
            showReminderDialog();
        } else if (state == WATCH) {
            openFollowUpScan();
        } else {
            openCompareWithNearestPrevious();
        }
    }

    private void updateImage() {
        if (!isBlank(currentImageBase64)) {
            try {
                byte[] bytes = Base64.decode(currentImageBase64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    imgResult.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgResult.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        imgResult.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imgResult.setImageResource(R.drawable.ic_camera);
    }

    private void updateInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("- Hồ sơ: ").append(isBlank(currentProfileName) ? "Không rõ" : currentProfileName).append("\n");
        sb.append("- Kết quả sàng lọc: ").append(currentResultLabel).append("\n");
        sb.append("- Mức chắc chắn: ").append(confidenceLevel(currentConfidence)).append("\n");
        sb.append("- Thời gian quét: ").append(dateFormat.format(new Date(currentCreatedAt > 0 ? currentCreatedAt : System.currentTimeMillis()))).append("\n");
        if (!isBlank(currentBodyPart)) {
            sb.append("- Vị trí vùng da: ").append(currentBodyPart).append("\n");
        }
        if (!isBlank(currentLesionCaseId)) {
            sb.append("- Hồ sơ tái kiểm tra: ")
                    .append(isBlank(currentLesionCaseTitle) ? currentLesionCaseId : currentLesionCaseTitle)
                    .append("\n");
        }
        sb.append("- Loại bản ghi: ").append(currentIsFollowUp ? "Tái kiểm tra" : "Scan gốc");
        txtDetailInfo.setText(sb.toString());
    }

    private void loadLatestCheckData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || isBlank(currentProfileId) || isBlank(currentCheckId)) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("skin_checks")
                .child(currentCheckId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    SkinCheck c = snapshot.getValue(SkinCheck.class);
                    if (c == null) return;

                    currentResultLabel = isBlank(c.resultLabel) ? currentResultLabel : normalizeRiskLabel(c.resultLabel);
                    currentConfidence = c.confidence;
                    currentCreatedAt = c.createdAt;
                    if (!isBlank(c.imageBase64)) currentImageBase64 = c.imageBase64;
                    currentNote = safe(c.note);
                    currentReminderEnabled = c.reminderEnabled;
                    currentReminderDays = c.reminderDays;
                    currentReminderAt = c.reminderAt > 0L
                            ? c.reminderAt
                            : (currentReminderEnabled ? computeLegacyReminderAt(c.createdAt, c.reminderDays) : 0L);
                    currentIsFollowUp = c.isFollowUp;
                    currentBodyPart = safe(c.bodyPart);
                    currentLesionCaseId = safe(c.lesionCaseId);
                    currentLesionCaseTitle = safe(c.lesionCaseTitle);

                    renderScreen();
                    loadTimelineData();
                });
    }

    private void loadTimelineData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || isBlank(currentProfileId)) {
            updateTimelineUI();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("skin_checks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    timelineChecks.clear();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        SkinCheck c = child.getValue(SkinCheck.class);
                        if (c == null) continue;
                        if (isBlank(c.id)) c.id = child.getKey();
                        c.resultLabel = normalizeRiskLabel(c.resultLabel);
                        timelineChecks.add(c);
                    }

                    Collections.sort(timelineChecks, new Comparator<SkinCheck>() {
                        @Override
                        public int compare(SkinCheck o1, SkinCheck o2) {
                            return Long.compare(o1.createdAt, o2.createdAt);
                        }
                    });

                    updateTimelineUI();
                })
                .addOnFailureListener(e -> {
                    timelineChecks.clear();
                    updateTimelineUI();
                });
    }

    private void updateTimelineUI() {
        layoutTimelineNodes.removeAllViews();

        if (timelineChecks.isEmpty()) {
            txtTimelineStatus.setText("Chưa có dữ liệu diễn tiến");
            txtTimelineSummary.setText("Hồ sơ này chưa có dữ liệu để phân tích xu hướng.");
            addTimelineText("Hãy thực hiện thêm lần quét để bắt đầu theo dõi theo mốc thời gian.");
            return;
        }

        SkinCheck current = findById(currentCheckId);
        if (current == null) current = timelineChecks.get(timelineChecks.size() - 1);

        SkinCheck previous = previousOf(current);
        int curRisk = riskLevel(current.resultLabel);
        int prevRisk = previous != null ? riskLevel(previous.resultLabel) : RISK_LOW;

        if (previous == null) {
            txtTimelineStatus.setText("Đang theo dõi nền ban đầu");
            txtTimelineSummary.setText("Đây là mốc đầu tiên của hồ sơ. Cần thêm dữ liệu để đánh giá xu hướng rõ hơn.");
        } else if (curRisk > prevRisk) {
            txtTimelineStatus.setText("Xu hướng tăng mức nguy cơ");
            txtTimelineSummary.setText("Lần quét hiện tại có mức nguy cơ cao hơn mốc trước.");
        } else if (curRisk < prevRisk) {
            txtTimelineStatus.setText("Xu hướng cải thiện");
            txtTimelineSummary.setText("Kết quả hiện tại tích cực hơn các mốc trước.");
        } else if (curRisk == RISK_HIGH) {
            txtTimelineStatus.setText("Nguy cơ vẫn ở mức cao");
            txtTimelineSummary.setText("Các lần quét gần đây vẫn nằm trong nhóm cần theo dõi.");
        } else if (curRisk == RISK_MEDIUM) {
            txtTimelineStatus.setText("Cần tiếp tục theo dõi");
            txtTimelineSummary.setText("Các mốc gần đây đang ở mức nguy cơ trung bình.");
        } else {
            txtTimelineStatus.setText("Xu hướng ổn định");
            txtTimelineSummary.setText("Diễn tiến giữa các mốc đang ổn định.");
        }

        int start = Math.max(0, timelineChecks.size() - 5);
        for (int i = start; i < timelineChecks.size(); i++) {
            SkinCheck c = timelineChecks.get(i);
            addTimelineNode(c, eq(c.id, current.id), i == timelineChecks.size() - 1);
        }
    }

    private void addTimelineText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setPadding(0, dp(4), 0, dp(4));
        layoutTimelineNodes.addView(tv);
    }

    private void addTimelineNode(SkinCheck check, boolean isCurrent, boolean isLast) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setLayoutParams(new LinearLayout.LayoutParams(dp(18), LinearLayout.LayoutParams.MATCH_PARENT));

        View dot = new View(this);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotLp.leftMargin = dp(4);
        dotLp.topMargin = dp(4);
        dot.setLayoutParams(dotLp);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(isCurrent ? Color.parseColor("#2563EB") : Color.parseColor("#CBD5E1"));
        dot.setBackground(dotBg);
        rail.addView(dot);

        if (!isLast) {
            View line = new View(this);
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(2), dp(44));
            lineLp.leftMargin = dp(8);
            lineLp.topMargin = dp(4);
            line.setLayoutParams(lineLp);
            line.setBackgroundColor(Color.parseColor("#E2E8F0"));
            rail.addView(line);
        }

        MaterialCardView card = new MaterialCardView(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.setRadius(dp(12));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(isCurrent ? Color.parseColor("#93C5FD") : Color.parseColor("#E5E7EB"));
        card.setCardBackgroundColor(isCurrent ? Color.parseColor("#EFF6FF") : Color.WHITE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(8), dp(10), dp(8));

        TextView title = new TextView(this);
        title.setText(dateFormat.format(new Date(check.createdAt > 0 ? check.createdAt : System.currentTimeMillis()))
                + (isCurrent ? "  -  Mốc hiện tại" : ""));
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText((isBlank(check.resultLabel) ? "Không rõ kết luận" : normalizeRiskLabel(check.resultLabel))
                + (check.isFollowUp ? " - Tái kiểm tra" : ""));
        subtitle.setTextColor(Color.parseColor("#475569"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setPadding(0, dp(4), 0, 0);

        content.addView(title);
        content.addView(subtitle);
        card.addView(content);

        row.addView(rail);
        row.addView(card);
        layoutTimelineNodes.addView(row);
    }

    private void openCompareWithNearestPrevious() {
        SkinCheck current = findById(currentCheckId);
        if (current == null && !timelineChecks.isEmpty()) current = timelineChecks.get(timelineChecks.size() - 1);

        SkinCheck previous = previousOf(current);
        if (current == null || previous == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Chưa đủ dữ liệu so sánh")
                    .setMessage("Cần ít nhất hai lần quét trong cùng hồ sơ để so sánh diễn tiến.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
            return;
        }

        openCompareScreen(previous);
    }

    private void showEditNoteDialog() {
        EditText e = new EditText(this);
        e.setHint("Nhập ghi chú cho lần quét này");
        e.setText(currentNote);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        int p = (int) (16 * getResources().getDisplayMetrics().density);
        e.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa ghi chú")
                .setView(e)
                .setPositiveButton("Lưu", (d, w) -> saveNoteToFirebase(e.getText().toString().trim()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveNoteToFirebase(String note) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || isBlank(currentProfileId) || isBlank(currentCheckId)) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("skin_checks")
                .child(currentCheckId)
                .child("note")
                .setValue(note)
                .addOnSuccessListener(u -> {
                    currentNote = note;
                    renderScreen();
                    Utils.toast(this, "Đã lưu ghi chú");
                })
                .addOnFailureListener(e -> Utils.toast(this, "Lưu ghi chú thất bại"));
    }
    private void showAttachCaseDialog() {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || isBlank(currentProfileId) || isBlank(currentCheckId)) {
            Utils.toast(this, "Không thể gắn hồ sơ theo dõi cho kết quả này");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SkinLesionCase> existingCases = new ArrayList<>();
                    List<String> options = new ArrayList<>();

                    options.add("＋ Tạo hồ sơ tái kiểm tra mới");

                    for (DataSnapshot profileSnap : snapshot.getChildren()) {
                        String profileId = profileSnap.getKey();

                        MedicalProfile profile = profileSnap.getValue(MedicalProfile.class);
                        String profileName = profile != null
                                ? TextSanitizer.sanitize(profile.fullName)
                                : "";

                        if (isBlank(profileName)) {
                            profileName = "Hồ sơ người dùng chưa đặt tên";
                        }

                        for (DataSnapshot caseSnap : profileSnap.child("lesion_cases").getChildren()) {
                            SkinLesionCase item = caseSnap.getValue(SkinLesionCase.class);

                            if (item == null) {
                                continue;
                            }

                            if (isBlank(item.id)) {
                                item.id = caseSnap.getKey();
                            }

                            item.profileId = profileId;
                            item.profileName = profileName;

                            String caseTitle = TextSanitizer.sanitize(item.title);
                            String bodyPart = TextSanitizer.sanitize(item.bodyPart);

                            if (isBlank(caseTitle)) {
                                caseTitle = "Hồ sơ tái kiểm tra chưa đặt tên";
                            }

                            StringBuilder label = new StringBuilder();

                            label.append(caseTitle);

                            if (!isBlank(bodyPart)) {
                                label.append(" • ").append(bodyPart);
                            }

                            label.append("\n");
                            label.append("Thuộc: ").append(profileName);

                            if (item.scanCount > 0) {
                                label.append(" • ").append(item.scanCount).append(" lần quét");
                            } else {
                                label.append(" • chưa có mốc quét");
                            }

                            existingCases.add(item);
                            options.add(label.toString());
                        }
                    }

                    String dialogTitle = existingCases.isEmpty()
                            ? "Chưa có hồ sơ theo dõi"
                            : "Chọn hồ sơ để theo dõi";

                    new AlertDialog.Builder(this)
                            .setTitle(dialogTitle)
                            .setItems(options.toArray(new String[0]), (dialog, which) -> {
                                if (which == 0) {
                                    showCreateCaseDialog();
                                    return;
                                }

                                SkinLesionCase selectedCase = existingCases.get(which - 1);

                                if (!currentProfileId.equals(selectedCase.profileId)) {
                                    Utils.toast(
                                            this,
                                            "Hồ sơ này thuộc: " + selectedCase.profileName
                                    );
                                    return;
                                }

                                attachCheckToCase(selectedCase);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Utils.toast(this, "Không thể tải danh sách hồ sơ tái kiểm tra"));
    }
    private void confirmCopyCaseToCurrentProfile(SkinLesionCase selectedCase) {
        if (selectedCase == null) {
            return;
        }

        final String finalTitle = isBlank(TextSanitizer.sanitize(selectedCase.title))
                ? "Hồ sơ tái kiểm tra"
                : TextSanitizer.sanitize(selectedCase.title);

        final String finalBodyPart = TextSanitizer.sanitize(selectedCase.bodyPart);
        final String finalDescription = TextSanitizer.sanitize(selectedCase.description);

        String sourceProfileName = TextSanitizer.sanitize(selectedCase.profileName);

        if (isBlank(sourceProfileName)) {
            sourceProfileName = "hồ sơ khác";
        }

        new AlertDialog.Builder(this)
                .setTitle("Hồ sơ thuộc người dùng khác")
                .setMessage(
                        "\"" + finalTitle + "\" đang thuộc " + sourceProfileName + ".\n\n" +
                                "Để tránh lẫn dữ liệu giữa các hồ sơ người dùng, app sẽ tạo một bản sao hồ sơ theo dõi này cho hồ sơ hiện tại, sau đó gắn ảnh quét vào bản sao mới."
                )
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo bản sao", (dialog, which) ->
                        createCaseAndAttach(finalTitle, finalBodyPart, finalDescription))
                .show();
    }
    private void showCreateCaseDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(R.layout.bottomsheet_create_lesion_case, null);
        bottomSheetDialog.setContentView(view);

        TextView txtSheetRisk = view.findViewById(R.id.txtSheetRisk);
        TextView txtSheetDate = view.findViewById(R.id.txtSheetDate);

        com.google.android.material.textfield.TextInputLayout layoutCaseTitle =
                view.findViewById(R.id.layoutCaseTitle);
        com.google.android.material.textfield.TextInputLayout layoutCaseBodyPart =
                view.findViewById(R.id.layoutCaseBodyPart);
        com.google.android.material.textfield.TextInputLayout layoutCaseDescription =
                view.findViewById(R.id.layoutCaseDescription);

        com.google.android.material.textfield.TextInputEditText edtCaseTitle =
                view.findViewById(R.id.edtCaseTitle);
        com.google.android.material.textfield.TextInputEditText edtCaseBodyPart =
                view.findViewById(R.id.edtCaseBodyPart);
        com.google.android.material.textfield.TextInputEditText edtCaseDescription =
                view.findViewById(R.id.edtCaseDescription);

        com.google.android.material.button.MaterialButton btnCancel =
                view.findViewById(R.id.btnCancelCreateCase);
        com.google.android.material.button.MaterialButton btnSubmit =
                view.findViewById(R.id.btnSubmitCreateCase);

        txtSheetRisk.setText(normalizeRiskLabel(currentResultLabel));
        txtSheetDate.setText("Lần quét ngày " +
                new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        .format(new java.util.Date(currentCreatedAt > 0 ? currentCreatedAt : System.currentTimeMillis())));

        edtCaseTitle.setText(defaultCaseTitle());

        if (!isBlank(currentBodyPart)) {
            edtCaseBodyPart.setText(currentBodyPart);
        }

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            layoutCaseTitle.setError(null);
            layoutCaseBodyPart.setError(null);
            layoutCaseDescription.setError(null);

            String rawTitle = edtCaseTitle.getText() != null
                    ? TextSanitizer.sanitize(edtCaseTitle.getText().toString())
                    : "";
            String finalTitle = isBlank(rawTitle) ? defaultCaseTitle() : rawTitle;

            String bodyPart = edtCaseBodyPart.getText() != null
                    ? TextSanitizer.sanitize(edtCaseBodyPart.getText().toString())
                    : "";

            String description = edtCaseDescription.getText() != null
                    ? TextSanitizer.sanitize(edtCaseDescription.getText().toString())
                    : "";

            if (isBlank(finalTitle)) {
                layoutCaseTitle.setError("Vui lòng nhập tên hồ sơ");
                return;
            }

            createCaseAndAttach(finalTitle, bodyPart, description);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
    private String defaultCaseTitle() {
        if (!isBlank(currentBodyPart)) {
            return "Theo dõi " + currentBodyPart;
        }

        long time = currentCreatedAt > 0 ? currentCreatedAt : System.currentTimeMillis();

        return "Tên hồ sơ " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(time));
    }

    private void createCaseAndAttach(String title, String bodyPart, String description) {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || isBlank(currentProfileId)) {
            return;
        }

        String caseId = String.valueOf(System.currentTimeMillis());

        SkinLesionCase lesionCase = new SkinLesionCase(
                caseId,
                currentProfileId,
                TextSanitizer.sanitize(currentProfileName),
                title,
                bodyPart,
                description
        );

        lesionCase.latestCheckId = currentCheckId;
        lesionCase.latestRiskLabel = normalizeRiskLabel(currentResultLabel);
        lesionCase.latestConfidence = currentConfidence;
        lesionCase.lastScanAt = currentCreatedAt;
        lesionCase.scanCount = 1;
        lesionCase.coverImageBase64 = currentImageBase64;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("lesion_cases")
                .child(caseId)
                .setValue(lesionCase)
                .addOnSuccessListener(unused -> attachCheckToCase(lesionCase))
                .addOnFailureListener(e -> Utils.toast(this, "Tạo hồ sơ thất bại"));
    }

    private void attachCheckToCase(SkinLesionCase lesionCase) {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null
                || lesionCase == null
                || isBlank(lesionCase.id)
                || isBlank(currentProfileId)
                || isBlank(currentCheckId)) {
            return;
        }

        String rawTitle = TextSanitizer.sanitize(lesionCase.title);
        final String finalTitle = isBlank(rawTitle) ? "Case theo dõi" : rawTitle;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("lesionCaseId", lesionCase.id);
        updates.put("lesionCaseTitle", finalTitle);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("skin_checks")
                .child(currentCheckId)
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    currentLesionCaseId = lesionCase.id;
                    currentLesionCaseTitle = finalTitle;

                    updateCaseSummary(lesionCase.id);
                    renderScreen();

                    Utils.toast(this, "Đã tạo hồ sơ tái kiểm tra");
                })
                .addOnFailureListener(e -> Utils.toast(this, "Tạo hồ sơ thất bại"));
    }
    private void updateCaseSummary(String caseId) {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || isBlank(caseId) || isBlank(currentProfileId)) {
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("skin_checks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 0;
                    SkinCheck latest = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        SkinCheck sc = child.getValue(SkinCheck.class);

                        if (sc == null || !caseId.equals(sc.lesionCaseId)) {
                            continue;
                        }

                        if (isBlank(sc.id)) {
                            sc.id = child.getKey();
                        }

                        count++;

                        if (latest == null || sc.createdAt > latest.createdAt) {
                            latest = sc;
                        }
                    }

                    if (latest == null) {
                        latest = new SkinCheck(currentCheckId, currentResultLabel, currentConfidence, currentImageBase64);
                        latest.createdAt = currentCreatedAt;
                        latest.id = currentCheckId;
                    }

                    java.util.Map<String, Object> caseUpdates = new java.util.HashMap<>();
                    caseUpdates.put("updatedAt", System.currentTimeMillis());
                    caseUpdates.put("scanCount", Math.max(1, count));
                    caseUpdates.put("latestCheckId", latest.id);
                    caseUpdates.put("latestRiskLabel", normalizeRiskLabel(latest.resultLabel));
                    caseUpdates.put("latestConfidence", latest.confidence);
                    caseUpdates.put("lastScanAt", latest.createdAt);
                    caseUpdates.put("coverImageBase64", latest.imageBase64);

                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .child("medical_profiles")
                            .child(currentProfileId)
                            .child("lesion_cases")
                            .child(caseId)
                            .updateChildren(caseUpdates);
                });
    }

    private void openCurrentLesionCase() {
        if (isBlank(currentLesionCaseId)) {
            showAttachCaseDialog();
            return;
        }

        Intent intent = new Intent(this, LesionCaseDetailActivity.class);
        intent.putExtra("profileId", currentProfileId);
        intent.putExtra("profileName", currentProfileName);
        intent.putExtra("caseId", currentLesionCaseId);

        PageTransitionHelper.navigateWithLoading(this, intent);
    }
    private void showReminderDialog() {
        String[] options = {"Tắt nhắc lại", "Chọn ngày giờ nhắc"};

        new AlertDialog.Builder(this)
                .setTitle("Thiết lập nhắc tái quét")
                .setItems(options, (d, w) -> {
                    if (w == 0) {
                        saveReminderToFirebase(false, 0L);
                    } else {
                        openReminderDatePicker();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openReminderDatePicker() {
        final Calendar initial = Calendar.getInstance();
        if (currentReminderAt > System.currentTimeMillis()) {
            initial.setTimeInMillis(currentReminderAt);
        } else {
            initial.add(Calendar.HOUR_OF_DAY, 1);
        }

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> openReminderTimePicker(initial, year, month, dayOfMonth),
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        datePicker.show();
    }

    private void openReminderTimePicker(Calendar initial, int year, int month, int dayOfMonth) {
        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    long selectedAt = selected.getTimeInMillis();
                    if (selectedAt <= System.currentTimeMillis()) {
                        Utils.toast(this, "Không thể đặt thời gian trong quá khứ.");
                        return;
                    }
                    saveReminderToFirebase(true, selectedAt);
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
        );
        timePicker.show();
    }

    private void saveReminderToFirebase(boolean enabled, long reminderAtMillis) {
        if (enabled && !ensureReminderPermissions()) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || isBlank(currentProfileId) || isBlank(currentCheckId)) return;

        long safeReminderAt = enabled ? reminderAtMillis : 0L;
        long baseTime = currentCreatedAt > 0L ? currentCreatedAt : System.currentTimeMillis();
        int legacyDays = enabled ? Math.max(1, (int) Math.ceil((safeReminderAt - baseTime) / (double) DAY_MS)) : 7;

        FirebaseDatabase.getInstance().getReference("users").child(uid).child("medical_profiles").child(currentProfileId)
                .child("skin_checks").child(currentCheckId).child("reminderEnabled").setValue(enabled);
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("medical_profiles").child(currentProfileId)
                .child("skin_checks").child(currentCheckId).child("reminderDays").setValue(legacyDays);
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("medical_profiles").child(currentProfileId)
                .child("skin_checks").child(currentCheckId).child("reminderAt").setValue(safeReminderAt)
                .addOnSuccessListener(u -> {
                    currentReminderEnabled = enabled;
                    currentReminderDays = legacyDays;
                    currentReminderAt = safeReminderAt;
                    renderScreen();

                    if (enabled) {
                        ReminderService.scheduleAt(this, currentProfileId, currentCheckId, currentResultLabel, safeReminderAt);
                        Utils.toast(this, "Đã bật nhắc vào " + dateFormat.format(new Date(safeReminderAt)));
                    } else {
                        ReminderService.cancel(this, currentProfileId, currentCheckId);
                        Utils.toast(this, "Đã tắt nhắc lại");
                    }
                })
                .addOnFailureListener(e -> Utils.toast(this, "Lưu nhắc thất bại"));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Utils.toast(this, "Đã cấp quyền thông báo. Hãy đặt nhắc lại một lần nữa.");
            } else {
                Utils.toast(this, "Chưa có quyền thông báo, reminder sẽ không hiện.");
            }
        }
    }
    private void openFollowUpScan() {
        refreshAfterAction = true;
        Intent i = new Intent(this, ScanActivity.class);
        i.putExtra("profileId", currentProfileId);
        i.putExtra("followUpMode", true);
        i.putExtra("followUpSourceCheckId", currentCheckId);
        i.putExtra("followUpProfileName", currentProfileName);
        i.putExtra("trackingCaseId", currentLesionCaseId);
        i.putExtra("trackingCaseTitle", currentLesionCaseTitle);
        startActivity(i);
    }

    private void loadOtherScansForCompare() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || isBlank(currentProfileId)) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(currentProfileId)
                .child("skin_checks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SkinCheck> others = new ArrayList<>();
                    List<String> labels = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        SkinCheck c = child.getValue(SkinCheck.class);
                        if (c != null && isBlank(c.id)) {
                            c.id = child.getKey();
                        }
                        if (c != null && c.id != null && !c.id.equals(currentCheckId)) {
                            c.resultLabel = normalizeRiskLabel(c.resultLabel);
                            others.add(c);
                            labels.add(dateFormat.format(new Date(c.createdAt > 0 ? c.createdAt : System.currentTimeMillis()))
                                    + " - " + (c.resultLabel != null ? c.resultLabel : "Không rõ"));
                        }
                    }

                    if (others.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Chưa thể so sánh")
                                .setMessage("Hồ sơ này chưa có đủ ít nhất 2 lần quét để so sánh.")
                                .setPositiveButton("Đã hiểu", null)
                                .show();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Chọn lần quét để so sánh")
                            .setItems(labels.toArray(new String[0]), (d, w) -> openCompareScreen(others.get(w)))
                            .setNegativeButton("Hủy", null)
                            .show();
                });
    }

    private void openCompareScreen(SkinCheck oldScan) {
        Intent i = new Intent(this, CompareScanActivity.class);
        i.putExtra("profileName", currentProfileName);

        i.putExtra("oldResultLabel", normalizeRiskLabel(oldScan.resultLabel));
        i.putExtra("oldConfidence", oldScan.confidence);
        i.putExtra("oldCreatedAt", oldScan.createdAt);
        i.putExtra("oldImageBase64", oldScan.imageBase64);

        i.putExtra("newResultLabel", normalizeRiskLabel(currentResultLabel));
        i.putExtra("newConfidence", currentConfidence);
        i.putExtra("newCreatedAt", currentCreatedAt);
        i.putExtra("newImageBase64", currentImageBase64);

        startActivity(i);
    }

    private long computeLegacyReminderAt(long createdAt, int reminderDays) {
        if (createdAt <= 0L || reminderDays <= 0) return 0L;
        return createdAt + (reminderDays * DAY_MS);
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        ));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean eq(String a, String b) {
        return a != null && a.equals(b);
    }

    private float clamp(float p) {
        return Math.max(0f, Math.min(100f, p));
    }

    private int riskState(String label, float confidence) {
        if (clamp(confidence * 100f) < 55f) return WATCH;

        int risk = riskLevel(label);
        if (risk == RISK_HIGH) return HIGH;
        if (risk == RISK_MEDIUM) return WATCH;
        return LOW;
    }

    private String confidenceLevel(float c) {
        float p = clamp(c * 100f);
        if (p >= 85f) return "Cao";
        if (p >= 70f) return "Trung bình";
        if (p >= 55f) return "Thấp";
        return "Không chắc chắn";
    }

    private String riskTag(String label, float confidence) {
        if (clamp(confidence * 100f) < 55f) return "Kết quả chưa chắc chắn";

        int risk = riskLevel(label);
        if (risk == RISK_HIGH) return "Nguy cơ cao";
        if (risk == RISK_MEDIUM) return "Nguy cơ trung bình";
        return "Nguy cơ thấp";
    }

    private String resultSubtitle(String label, float confidence) {
        if (clamp(confidence * 100f) < 55f) return "Nên quét lại hoặc theo dõi thêm";

        int risk = riskLevel(label);
        if (risk == RISK_HIGH) return "Khuyến nghị đi khám bác sĩ da liễu sớm";
        if (risk == RISK_MEDIUM) return "Nên theo dõi và kiểm tra lại";
        return "Tiếp tục theo dõi tại nhà";
    }

    private String resultExplain(String label, float confidence) {
        if (clamp(confidence * 100f) < 55f) {
            return "Kết quả hiện chưa đủ chắc chắn để đưa ra đánh giá ổn định.";
        }

        int risk = riskLevel(label);
        if (risk == RISK_HIGH) {
            return "Kết quả sàng lọc AI cho thấy tổn thương thuộc nhóm nguy cơ cao.";
        }
        if (risk == RISK_MEDIUM) {
            return "Kết quả sàng lọc AI cho thấy tổn thương thuộc nhóm nguy cơ trung bình.";
        }
        return "Kết quả sàng lọc AI cho thấy tổn thương thuộc nhóm nguy cơ thấp.";
    }

    private String nextAction(String label, float confidence) {
        if (clamp(confidence * 100f) < 55f) {
            return "Khuyến nghị: chụp lại ảnh ở điều kiện sáng tốt hơn.";
        }

        int risk = riskLevel(label);
        if (risk == RISK_HIGH) {
            return "Khuyến nghị: đi khám bác sĩ chuyên khoa da liễu sớm nhất có thể.";
        }
        if (risk == RISK_MEDIUM) {
            return "Khuyến nghị: theo dõi và quét lại nếu tổn thương thay đổi hoặc kéo dài.";
        }
        return "Khuyến nghị: tiếp tục theo dõi. Nếu tổn thương thay đổi kích thước hoặc màu sắc, hãy kiểm tra lại.";
    }

    private SkinCheck findById(String id) {
        if (isBlank(id)) return null;
        for (SkinCheck c : timelineChecks) {
            if (c != null && eq(c.id, id)) return c;
        }
        return null;
    }

    private SkinCheck previousOf(SkinCheck current) {
        if (current == null || timelineChecks.size() < 2) return null;
        int idx = timelineChecks.indexOf(current);
        return idx > 0 ? timelineChecks.get(idx - 1) : null;
    }

    private String normalizeRiskLabel(String label) {
        if (label == null) return "Không rõ kết luận";
        String clean = TextSanitizer.sanitize(label);
        String lower = clean.toLowerCase(Locale.getDefault());

        if (lower.contains("nguy cơ cao") || lower.contains("nguy co cao")
                || lower.contains("ác tính") || lower.contains("ac tinh")
                || lower.contains("malignant") || lower.contains("suspicious")) {
            return "Nguy cơ cao";
        }

        if (lower.contains("nguy cơ trung bình") || lower.contains("nguy co trung binh")
                || lower.contains("medium")) {
            return "Nguy cơ trung bình";
        }

        if (lower.contains("nguy cơ thấp") || lower.contains("nguy co thap")
                || lower.contains("lành tính") || lower.contains("lanh tinh")
                || lower.contains("benign") || lower.contains("normal")) {
            return "Nguy cơ thấp";
        }

        return clean;
    }
    private boolean ensureReminderPermissions() {
        if (!AppPreferences.isAppNotificationsEnabled(this)) {
            Utils.toast(this, "Bạn đang tắt thông báo trong app. Hãy bật lại ở Hồ sơ > Cài đặt.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                );
                Utils.toast(this, "Hãy cấp quyền thông báo rồi bấm lại.");
                return false;
            }
        }

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        if (!managerCompat.areNotificationsEnabled()) {
            Intent appSettings = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(appSettings);
            Utils.toast(this, "Thông báo hệ thống đang tắt cho app. Hãy bật rồi thử lại.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ReminderService.isReminderChannelBlocked(this)) {
            Intent channelSettings = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderService.CHANNEL_ID);
            startActivity(channelSettings);
            Utils.toast(this, "Kênh nhắc tái quét đang bị tắt. Hãy bật âm thanh/hiển thị rồi thử lại.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            boolean canExact = alarmManager != null && alarmManager.canScheduleExactAlarms();
            android.util.Log.d("REMINDER_DEBUG", "canScheduleExactAlarms=" + canExact);

            if (!canExact) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Utils.toast(this, "Hãy cho phép báo thức chính xác rồi bấm lại.");
                return false;
            }
        }

        return true;
    }
    private int riskLevel(String resultLabel) {
        String lower = normalizeRiskLabel(resultLabel).toLowerCase(Locale.getDefault());
        if (lower.contains("nguy cơ cao")) return RISK_HIGH;
        if (lower.contains("nguy cơ trung bình")) return RISK_MEDIUM;
        return RISK_LOW;
    }
}


