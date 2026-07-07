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

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LesionTimelineAdapter extends RecyclerView.Adapter<LesionTimelineAdapter.TimelineVH> {

    public interface OnTimelineClickListener {
        void onTimelineClick(SkinCheck item);
    }

    private final List<SkinCheck> items = new ArrayList<>();
    private final OnTimelineClickListener listener;
    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public LesionTimelineAdapter(OnTimelineClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SkinCheck> data) {
        items.clear();

        if (data != null) {
            items.addAll(data);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesion_timeline, parent, false);
        return new TimelineVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineVH holder, int position) {
        SkinCheck item = items.get(position);

        String label = safe(item.resultLabel, "Chưa có kết quả");
        String note = safe(item.note, "");

        holder.txtOrder.setText("#" + (items.size() - position));
        holder.txtRisk.setText(label);
        holder.txtDate.setText(item.createdAt > 0
                ? dateTimeFormat.format(new Date(item.createdAt))
                : "Không rõ thời gian");

        holder.txtConfidence.setText(String.format(
                Locale.getDefault(),
                "%.1f%%",
                item.confidence * 100f
        ));

        if (note.trim().isEmpty()) {
            holder.txtNote.setVisibility(View.GONE);
        } else {
            holder.txtNote.setVisibility(View.VISIBLE);
            holder.txtNote.setText("Ghi chú: " + note);
        }

        applyRiskColor(holder.txtRisk, label);

        Bitmap bitmap = decodeBase64(item.imageBase64);
        if (bitmap != null) {
            holder.imgScan.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.imgScan.setImageBitmap(bitmap);
        } else {
            holder.imgScan.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.imgScan.setImageResource(R.drawable.ic_camera);
        }

        holder.root.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimelineClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private Bitmap decodeBase64(String base64) {
        try {
            if (base64 == null || base64.trim().isEmpty()) {
                return null;
            }

            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void applyRiskColor(TextView view, String label) {
        String lower = label == null ? "" : label.toLowerCase(Locale.ROOT);

        if (lower.contains("cao") || lower.contains("high")) {
            view.setTextColor(Color.parseColor("#DC2626"));
        } else if (lower.contains("trung") || lower.contains("medium")) {
            view.setTextColor(Color.parseColor("#D97706"));
        } else {
            view.setTextColor(Color.parseColor("#2563EB"));
        }
    }

    static class TimelineVH extends RecyclerView.ViewHolder {
        MaterialCardView root;
        ImageView imgScan;
        TextView txtOrder;
        TextView txtRisk;
        TextView txtDate;
        TextView txtConfidence;
        TextView txtNote;

        TimelineVH(@NonNull View itemView) {
            super(itemView);

            root = (MaterialCardView) itemView;
            imgScan = itemView.findViewById(R.id.imgTimelineScan);
            txtOrder = itemView.findViewById(R.id.txtTimelineOrder);
            txtRisk = itemView.findViewById(R.id.txtTimelineRisk);
            txtDate = itemView.findViewById(R.id.txtTimelineDate);
            txtConfidence = itemView.findViewById(R.id.txtTimelineConfidence);
            txtNote = itemView.findViewById(R.id.txtTimelineNote);
        }
    }
}
