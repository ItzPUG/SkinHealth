package com.example.skincancerai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "main_notification_prefs";
    private static final String PREF_SEEN_REMINDERS = "seen_reminders";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private static final String HOME_CACHE_PREFS = "home_cache_prefs";
    private static final String KEY_HERO_TITLE = "hero_title";
    private static final String KEY_HERO_DESC = "hero_desc";
    private static final String KEY_SUMMARY_RESULT = "summary_result";
    private static final String KEY_SUMMARY_RISK = "summary_risk";
    private static final String KEY_SUMMARY_META = "summary_meta";
    private static final String KEY_REMINDER_TITLE = "reminder_title";
    private static final String KEY_REMINDER_SUB = "reminder_sub";

    private RecyclerView rvQuickActions;
    private TextView txtUser;
    private ImageView btnNotify;
    private ImageView imgAvatar;
    private TextView txtNotifyBadge;
    private FloatingActionButton fabCamera;
    private LinearLayout navHome;
    private LinearLayout navHistory;
    private LinearLayout navProfile;
    private LinearLayout navHealth;
    private RecyclerView rvLastScan;

    private TextView txtHeroTitle;
    private TextView txtHeroDescription;
    private TextView txtSummaryResult;
    private TextView txtSummaryRisk;
    private TextView txtSummaryMeta;
    private TextView txtReminderTitle;
    private TextView txtReminderSubtitle;
    private TextView txtLastScansEmpty;

    private LinearLayout btnOpenLastResult;
    private LinearLayout btnReminderAction;
//    private LinearLayout cardQuickSup;
//    private LinearLayout cardQuickHistory;
//    private LinearLayout cardQuickProfile;
    private MaterialCardView cardLatestSummary;
    private MaterialCardView cardReminder;

    private LastScanAdapter.HomeScanItem latestScanItem;
    private final List<ReminderNotice> dueReminderNotices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            PageTransitionHelper.navigateWithLoading(this, new Intent(this, LoginActivity.class), true);
            return;
        }

        bindViews();
        setupBottomNavLabelsAndIcons();
        setupActions();
        loadAvatar(user);
        loadHeaderUserName(user);
        restoreHomeCache();
        loadDashboardData();
        loadReminderNotifications();
        requestNotificationPermissionIfNeeded();
        setupQuickActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
        loadReminderNotifications();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadHeaderUserName(user);
            loadAvatar(user);
        }
    }

    private void bindViews() {
        txtUser = findViewById(R.id.txtUser);
        btnNotify = findViewById(R.id.btnNotify);
        txtNotifyBadge = findViewById(R.id.txtNotifyBadge);
        imgAvatar = findViewById(R.id.imgAvatar);
        rvLastScan = findViewById(R.id.rvLastScan);

        rvQuickActions = findViewById(R.id.rvQuickActions);

        txtHeroTitle = findViewById(R.id.txtHeroTitle);
        txtHeroDescription = findViewById(R.id.txtHeroDescription);
        txtSummaryResult = findViewById(R.id.txtSummaryResult);
        txtSummaryRisk = findViewById(R.id.txtSummaryRisk);
        txtSummaryMeta = findViewById(R.id.txtSummaryMeta);
        txtReminderTitle = findViewById(R.id.txtReminderTitle);
        txtReminderSubtitle = findViewById(R.id.txtReminderSubtitle);
        txtLastScansEmpty = findViewById(R.id.txtLastScansEmpty);

        btnOpenLastResult = findViewById(R.id.btnOpenLastResult);
        btnReminderAction = findViewById(R.id.btnReminderAction);
//        cardQuickSup = findViewById(R.id.cardQuickSup);
//        cardQuickHistory = findViewById(R.id.cardQuickHistory);
//        cardQuickProfile = findViewById(R.id.cardQuickProfile);
        cardLatestSummary = findViewById(R.id.cardLatestSummary);
        cardReminder = findViewById(R.id.cardReminder);

        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navProfile = findViewById(R.id.navProfile);
        navHealth = findViewById(R.id.navHealth);
        fabCamera = findViewById(R.id.fabCamera);

        rvLastScan.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        renderEmptySummary();
        updateReminderCard();
    }

    private void setupActions() {
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> openAccount());
        }

        if (btnNotify != null) {
            btnNotify.setOnClickListener(v -> showReminderDialog());
        }


        if (fabCamera != null) {
            fabCamera.setOnClickListener(v -> openScan());
        }


        View btnSeeAllScans = findViewById(R.id.btnSeeAllScans);
        if (btnSeeAllScans != null) {
            btnSeeAllScans.setOnClickListener(v -> openHistory());
        }


        if (btnOpenLastResult != null) {
            btnOpenLastResult.setOnClickListener(v -> openLatestAction());
        }

        if (cardLatestSummary != null) {
            cardLatestSummary.setOnClickListener(v -> openLatestAction());
        }

        if (btnReminderAction != null) {
            btnReminderAction.setOnClickListener(v -> openReminderAction());
        }

        if (cardReminder != null) {
            cardReminder.setOnClickListener(v -> openReminderAction());
        }

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                // current screen
            });
        }

        if (navHistory != null) {
            navHistory.setOnClickListener(v -> openHistory());
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> openAccount());
        }

        if (navHealth != null) {
            navHealth.setOnClickListener(v -> openProfiles());
        }
    }

    private void openScan() {
        PageTransitionHelper.navigateWithLoading(this, new Intent(this, ScanActivity.class));
    }

    private void openHistory() {
        PageTransitionHelper.navigateWithLoading(this, new Intent(this, HistoryActivity.class));
    }

    private void openProfiles() {
        PageTransitionHelper.navigateWithLoading(this, new Intent(this, MedicalProfileListActivity.class));
    }

    private void openAccount() {
        PageTransitionHelper.navigateWithLoading(this, new Intent(this, ProfileActivity.class));
    }

    private void openLatestAction() {
        if (latestScanItem != null && latestScanItem.skinCheck != null) {
            openHistoryDetail(latestScanItem.profileId, latestScanItem.profileName, latestScanItem.skinCheck);
        } else {
            openProfiles();
        }
    }

    private void openReminderAction() {
        if (dueReminderNotices.isEmpty()) {
            openHistory();
        } else {
            showReminderDialog();
        }
    }

    private void loadHeaderUserName(FirebaseUser user) {
        if (user == null) return;

        String fallback = getFriendlyNameFromEmail(user.getEmail());

        DatabaseReference profileRef = FirebaseDatabase
                .getInstance("https://skincancerai-6c951-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(user.getUid())
                .child("profile");

        profileRef.child("displayName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String displayName = safeDecrypt(snapshot.getValue(String.class));

                if (!displayName.isEmpty()) {
                    txtUser.setText(TextSanitizer.sanitize(displayName));
                } else if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                    txtUser.setText(TextSanitizer.sanitize(user.getDisplayName()));
                } else {
                    txtUser.setText(fallback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtUser.setText(fallback);
            }
        });
    }

    private void setupBottomNavLabelsAndIcons() {
        updateBottomNavItem(navHealth, R.drawable.ic_profile, getString(R.string.bottom_label_profile));
        updateBottomNavItem(navProfile, R.drawable.ic_account, getString(R.string.bottom_label_account));
    }

    private void updateBottomNavItem(LinearLayout navItem, int iconRes, String label) {
        if (navItem == null || navItem.getChildCount() < 2) return;
        if (navItem.getChildAt(0) instanceof ImageView) {
            ((ImageView) navItem.getChildAt(0)).setImageResource(iconRes);
        }
        if (navItem.getChildAt(1) instanceof TextView) {
            ((TextView) navItem.getChildAt(1)).setText(label);
        }
    }

    private void loadAvatar(FirebaseUser user) {
        DatabaseReference profileRef = FirebaseDatabase
                .getInstance("https://skincancerai-6c951-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(user.getUid())
                .child("profile");

        profileRef.child("avatarBase64").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String base64 = snapshot.getValue(String.class);
                if (base64 == null || base64.isEmpty()) return;
                try {
                    byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bitmap != null) imgAvatar.setImageBitmap(bitmap);
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadDashboardData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            renderEmptySummary();
            rvLastScan.setAdapter(new LastScanAdapter(new ArrayList<>(), null));
            txtLastScansEmpty.setVisibility(View.VISIBLE);
            return;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles");

        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LastScanAdapter.HomeScanItem> previewItems = new ArrayList<>();
                latestScanItem = null;

                for (DataSnapshot profileSnap : snapshot.getChildren()) {
                    String profileId = profileSnap.getKey();
                    MedicalProfile profile = profileSnap.getValue(MedicalProfile.class);
                    String profileName = profile != null ? TextSanitizer.sanitize(profile.fullName) : "";
                    if (profileName.trim().isEmpty()) {
                        profileName = getString(R.string.profile_unnamed);
                    }

                    DataSnapshot checksSnap = profileSnap.child("skin_checks");
                    for (DataSnapshot checkSnap : checksSnap.getChildren()) {
                        SkinCheck check = checkSnap.getValue(SkinCheck.class);
                        if (check == null) continue;

                        if (check.id == null || check.id.trim().isEmpty()) {
                            check.id = checkSnap.getKey();
                        }
                        check.resultLabel = TextSanitizer.normalizeResultLabel(check.resultLabel);

                        LastScanAdapter.HomeScanItem item =
                                new LastScanAdapter.HomeScanItem(profileId, profileName, check);
                        previewItems.add(item);

                        if (latestScanItem == null
                                || item.skinCheck.createdAt > latestScanItem.skinCheck.createdAt) {
                            latestScanItem = item;
                        }
                    }
                }

                previewItems.sort((o1, o2) -> Long.compare(o2.skinCheck.createdAt, o1.skinCheck.createdAt));
                List<LastScanAdapter.HomeScanItem> topItems = previewItems.size() > 5
                        ? new ArrayList<>(previewItems.subList(0, 5))
                        : previewItems;

                rvLastScan.setAdapter(new LastScanAdapter(topItems, item ->
                        openHistoryDetail(item.profileId, item.profileName, item.skinCheck)));
                txtLastScansEmpty.setVisibility(topItems.isEmpty() ? View.VISIBLE : View.GONE);
                renderLatestSummary(latestScanItem);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                renderEmptySummary();
                txtLastScansEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderLatestSummary(LastScanAdapter.HomeScanItem latestItem) {
        if (latestItem == null || latestItem.skinCheck == null) {
            renderEmptySummary();
            return;
        }

        SkinCheck check = latestItem.skinCheck;
        String resultLabel = TextSanitizer.normalizeResultLabel(check.resultLabel);
        boolean highRisk = isHighRisk(resultLabel);
        String followUpText = check.isFollowUp ? " • Bản theo dõi" : " • Lần quét mới";

        txtHeroTitle.setText(highRisk
                ? "Lần quét gần nhất cần được theo dõi kỹ hơn"
                : "Lần quét gần nhất đang ở mức nguy cơ thấp");
        txtHeroDescription.setText(latestItem.profileName + followUpText + "\n"
                + "Hãy mở chi tiết để xem lại ảnh, độ tin cậy và đặt lịch tái quét.");

        txtSummaryResult.setText(resultLabel);
        txtSummaryRisk.setText(highRisk
                ? "Khuyến nghị: tái quét sớm hoặc đi khám da liễu"
                : "Khuyến nghị: tiếp tục theo dõi định kỳ");
        txtSummaryMeta.setText("Quét lúc " + formatDateTime(check.createdAt)
                + " • Độ tin cậy " + formatConfidence(check.confidence));

        if (btnOpenLastResult != null) {
            TextView btnText = btnOpenLastResult.findViewById(R.id.txtLatestActionLabel);
            if (btnText != null) {
                btnText.setText("Xem kết quả gần nhất");
            }
        }

        saveCurrentHomeCache();
    }
    private void setupQuickActions() {
        if (rvQuickActions == null) return;

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvQuickActions.setLayoutManager(layoutManager);
        rvQuickActions.setHasFixedSize(true);
        rvQuickActions.setNestedScrollingEnabled(false);
        rvQuickActions.setOverScrollMode(View.OVER_SCROLL_NEVER);

        java.util.List<QuickActionItem> quickActions = new java.util.ArrayList<>();
        quickActions.add(new QuickActionItem(
                R.drawable.ic_chatbot,
                "Hỗ trợ",
                "Chat-bot tư vấn",
                "chatbot"
        ));
        quickActions.add(new QuickActionItem(
                R.drawable.ic_history,
                "Lịch sử",
                "Xem chi tiết",
                "history"
        ));
        quickActions.add(new QuickActionItem(
                R.drawable.ic_history,
                "Theo dõi",
                "Timeline tổn thương",
                "lesion_tracker"
        ));
        quickActions.add(new QuickActionItem(
                R.drawable.ic_profile,
                "Hồ sơ",
                "Quản lý bệnh nhân",
                "profile"
        ));
        quickActions.add(new QuickActionItem(
                R.drawable.ic_info,
                "Tin tức",
                "Kiến thức da liễu",
                "news"
        ));

        QuickActionAdapter quickAdapter = new QuickActionAdapter(quickActions, item -> {
            if (item == null) return;

            switch (item.action) {
                case "chatbot":
                    PageTransitionHelper.navigateWithLoading(
                            MainActivity.this,
                            new Intent(MainActivity.this, ChatbotActivity.class)
                    );
                    break;

                case "history":
                    openHistory();
                    break;

                case "lesion_tracker":
                    PageTransitionHelper.navigateWithLoading(
                            MainActivity.this,
                            new Intent(MainActivity.this, LesionCaseListActivity.class)
                    );
                    break;

                case "profile":
                    openProfiles();
                    break;

                case "news":
                    PageTransitionHelper.navigateWithLoading(
                            MainActivity.this,
                            new Intent(MainActivity.this, NewsActivity.class)
                    );
                    break;
            }
        });

        rvQuickActions.setAdapter(quickAdapter);

        androidx.recyclerview.widget.PagerSnapHelper snapHelper =
                new androidx.recyclerview.widget.PagerSnapHelper();
        if (rvQuickActions.getOnFlingListener() == null) {
            snapHelper.attachToRecyclerView(rvQuickActions);
        }
    }
    private void renderEmptySummary() {
        latestScanItem = null;
        txtHeroTitle.setText("Sẵn sàng cho lần quét đầu tiên");
        txtHeroDescription.setText("Tạo hồ sơ bệnh nhân, chụp ảnh vùng da và bắt đầu theo dõi bằng AI ngay trên ứng dụng.");
        txtSummaryResult.setText("Chưa có dữ liệu");
        txtSummaryRisk.setText("Bạn chưa lưu kết quả quét nào");
        txtSummaryMeta.setText("Kết quả gần nhất sẽ xuất hiện tại đây sau lần quét đầu tiên.");

        if (btnOpenLastResult != null) {
            TextView btnText = btnOpenLastResult.findViewById(R.id.txtLatestActionLabel);
            if (btnText != null) {
                btnText.setText("Tạo hồ sơ trước");
            }
        }

        saveCurrentHomeCache();
    }

    private void loadReminderNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            dueReminderNotices.clear();
            updateNotifyBadge();
            updateReminderCard();
            return;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles");

        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ReminderNotice> due = new ArrayList<>();
                long now = System.currentTimeMillis();

                for (DataSnapshot profileSnap : snapshot.getChildren()) {
                    String profileId = profileSnap.getKey();
                    if (profileId == null) continue;

                    MedicalProfile profile = profileSnap.getValue(MedicalProfile.class);
                    String profileName = (profile != null && profile.fullName != null && !profile.fullName.trim().isEmpty())
                            ? TextSanitizer.sanitize(profile.fullName)
                            : "Hồ sơ";

                    DataSnapshot checksSnap = profileSnap.child("skin_checks");
                    for (DataSnapshot checkSnap : checksSnap.getChildren()) {
                        SkinCheck check = checkSnap.getValue(SkinCheck.class);
                        if (check == null || !check.reminderEnabled) continue;

                        long dueAt = check.reminderAt > 0L
                                ? check.reminderAt
                                : ((check.reminderDays > 0 && check.createdAt > 0)
                                ? (check.createdAt + (check.reminderDays * DAY_MS))
                                : 0L);
                        if (dueAt <= 0L) continue;

                        String checkId = (check.id != null && !check.id.trim().isEmpty()) ? check.id : checkSnap.getKey();
                        if (checkId == null) continue;

                        String label = TextSanitizer.normalizeResultLabel(check.resultLabel);
                        if (label.trim().isEmpty()) {
                            label = "Kết quả trước đó";
                        }

                        ReminderNotice notice = new ReminderNotice();
                        notice.id = profileId + "_" + checkId;
                        notice.profileId = profileId;
                        notice.profileName = profileName;
                        notice.checkId = checkId;
                        notice.resultLabel = label;
                        notice.title = (dueAt <= now) ? "Đến hạn tái quét" : "Lịch nhắc tái quét";
                        notice.message = profileName + " • " + label;
                        notice.dueAt = dueAt;
                        due.add(notice);
                    }
                }

                due.sort((o1, o2) -> Long.compare(o1.dueAt, o2.dueAt));
                dueReminderNotices.clear();
                dueReminderNotices.addAll(due);
                updateNotifyBadge();
                updateReminderCard();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateReminderCard();
            }
        });
    }

    private void updateReminderCard() {
        TextView btnText = null;
        if (btnReminderAction != null) {
            btnText = btnReminderAction.findViewById(R.id.txtReminderActionLabel);
        }

        if (dueReminderNotices.isEmpty()) {
            txtReminderTitle.setText("Chưa có lịch nhắc tái quét");
            txtReminderSubtitle.setText("Sau khi lưu kết quả, bạn có thể đặt tái quét trong phần chi tiết lịch sử để theo dõi lại đúng hẹn.");
            if (btnText != null) {
                btnText.setText("Mở lịch sử");
            }
            saveCurrentHomeCache();
            return;
        }

        ReminderNotice nearest = dueReminderNotices.get(0);
        boolean overdue = nearest.dueAt <= System.currentTimeMillis();
        txtReminderTitle.setText(overdue ? "Đã đến hạn tái quét" : "Lịch nhắc gần nhất");
        txtReminderSubtitle.setText(nearest.profileName + " • " + nearest.resultLabel + "\n" + formatReminderTime(nearest.dueAt));
        if (btnText != null) {
            btnText.setText("Xem nhắc nhở");
        }
        saveCurrentHomeCache();
    }

    private void showReminderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notifications, null, false);
        RecyclerView rvNotices = dialogView.findViewById(R.id.rvNotices);
        TextView txtEmpty = dialogView.findViewById(R.id.txtEmptyNotice);
        com.google.android.material.button.MaterialButton btnCloseDialog =
                dialogView.findViewById(R.id.btnCloseDialog);

        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        NotificationAdapter adapter = new NotificationAdapter(dueReminderNotices, notice -> {
            markNoticeAsSeen(notice.id);
            updateNotifyBadge();
            openNoticeDetail(notice);
        });
        rvNotices.setAdapter(adapter);

        boolean isEmpty = dueReminderNotices.isEmpty();
        txtEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvNotices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openNoticeDetail(ReminderNotice notice) {
        Intent intent = new Intent(this, HistoryDetailActivity.class);
        intent.putExtra("profileId", notice.profileId);
        intent.putExtra("profileName", notice.profileName);
        intent.putExtra("checkId", notice.checkId);
        intent.putExtra("resultLabel", notice.resultLabel);
        PageTransitionHelper.navigateWithLoading(this, intent);
    }

    private void openHistoryDetail(String profileId, String profileName, SkinCheck check) {
        Intent intent = new Intent(this, HistoryDetailActivity.class);
        intent.putExtra("profileId", profileId);
        intent.putExtra("profileName", profileName);
        intent.putExtra("checkId", check.id);
        intent.putExtra("resultLabel", check.resultLabel);
        intent.putExtra("confidence", check.confidence);
        intent.putExtra("createdAt", check.createdAt);
        intent.putExtra("imageBase64", check.imageBase64);
        intent.putExtra("note", check.note);
        intent.putExtra("reminderEnabled", check.reminderEnabled);
        intent.putExtra("reminderDays", check.reminderDays);
        intent.putExtra("reminderAt", check.reminderAt);
        intent.putExtra("isFollowUp", check.isFollowUp);
        intent.putExtra("bodyPart", check.bodyPart);
        intent.putExtra("lesionCaseId", check.lesionCaseId);
        intent.putExtra("lesionCaseTitle", check.lesionCaseTitle);
        PageTransitionHelper.navigateWithLoading(this, intent);
    }

    private void updateNotifyBadge() {
        int unseenCount = getUnseenReminderCount();
        if (unseenCount <= 0) {
            txtNotifyBadge.setVisibility(View.GONE);
            return;
        }
        txtNotifyBadge.setVisibility(View.VISIBLE);
        txtNotifyBadge.setText(unseenCount > 99 ? "99+" : String.valueOf(unseenCount));
    }

    private int getUnseenReminderCount() {
        Set<String> seen = getSeenReminderIds();
        int unseen = 0;
        for (ReminderNotice n : dueReminderNotices) {
            if (!seen.contains(n.id)) unseen++;
        }
        return unseen;
    }

    private void markNoticeAsSeen(String noticeId) {
        Set<String> seen = new HashSet<>(getSeenReminderIds());
        seen.add(noticeId);
        saveSeenIds(seen);
    }

    private Set<String> getSeenReminderIds() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> stored = prefs.getStringSet(PREF_SEEN_REMINDERS, new HashSet<>());
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    private void saveSeenIds(Set<String> seenIds) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putStringSet(PREF_SEEN_REMINDERS, seenIds).apply();
    }

    private boolean isHighRisk(String label) {
        String normalized = TextSanitizer.normalizeResultLabel(label).toLowerCase(Locale.ROOT);
        return normalized.contains("ác") || normalized.contains("ac")
                || normalized.contains("cao") || normalized.contains("high")
                || normalized.contains("malignant") || normalized.contains("suspicious");
    }

    private String formatDateTime(long timeMillis) {
        if (timeMillis <= 0L) return "chưa rõ thời gian";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return format.format(new Date(timeMillis));
    }

    private String formatConfidence(float confidence) {
        return String.format(Locale.getDefault(), "%.1f%%", confidence * 100f);
    }

    private String formatReminderTime(long dueAt) {
        long now = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        if (dueAt <= now) {
            return "Đến hạn từ " + format.format(new Date(dueAt));
        }
        return "Nhắc lúc " + format.format(new Date(dueAt));
    }

    private void restoreHomeCache() {
        SharedPreferences prefs = getSharedPreferences(HOME_CACHE_PREFS, MODE_PRIVATE);

        txtHeroTitle.setText(prefs.getString(KEY_HERO_TITLE, txtHeroTitle.getText().toString()));
        txtHeroDescription.setText(prefs.getString(KEY_HERO_DESC, txtHeroDescription.getText().toString()));
        txtSummaryResult.setText(prefs.getString(KEY_SUMMARY_RESULT, txtSummaryResult.getText().toString()));
        txtSummaryRisk.setText(prefs.getString(KEY_SUMMARY_RISK, txtSummaryRisk.getText().toString()));
        txtSummaryMeta.setText(prefs.getString(KEY_SUMMARY_META, txtSummaryMeta.getText().toString()));
        txtReminderTitle.setText(prefs.getString(KEY_REMINDER_TITLE, txtReminderTitle.getText().toString()));
        txtReminderSubtitle.setText(prefs.getString(KEY_REMINDER_SUB, txtReminderSubtitle.getText().toString()));
    }

    private void saveCurrentHomeCache() {
        SharedPreferences prefs = getSharedPreferences(HOME_CACHE_PREFS, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_HERO_TITLE, txtHeroTitle.getText().toString())
                .putString(KEY_HERO_DESC, txtHeroDescription.getText().toString())
                .putString(KEY_SUMMARY_RESULT, txtSummaryResult.getText().toString())
                .putString(KEY_SUMMARY_RISK, txtSummaryRisk.getText().toString())
                .putString(KEY_SUMMARY_META, txtSummaryMeta.getText().toString())
                .putString(KEY_REMINDER_TITLE, txtReminderTitle.getText().toString())
                .putString(KEY_REMINDER_SUB, txtReminderSubtitle.getText().toString())
                .apply();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        9001
                );
            }
        }
    }

    private String getFriendlyNameFromEmail(String email) {
        if (email == null || email.trim().isEmpty()) return "Người dùng";
        int at = email.indexOf("@");
        if (at > 0) {
            return email.substring(0, at);
        }
        return email;
    }

    private interface OnNoticeClickListener {
        void onClick(ReminderNotice notice);
    }

    private static class ReminderNotice {
        String id;
        String title;
        String message;
        long dueAt;
        String profileId;
        String profileName;
        String checkId;
        String resultLabel;
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NoticeVH> {
        private final List<ReminderNotice> notices;
        private final OnNoticeClickListener clickListener;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        NotificationAdapter(List<ReminderNotice> notices, OnNoticeClickListener clickListener) {
            this.notices = notices;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public NoticeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NoticeVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoticeVH holder, int position) {
            ReminderNotice notice = notices.get(position);
            holder.txtTitle.setText(notice.title);
            holder.txtMessage.setText(notice.message);
            holder.txtDueAt.setText(((notice.dueAt <= System.currentTimeMillis()) ? "Đến hạn: " : "Nhắc lúc: ")
                    + dateFormat.format(new Date(notice.dueAt)));

            boolean seen = getSeenReminderIds().contains(notice.id);
            holder.dotUnread.setVisibility(seen ? View.INVISIBLE : View.VISIBLE);
            holder.cardNotice.setCardBackgroundColor(android.graphics.Color.parseColor(seen ? "#FFFFFF" : "#EFF6FF"));
            holder.cardNotice.setStrokeColor(android.graphics.Color.parseColor(seen ? "#E2E8F0" : "#BFDBFE"));
            holder.itemView.setOnClickListener(v -> clickListener.onClick(notice));
        }

        @Override
        public int getItemCount() {
            return notices.size();
        }

        class NoticeVH extends RecyclerView.ViewHolder {
            com.google.android.material.card.MaterialCardView cardNotice;
            ImageView dotUnread;
            TextView txtTitle;
            TextView txtMessage;
            TextView txtDueAt;

            NoticeVH(@NonNull View itemView) {
                super(itemView);
                cardNotice = itemView.findViewById(R.id.cardNotice);
                dotUnread = itemView.findViewById(R.id.imgUnreadDot);
                txtTitle = itemView.findViewById(R.id.txtNoticeTitle);
                txtMessage = itemView.findViewById(R.id.txtNoticeMessage);
                txtDueAt = itemView.findViewById(R.id.txtNoticeDueAt);
            }
        }
    }
    private String safeDecrypt(String value) {
        if (value == null || value.trim().isEmpty()) return "";

        String decrypted = DataCipher.decrypt(value);

        if (decrypted == null || decrypted.trim().isEmpty()) return "";

        // Nếu giải mã thất bại, DataCipher trả lại nguyên chuỗi ENC::
        if (decrypted.startsWith("ENC::")) {
            return "";
        }

        return decrypted.trim();
    }
}
