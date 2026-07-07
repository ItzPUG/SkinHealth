package com.example.skincancerai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private static final int RISK_LOW = 0;
    private static final int RISK_MEDIUM = 1;
    private static final int RISK_HIGH = 2;

    public interface OnHistoryActionListener {
        void onHistoryClick(HistoryItem item);
        void onDeleteClick(HistoryItem item);
    }

    private final List<HistoryItem> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());
    private final OnHistoryActionListener listener;

    public HistoryAdapter(OnHistoryActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<HistoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        SkinCheck check = item.skinCheck;

        String profileName = TextSanitizer.sanitize(item.profileName);
        holder.txtProfileName.setText(
                profileName.isEmpty() ? "Hồ sơ chưa đặt tên" : profileName
        );

        String resultLabel = (check != null && check.resultLabel != null && !check.resultLabel.trim().isEmpty())
                ? normalizeRiskLabel(check.resultLabel)
                : "Không rõ kết luận";
        holder.txtResult.setText(resultLabel);

        float confidence = (check != null) ? check.confidence : 0f;
        holder.txtConfidence.setText(
                "Độ tin cậy "
                        + String.format(Locale.getDefault(), "%.1f%%", confidence * 100f)
                        + " • " + getConfidenceLevel(confidence)
        );

        long time = (check != null && check.createdAt > 0)
                ? check.createdAt
                : System.currentTimeMillis();
        holder.txtDate.setText(dateFormat.format(new Date(time)));

        int risk = riskLevel(resultLabel);
        if (risk == RISK_HIGH) {
            holder.txtRiskChip.setText("Nguy cơ cao");
            holder.txtRiskChip.setBackgroundResource(R.drawable.bg_warning_soft);
            holder.txtRiskChip.setTextColor(Color.parseColor("#B45309"));
        } else if (risk == RISK_MEDIUM) {
            holder.txtRiskChip.setText("Nguy cơ trung bình");
            holder.txtRiskChip.setBackgroundResource(R.drawable.bg_warning_soft);
            holder.txtRiskChip.setTextColor(Color.parseColor("#D97706"));
        } else {
            holder.txtRiskChip.setText("Nguy cơ thấp");
            holder.txtRiskChip.setBackgroundResource(R.drawable.bg_icon_soft_blue);
            holder.txtRiskChip.setTextColor(Color.parseColor("#395CFF"));
        }

        boolean isFollowUp = check != null && check.isFollowUp;
        holder.txtFollowUpChip.setVisibility(isFollowUp ? View.VISIBLE : View.GONE);
        if (isFollowUp) {
            holder.txtFollowUpChip.setText("Hiện đã tái kiểm tra");
        }

        Bitmap thumb = null;
        if (check != null && check.imageBase64 != null && !check.imageBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(check.imageBase64, Base64.DEFAULT);
                thumb = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (Exception ignored) {
            }
        }

        if (thumb != null) {
            holder.imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.imgThumbnail.setImageBitmap(thumb);
        } else {
            holder.imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.imgThumbnail.setImageResource(R.drawable.ic_camera);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onHistoryClick(item);
        });

        holder.txtOpen.setOnClickListener(v -> {
            if (listener != null) listener.onHistoryClick(item);
        });

        holder.txtDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
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

    private int riskLevel(String resultLabel) {
        String lower = normalizeRiskLabel(resultLabel).toLowerCase(Locale.getDefault());
        if (lower.contains("nguy cơ cao")) return RISK_HIGH;
        if (lower.contains("nguy cơ trung bình")) return RISK_MEDIUM;
        return RISK_LOW;
    }

    private String getConfidenceLevel(float confidence) {
        float percent = confidence * 100f;
        if (percent >= 85f) return "Cao";
        if (percent >= 70f) return "Trung bình";
        if (percent >= 55f) return "Thấp";
        return "Không chắc chắn";
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView txtProfileName;
        TextView txtResult;
        TextView txtConfidence;
        TextView txtDate;
        TextView txtRiskChip;
        TextView txtFollowUpChip;
        TextView txtOpen;
        TextView txtDelete;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            txtProfileName = itemView.findViewById(R.id.txtProfileName);
            txtResult = itemView.findViewById(R.id.txtResult);
            txtConfidence = itemView.findViewById(R.id.txtConfidence);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtRiskChip = itemView.findViewById(R.id.txtRiskChip);
            txtFollowUpChip = itemView.findViewById(R.id.txtFollowUpChip);
            txtOpen = itemView.findViewById(R.id.txtOpen);
            txtDelete = itemView.findViewById(R.id.txtDelete);
        }
    }
}
