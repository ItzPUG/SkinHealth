package com.example.skincancerai;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;

import java.util.List;
import java.util.Locale;

public class MedicalProfileAdapter extends RecyclerView.Adapter<MedicalProfileAdapter.VH> {

    private final List<MedicalProfile> list;
    private final Context context;
    private final DatabaseReference ref;

    public MedicalProfileAdapter(
            List<MedicalProfile> list,
            Context context,
            DatabaseReference ref
    ) {
        this.list = list;
        this.context = context;
        this.ref = ref;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_medical_profile, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MedicalProfile p = list.get(position);

        String displayName = (p.fullName != null && !p.fullName.trim().isEmpty())
                ? TextSanitizer.sanitize(p.fullName)
                : "Hồ sơ chưa đặt tên";
        h.txtName.setText(displayName);

        String ageText = p.age > 0 ? p.age + " tuổi" : "Chưa cập nhật tuổi";
        String genderText = (p.gender != null && !p.gender.trim().isEmpty())
                ? TextSanitizer.sanitize(p.gender)
                : "Chưa cập nhật giới tính";
        h.txtInfo.setText(ageText + " • " + genderText);

        h.txtProfileBadge.setText(buildBadge(p));
        h.txtHistory.setText(buildHistory(p.skinHistory));
        h.txtNote.setText(buildNote(p.note));

        h.btnScan.setOnClickListener(v -> openScan(p));
        h.itemView.setOnClickListener(v -> openScan(p));

        h.btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(context, MedicalProfileEditActivity.class);
            i.putExtra("profileId", p.id);
            context.startActivity(i);
        });

        h.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xóa hồ sơ")
                    .setMessage("Bạn chắc chắn muốn xóa hồ sơ này? Lịch sử quét trong hồ sơ cũng sẽ không còn hiển thị.")
                    .setPositiveButton("Xóa", (d, w) -> ref.child(p.id).removeValue())
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void openScan(MedicalProfile p) {
        Intent i = new Intent(context, ScanActivity.class);
        i.putExtra("profileId", p.id);
        context.startActivity(i);
    }

    private String buildBadge(MedicalProfile p) {
        String age = p.age > 0 ? (p.age + " tuổi") : "Tuổi chưa rõ";
        String gender = !TextUtils.isEmpty(p.gender)
                ? TextSanitizer.sanitize(p.gender)
                : "Giới tính chưa rõ";
        return age + " • " + gender;
    }

    private String buildHistory(String history) {
        if (history == null || history.trim().isEmpty()) {
            return "Tiền sử bệnh da: Chưa cập nhật";
        }
        return "Tiền sử bệnh da: " + TextSanitizer.sanitize(history);
    }

    private String buildNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            return "Ghi chú: Chưa có ghi chú thêm";
        }
        return "Ghi chú: " + TextSanitizer.sanitize(note);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtName, txtInfo, txtProfileBadge, txtHistory, txtNote;
        MaterialButton btnScan, btnEdit, btnDelete;

        VH(View v) {
            super(v);
            txtName = v.findViewById(R.id.txtName);
            txtInfo = v.findViewById(R.id.txtInfo);
            txtProfileBadge = v.findViewById(R.id.txtProfileBadge);
            txtHistory = v.findViewById(R.id.txtHistory);
            txtNote = v.findViewById(R.id.txtNote);
            btnScan = v.findViewById(R.id.btnScan);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
