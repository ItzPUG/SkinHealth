package com.example.skincancerai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuickActionAdapter extends RecyclerView.Adapter<QuickActionAdapter.QuickActionVH> {

    public interface OnQuickActionClickListener {
        void onQuickActionClick(QuickActionItem item);
    }

    private final List<QuickActionItem> items;
    private final OnQuickActionClickListener listener;

    public QuickActionAdapter(List<QuickActionItem> items, OnQuickActionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuickActionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_action, parent, false);
        return new QuickActionVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuickActionVH holder, int position) {
        QuickActionItem item = items.get(position);

        holder.imgQuickAction.setImageResource(item.iconRes);
        holder.txtQuickActionTitle.setText(item.title);
        holder.txtQuickActionSub.setText(item.subtitle);

        holder.root.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuickActionClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class QuickActionVH extends RecyclerView.ViewHolder {
        LinearLayout root;
        ImageView imgQuickAction;
        TextView txtQuickActionTitle;
        TextView txtQuickActionSub;

        public QuickActionVH(@NonNull View itemView) {
            super(itemView);
            root = (LinearLayout) itemView;
            imgQuickAction = itemView.findViewById(R.id.imgQuickAction);
            txtQuickActionTitle = itemView.findViewById(R.id.txtQuickActionTitle);
            txtQuickActionSub = itemView.findViewById(R.id.txtQuickActionSub);
        }
    }
}
