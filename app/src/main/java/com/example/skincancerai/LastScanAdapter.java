package com.example.skincancerai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LastScanAdapter extends RecyclerView.Adapter<LastScanAdapter.ViewHolder> {

    public interface OnScanClickListener {
        void onScanClick(HomeScanItem item);
    }

    public static class HomeScanItem {
        public final String profileId;
        public final String profileName;
        public final SkinCheck skinCheck;

        public HomeScanItem(String profileId, String profileName, SkinCheck skinCheck) {
            this.profileId = profileId;
            this.profileName = profileName;
            this.skinCheck = skinCheck;
        }
    }

    private final List<HomeScanItem> items;
    private final OnScanClickListener clickListener;

    public LastScanAdapter(List<HomeScanItem> items, OnScanClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_last_scan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeScanItem item = items.get(position);
        SkinCheck check = item.skinCheck;
        String resultLabel = TextSanitizer.normalizeResultLabel(check.resultLabel);
        boolean highRisk = isHighRisk(resultLabel);

        holder.txtScanType.setText(check.isFollowUp ? "Follow-up" : "Mới");
        holder.txtScanName.setText(item.profileName);
        holder.txtScanResult.setText(resultLabel);
        holder.txtScanDate.setText(buildMetaLine(check));

        holder.txtScanResult.setBackgroundResource(highRisk ? R.drawable.bg_warning_soft : R.drawable.bg_icon_soft_blue);
        holder.txtScanResult.setTextColor(highRisk ? 0xFF9A3412 : 0xFF1D4ED8);
        holder.txtScanType.setBackgroundResource(check.isFollowUp ? R.drawable.bg_glass_box : R.drawable.bg_white_pill);
        holder.txtScanType.setTextColor(check.isFollowUp ? 0xFFFFFFFF : 0xFF1D4ED8);

        if (check.imageBase64 != null && !check.imageBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(check.imageBase64, Base64.DEFAULT);
                Bitmap original = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (original != null) {
                    holder.imgScan.setImageBitmap(original);
                } else {
                    holder.imgScan.setImageResource(R.drawable.ic_camera);
                }
            } catch (Exception ignored) {
                holder.imgScan.setImageResource(R.drawable.ic_camera);
            }
        } else {
            holder.imgScan.setImageResource(R.drawable.ic_camera);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onScanClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static boolean isHighRisk(String label) {
        String normalized = label == null ? "" : label.toLowerCase(Locale.ROOT);
        return normalized.contains("ác") || normalized.contains("ac")
                || normalized.contains("cao") || normalized.contains("high")
                || normalized.contains("malignant") || normalized.contains("suspicious");
    }

    private static String buildMetaLine(SkinCheck check) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());
        return format.format(new Date(check.createdAt))
                + " • " + String.format(Locale.getDefault(), "%.0f%%", check.confidence * 100f);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgScan;
        TextView txtScanType;
        TextView txtScanName;
        TextView txtScanResult;
        TextView txtScanDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgScan = itemView.findViewById(R.id.imgScan);
            txtScanType = itemView.findViewById(R.id.txtScanType);
            txtScanName = itemView.findViewById(R.id.txtScanName);
            txtScanResult = itemView.findViewById(R.id.txtScanResult);
            txtScanDate = itemView.findViewById(R.id.txtScanDate);
        }
    }
}
