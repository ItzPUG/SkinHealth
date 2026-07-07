package com.example.skincancerai;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import android.app.DatePickerDialog;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryActionListener {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;

    private TextView txtTotal;
    private TextView txtUploaded;
    private TextView txtNormal;
    private TextView txtProblem;

    private Spinner spinnerProfileFilter;
    private Spinner spinnerResultFilter;
    private LinearLayout layoutFilterHeader;
    private LinearLayout layoutFilterContent;
    private TextView txtFilterSummary;
    private ImageView imgFilterExpand;

    private LinearLayout layoutEmptyState;
    private TextView txtEmptyTitle;
    private TextView txtEmptyMessage;

    private FloatingActionButton fabCamera;
    private LinearLayout navHome;
    private LinearLayout navHistory;
    private LinearLayout navProfile;
    private LinearLayout navHealth;

    private LinearLayout layoutFromDate;
    private LinearLayout layoutToDate;
    private ImageView btnSwapDate;
    private TextView txtFromDate;
    private TextView txtToDate;
    private TextView btnClearDateFilter;
    private final List<HistoryItem> filteredList = new ArrayList<>();
    private Long selectedFromDateMillis = null;
    private Long selectedToDateMillis = null;

    private final SimpleDateFormat dateOnlyFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());


    private final List<HistoryItem> allHistoryItems = new ArrayList<>();
    private final List<String> filterNames = new ArrayList<>();
    private final List<String> filterProfileIds = new ArrayList<>();

    private String selectedProfileFilterId = "ALL";
    private String selectedProfileFilterName = "Tất cả hồ sơ";
    private String selectedResultFilter = "ALL";
    private String selectedResultFilterName = "Tất cả kết quả";

    private boolean isFilterExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử chẩn đoán");
        }

        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this);
        recyclerView.setAdapter(adapter);

        txtTotal = findViewById(R.id.txtTotal);
        txtUploaded = findViewById(R.id.txtUploaded);
        txtNormal = findViewById(R.id.txtNormal);
        txtProblem = findViewById(R.id.txtProblem);

        spinnerProfileFilter = findViewById(R.id.spinnerProfileFilter);
        spinnerResultFilter = findViewById(R.id.spinnerResultFilter);
        layoutFilterHeader = findViewById(R.id.layoutFilterHeader);
        layoutFilterContent = findViewById(R.id.layoutFilterContent);
        txtFilterSummary = findViewById(R.id.txtFilterSummary);
        imgFilterExpand = findViewById(R.id.imgFilterExpand);
        layoutFromDate = findViewById(R.id.layoutFromDate);
        layoutToDate = findViewById(R.id.layoutToDate);
        btnSwapDate = findViewById(R.id.btnSwapDate);
        txtFromDate = findViewById(R.id.txtFromDate);
        txtToDate = findViewById(R.id.txtToDate);
        btnClearDateFilter = findViewById(R.id.btnClearDateFilter);

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        txtEmptyTitle = findViewById(R.id.txtEmptyTitle);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);

        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navProfile = findViewById(R.id.navProfile);
        navHealth = findViewById(R.id.navHealth);
        fabCamera = findViewById(R.id.fabCamera);
        CardView btnExportPdf = findViewById(R.id.btnExportPdf);

        btnExportPdf.setOnClickListener(v -> exportPdfWithResolvedUserName(filteredList));

        setupBottomNavLabelsAndIcons();

        navHome.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        HistoryActivity.this,
                        new Intent(HistoryActivity.this, MainActivity.class)
                )
        );
        navHistory.setOnClickListener(v -> {
            // stay here
        });
        fabCamera.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, ScanActivity.class)
                )
        );
        navProfile.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, ProfileActivity.class)
                )
        );
        navHealth.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, MedicalProfileListActivity.class)
                )
        );

        setupFilterAccordion();
        setupDateFilter();
        loadHistoryFromFirebase();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void openDatePicker(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();
        Long selected = isFromDate ? selectedFromDateMillis : selectedToDateMillis;

        if (selected != null) {
            calendar.setTimeInMillis(selected);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    if (isFromDate) {
                        selectedFromDateMillis = startOfDay(picked.getTimeInMillis());
                    } else {
                        selectedToDateMillis = endOfDay(picked.getTimeInMillis());
                    }

                    normalizeDateRangeIfNeeded();
                    updateDateViews();
                    updateFilterSummary();
                    applyFilters();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }
    private void setupDateFilter() {
        updateDateViews();

        if (layoutFromDate != null) {
            layoutFromDate.setOnClickListener(v -> openDatePicker(true));
        }

        if (layoutToDate != null) {
            layoutToDate.setOnClickListener(v -> openDatePicker(false));
        }

        if (btnSwapDate != null) {
            btnSwapDate.setOnClickListener(v -> {
                Long temp = selectedFromDateMillis;
                selectedFromDateMillis = selectedToDateMillis;
                selectedToDateMillis = temp;
                normalizeDateRangeIfNeeded();
                updateDateViews();
                updateFilterSummary();
                applyFilters();
            });
        }

        if (btnClearDateFilter != null) {
            btnClearDateFilter.setOnClickListener(v -> {
                selectedFromDateMillis = null;
                selectedToDateMillis = null;
                updateDateViews();
                updateFilterSummary();
                applyFilters();
            });
        }
    }
    private void setupFilterAccordion() {
        updateFilterSummary();
        applyFilterAccordionState();
        layoutFilterHeader.setOnClickListener(v -> {
            isFilterExpanded = !isFilterExpanded;
            applyFilterAccordionState();
        });
    }

    private void applyFilterAccordionState() {
        layoutFilterContent.setVisibility(isFilterExpanded ? View.VISIBLE : View.GONE);
        imgFilterExpand.setRotation(isFilterExpanded ? 90f : 0f);
    }

    private void updateFilterSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(selectedProfileFilterName).append(" • ").append(selectedResultFilterName);

        if (selectedFromDateMillis != null || selectedToDateMillis != null) {
            sb.append(" • ");
            if (selectedFromDateMillis != null) {
                sb.append(dateOnlyFormat.format(new Date(selectedFromDateMillis)));
            } else {
                sb.append("...");
            }

            sb.append(" - ");

            if (selectedToDateMillis != null) {
                sb.append(dateOnlyFormat.format(new Date(selectedToDateMillis)));
            } else {
                sb.append("...");
            }
        }

        txtFilterSummary.setText(sb.toString());
    }

    private void loadHistoryFromFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            updateStatistics(new ArrayList<>());
            updateEmptyState(new ArrayList<>());
            return;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles");

        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allHistoryItems.clear();
                filterNames.clear();
                filterProfileIds.clear();

                filterNames.add("Tất cả hồ sơ");
                filterProfileIds.add("ALL");

                for (DataSnapshot profileSnap : snapshot.getChildren()) {
                    String profileId = profileSnap.getKey();

                    MedicalProfile profile = profileSnap.getValue(MedicalProfile.class);
                    String profileNameRaw = (profile != null && profile.fullName != null) ? profile.fullName : "";
                    String profileName = TextSanitizer.sanitize(profileNameRaw);
                    if (profileName.trim().isEmpty()) {
                        profileName = "Hồ sơ chưa đặt tên";
                    }

                    if (profileId != null) {
                        filterNames.add(profileName);
                        filterProfileIds.add(profileId);
                    }

                    DataSnapshot checksSnap = profileSnap.child("skin_checks");
                    for (DataSnapshot checkSnap : checksSnap.getChildren()) {
                        SkinCheck check = checkSnap.getValue(SkinCheck.class);
                        if (check != null) {
                            if (check.id == null || check.id.trim().isEmpty()) {
                                check.id = checkSnap.getKey();
                            }
                            check.resultLabel = TextSanitizer.normalizeResultLabel(check.resultLabel);
                            allHistoryItems.add(new HistoryItem(profileId, profileName, check));
                        }
                    }
                }

                Collections.sort(allHistoryItems, new Comparator<HistoryItem>() {
                    @Override
                    public int compare(HistoryItem o1, HistoryItem o2) {
                        long t1 = (o1.skinCheck != null) ? o1.skinCheck.createdAt : 0L;
                        long t2 = (o2.skinCheck != null) ? o2.skinCheck.createdAt : 0L;
                        return Long.compare(t2, t1);
                    }
                });

                setupProfileFilter();
                setupResultFilter();
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                UiFeedback.showActionDialog(
                        HistoryActivity.this,
                        R.drawable.ic_status_warning,
                        getString(R.string.error_title_common),
                        getString(R.string.error_history_load_message),
                        getString(R.string.feedback_retry),
                        HistoryActivity.this::loadHistoryFromFirebase,
                        getString(R.string.feedback_close),
                        null
                );
            }
        });
    }

    private void setupProfileFilter() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filterNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfileFilter.setAdapter(spinnerAdapter);

        spinnerProfileFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedProfileFilterId = filterProfileIds.get(position);
                selectedProfileFilterName = filterNames.get(position);
                updateFilterSummary();
                applyFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupResultFilter() {
        List<String> resultOptions = new ArrayList<>();
        resultOptions.add("Tất cả kết quả");
        resultOptions.add("Nguy cơ thấp");
        resultOptions.add("Nguy cơ trung bình");
        resultOptions.add("Nguy cơ cao");

        ArrayAdapter<String> resultAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                resultOptions
        );
        resultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResultFilter.setAdapter(resultAdapter);

        spinnerResultFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedResultFilterName = resultOptions.get(position);

                if (position == 1) {
                    selectedResultFilter = "LOW";
                } else if (position == 2) {
                    selectedResultFilter = "MEDIUM";
                } else if (position == 3) {
                    selectedResultFilter = "HIGH";
                } else {
                    selectedResultFilter = "ALL";
                }

                updateFilterSummary();
                applyFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }
    private void exportPdfWithResolvedUserName(List<HistoryItem> list) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            exportPdf(list, "Người dùng");
            return;
        }

        DatabaseReference profileRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("profile")
                .child("displayName");

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String displayName = DataCipher.decrypt(snapshot.getValue(String.class));
                String userName = TextSanitizer.sanitize(displayName);

                if (userName.trim().isEmpty() || isLikelyEmail(userName)) {
                    userName = resolveFallbackUserName();
                }

                exportPdf(list, userName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                exportPdf(list, resolveFallbackUserName());
            }
        });
    }

    private void exportPdf(List<HistoryItem> list, String userName) {

        if (list == null || list.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();

        Paint textPaint = new Paint();
        Paint titlePaint = new Paint();
        Paint linePaint = new Paint();

        textPaint.setTextSize(11f);
        textPaint.setAntiAlias(true);

        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);

        linePaint.setStrokeWidth(1.5f);

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 40;

        int col1 = margin;
        int col2 = col1 + 40;
        int col3 = col2 + 100;
        int col4 = col3 + 120;
        int col5 = col4 + 100;

        int rowHeight = 90; // tăng vì có ảnh

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();

        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = margin;

        // ===== LOGO =====
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_app);
        if (logo != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 60, 60, true);
            canvas.drawBitmap(scaledLogo, pageWidth - margin - 60, margin, textPaint);
        }

        // ===== HEADER =====
        canvas.drawText("SKINHEALTH REPORT", margin, y, titlePaint);
        y += 25;

        canvas.drawText("Người dùng: " + userName, margin, y, textPaint);
        y += 18;

        canvas.drawText("Hồ sơ: " + selectedProfileFilterName, margin, y, textPaint);
        y += 18;

        canvas.drawText("Ngày xuất: " +
                        android.text.format.DateFormat.format("dd/MM/yyyy", System.currentTimeMillis()),
                margin, y, textPaint);
        y += 18;

        canvas.drawText(
                "Lưu ý: Kết quả chỉ mang tính tham khảo, không thay thế bác sĩ.",
                margin, y, textPaint
        );
        y += 25;

        // ===== TABLE HEADER =====
        canvas.drawText("STT", col1, y, textPaint);
        canvas.drawText("Ngày", col2, y, textPaint);
        canvas.drawText("Kết quả", col3, y, textPaint);
        canvas.drawText("%", col4, y, textPaint);
        canvas.drawText("Ảnh", col5, y, textPaint);

        y += 10;
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
        y += 15;

        int index = 1;

        for (HistoryItem item : list) {

            SkinCheck sc = item.skinCheck;
            if (sc == null) continue;

            // ===== PAGE BREAK =====
            if (y > pageHeight - 120) {

                canvas.drawText("Generated by SkinHealth AI", margin, pageHeight - 20, textPaint);
                document.finishPage(page);

                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;

                // Chỉ vẽ lại header bảng ở trang tiếp theo (không lặp lại tiêu đề report)
                canvas.drawText("STT", col1, y, textPaint);
                canvas.drawText("Ngày", col2, y, textPaint);
                canvas.drawText("Kết quả", col3, y, textPaint);
                canvas.drawText("%", col4, y, textPaint);
                canvas.drawText("Ảnh", col5, y, textPaint);

                y += 10;
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
                y += 15;
            }

            String date = android.text.format.DateFormat
                    .format("dd/MM/yyyy", sc.createdAt).toString();

            String confidence = (int) (sc.confidence * 100) + "%";

            String result = ellipsize(sc.resultLabel, 15);

            canvas.drawText(String.valueOf(index), col1, y, textPaint);
            canvas.drawText(date, col2, y, textPaint);
            canvas.drawText(result, col3, y, textPaint);
            canvas.drawText(confidence, col4, y, textPaint);

            // ===== ẢNH =====
            Bitmap bmp = base64ToBitmap(sc.imageBase64);
            if (bmp != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, 80, 80, true);
                canvas.drawBitmap(scaled, col5, y - 20, textPaint);
            }

            y += rowHeight;

            canvas.drawLine(margin, y - 20, pageWidth - margin, y - 20, linePaint);

            index++;
        }

        // ===== FOOTER =====
        canvas.drawText("Generated by SkinHealth AI", margin, pageHeight - 20, textPaint);

        document.finishPage(page);

        // ===== SAVE FILE =====
        File file = new File(
                android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                "SkinHealth_Report_" + System.currentTimeMillis() + ".pdf"
        );

        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this,
                    "Đã xuất PDF:\n" + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
            showOpenPdfDialog(file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo PDF", Toast.LENGTH_SHORT).show();
        }
    }
    private Bitmap base64ToBitmap(String base64) {
        try {
            byte[] decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            return android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            return null;
        }
    }
    private String ellipsize(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
    private boolean isLikelyEmail(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.contains("@");
    }
    private String resolveFallbackUserName() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return "Người dùng";
        }
        String authDisplayName = TextSanitizer.sanitize(
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName()
        );
        if (!authDisplayName.trim().isEmpty() && !isLikelyEmail(authDisplayName)) {
            return authDisplayName;
        }

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email != null && !email.trim().isEmpty()) {
            int at = email.indexOf("@");
            if (at > 0) return email.substring(0, at);
            return email;
        }
        return "Người dùng";
    }
    private void showOpenPdfDialog(File file) {
        if (file == null || !file.exists()) return;

        new AlertDialog.Builder(this)
                .setTitle("Mở file PDF?")
                .setMessage("Bạn có muốn mở file vừa xuất không?")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Mở", (dialog, which) -> openPdfFile(file))
                .show();
    }

    private void openPdfFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(Intent.createChooser(intent, "Mở file PDF"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Thiết bị chưa có ứng dụng mở PDF", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở file PDF", Toast.LENGTH_SHORT).show();
        }
    }
    private void applyFilters() {
        normalizeDateRangeIfNeeded();

        List<HistoryItem> filtered = new ArrayList<>();

        for (HistoryItem item : allHistoryItems) {
            boolean profileMatched = "ALL".equals(selectedProfileFilterId)
                    || (item.profileId != null && item.profileId.equals(selectedProfileFilterId));
            if (!profileMatched) continue;

            if (!matchesResultFilter(item.skinCheck, selectedResultFilter)) {
                continue;
            }

            long createdAt = (item.skinCheck != null) ? item.skinCheck.createdAt : 0L;

            if (selectedFromDateMillis != null && createdAt < selectedFromDateMillis) {
                continue;
            }

            if (selectedToDateMillis != null && createdAt > selectedToDateMillis) {
                continue;
            }

            filtered.add(item);
        }

        adapter.setItems(filtered);
        updateStatistics(filtered);
        updateEmptyState(filtered);
        filteredList.clear();
        filteredList.addAll(filtered);
    }

    private boolean matchesResultFilter(SkinCheck check, String resultFilter) {
        if ("ALL".equals(resultFilter)) return true;
        if (check == null || check.resultLabel == null) return false;

        int risk = TextSanitizer.riskLevel(check.resultLabel);

        if ("LOW".equals(resultFilter)) return risk == TextSanitizer.RISK_LOW;
        if ("MEDIUM".equals(resultFilter)) return risk == TextSanitizer.RISK_MEDIUM;
        if ("HIGH".equals(resultFilter)) return risk == TextSanitizer.RISK_HIGH;

        return true;
    }

    private void updateStatistics(List<HistoryItem> checks) {
        int total = checks.size();
        int low = 0;
        int watch = 0;

        for (HistoryItem item : checks) {
            if (item.skinCheck == null || item.skinCheck.resultLabel == null) continue;
            int risk = TextSanitizer.riskLevel(item.skinCheck.resultLabel);
            if (risk == TextSanitizer.RISK_LOW) {
                low++;
            } else {
                watch++;
            }
        }

        txtTotal.setText(String.valueOf(total));
        txtNormal.setText(String.valueOf(low));
        txtProblem.setText(String.valueOf(watch));
        txtUploaded.setText("Đang hiển thị " + total + " lần quét sau khi lọc");
    }

    private void updateEmptyState(List<HistoryItem> filtered) {
        boolean isEmpty = filtered == null || filtered.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (!isEmpty) return;

        if (allHistoryItems.isEmpty()) {
            txtEmptyTitle.setText("Chưa có lịch sử quét");
            txtEmptyMessage.setText("Hãy thực hiện lần scan đầu tiên để bắt đầu theo dõi.");
            return;
        }

        boolean hasDateFilter = selectedFromDateMillis != null || selectedToDateMillis != null;

        txtEmptyTitle.setText("Không có kết quả phù hợp");
        if (hasDateFilter) {
            txtEmptyMessage.setText("Không có lịch sử trong khoảng ngày đã chọn. Hãy thử đổi khoảng ngày hoặc xóa lọc.");
        } else {
            txtEmptyMessage.setText("Hãy đổi bộ lọc hồ sơ hoặc kết quả AI.");
        }
    }

    @Override
    public void onHistoryClick(HistoryItem item) {
        if (item == null || item.skinCheck == null) return;

        SkinCheck check = item.skinCheck;

        Intent intent = new Intent(this, HistoryDetailActivity.class);
        intent.putExtra("profileId", item.profileId);
        intent.putExtra("profileName", TextSanitizer.sanitize(item.profileName));
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

        PageTransitionHelper.navigateWithLoading(this, intent);
    }

    @Override
    public void onDeleteClick(HistoryItem item) {
        if (item == null || item.skinCheck == null || item.skinCheck.id == null || item.skinCheck.id.trim().isEmpty()) {
            Utils.toast(this, "Không thể xóa lịch sử này");
            return;
        }

        String title = item.profileName != null ? TextSanitizer.sanitize(item.profileName) : "hồ sơ này";
        String result = item.skinCheck.resultLabel != null ? TextSanitizer.sanitize(item.skinCheck.resultLabel) : "kết quả này";

        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử quét?")
                .setMessage("Bạn có chắc muốn xóa bản ghi \"" + result + "\" của " + title + "?\n\nHành động này không thể hoàn tác.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteHistoryItem(item))
                .show();
    }

    private void deleteHistoryItem(HistoryItem item) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || item == null || item.skinCheck == null || item.profileId == null || item.skinCheck.id == null) {
            Utils.toast(this, "Không thể xóa lịch sử này");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("medical_profiles")
                .child(item.profileId)
                .child("skin_checks")
                .child(item.skinCheck.id)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    if (item.skinCheck.reminderEnabled) {
                        ReminderService.cancel(this, item.profileId, item.skinCheck.id);
                    }
                    Utils.toast(this, "Đã xóa lịch sử quét");
                    loadHistoryFromFirebase();
                })
                .addOnFailureListener(e ->
                        UiFeedback.showActionDialog(
                                this,
                                R.drawable.ic_status_warning,
                                getString(R.string.error_title_common),
                                getString(R.string.error_delete_history_message),
                                getString(R.string.feedback_retry),
                                () -> deleteHistoryItem(item),
                                getString(R.string.feedback_close),
                                null
                        )
                );
    }

    private boolean isBenignLabel(String label) {
        return TextSanitizer.riskLevel(label) == TextSanitizer.RISK_LOW;
    }

    private boolean isMalignantLabel(String label) {
        int risk = TextSanitizer.riskLevel(label);
        return risk == TextSanitizer.RISK_MEDIUM || risk == TextSanitizer.RISK_HIGH;
    }
    private long startOfDay(long timeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long endOfDay(long timeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private void normalizeDateRangeIfNeeded() {
        if (selectedFromDateMillis != null
                && selectedToDateMillis != null
                && selectedFromDateMillis > selectedToDateMillis) {
            Long temp = selectedFromDateMillis;
            selectedFromDateMillis = selectedToDateMillis;
            selectedToDateMillis = temp;
        }
    }

    private void updateDateViews() {
        if (txtFromDate != null) {
            txtFromDate.setText(selectedFromDateMillis == null
                    ? "Chọn ngày"
                    : dateOnlyFormat.format(new Date(selectedFromDateMillis)));
        }

        if (txtToDate != null) {
            txtToDate.setText(selectedToDateMillis == null
                    ? "Chọn ngày"
                    : dateOnlyFormat.format(new Date(selectedToDateMillis)));
        }
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
}
