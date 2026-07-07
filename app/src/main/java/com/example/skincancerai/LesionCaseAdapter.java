package com.example.skincancerai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LesionCaseAdapter extends RecyclerView.Adapter<LesionCaseAdapter.CaseVH> {

    public interface OnCaseClickListener {
        void onCaseClick(SkinLesionCase item);
    }
    public interface OnCaseDeleteListener {
        void onCaseDelete(SkinLesionCase item);
    }

    private final List<SkinLesionCase> items = new ArrayList<>();
    private final OnCaseClickListener listener;
    private final OnCaseDeleteListener deleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public LesionCaseAdapter(OnCaseClickListener listener) {
        this(listener, null);
    }

    public LesionCaseAdapter(OnCaseClickListener listener, OnCaseDeleteListener deleteListener) {
        this.listener = listener;
        this.deleteListener = deleteListener;
    }
    public void setItems(List<SkinLesionCase> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CaseVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesion_case, parent, false);
        return new CaseVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaseVH holder, int position) {
        SkinLesionCase item = items.get(position);

        String title = sanitize(item.title, "Vùng da chưa đặt tên");
        String profile = sanitize(item.profileName, "Không rõ hồ sơ");
        String bodyPart = sanitize(item.bodyPart, "Chưa ghi vị trí");
        String latest = sanitize(item.latestRiskLabel, "Chưa có mốc quét");

        holder.txtTitle.setText(title);
        holder.txtSubtitle.setText(profile + " • " + bodyPart);
        holder.txtMeta.setText(item.scanCount + " lần quét" + lastScanText(item.lastScanAt));
        holder.txtRisk.setText(latest);

        holder.txtConfidence.setText(item.latestConfidence > 0f
                ? String.format(Locale.getDefault(), "%.1f%%", item.latestConfidence * 100f)
                : "--");

        Bitmap cover = decodeBase64(item.coverImageBase64);
        if (cover != null) {
            holder.imgCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.imgCover.setImageBitmap(cover);
        } else {
            holder.imgCover.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.imgCover.setImageResource(R.drawable.ic_camera);
        }

        holder.root.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCaseClick(item);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onCaseDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String sanitize(String value, String fallback) {
        String clean = TextSanitizer.sanitize(value);
        return clean.trim().isEmpty() ? fallback : clean;
    }

    private String lastScanText(long time) {
        if (time <= 0L) {
            return "";
        }
        return " • gần nhất " + dateFormat.format(new Date(time));
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

    static class CaseVH extends RecyclerView.ViewHolder {
        MaterialCardView root;
        ImageView imgCover;
        TextView txtTitle;
        TextView txtSubtitle;
        TextView txtMeta;
        TextView txtRisk;
        TextView txtConfidence;
        ImageButton btnDelete;
        CaseVH(@NonNull View itemView) {
            super(itemView);
            root = (MaterialCardView) itemView;
            imgCover = itemView.findViewById(R.id.imgCaseCover);
            txtTitle = itemView.findViewById(R.id.txtCaseTitle);
            txtSubtitle = itemView.findViewById(R.id.txtCaseSubtitle);
            txtMeta = itemView.findViewById(R.id.txtCaseMeta);
            txtRisk = itemView.findViewById(R.id.txtCaseRisk);
            txtConfidence = itemView.findViewById(R.id.txtCaseConfidence);
            btnDelete = itemView.findViewById(R.id.btnDeleteLesionCaseSmall);
        }
    }
}
