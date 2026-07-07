package com.example.skincancerai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class NewsHighlightPagerAdapter extends RecyclerView.Adapter<NewsHighlightPagerAdapter.HighlightVH> {

    public interface OnHighlightClickListener {
        void onHighlightClick(NewsFeedItem item);
    }

    private final List<NewsFeedItem> items = new ArrayList<>();
    private final OnHighlightClickListener listener;

    public NewsHighlightPagerAdapter(OnHighlightClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NewsFeedItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HighlightVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_highlight, parent, false);
        return new HighlightVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HighlightVH holder, int position) {
        NewsFeedItem item = items.get(position);

        holder.txtMeta.setText(item.dateText);
        holder.txtTitle.setText(item.title);

        if (item.hasRemoteImage()) {
            Glide.with(holder.imgHighlight.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.onboard_health_1)
                    .error(R.drawable.onboard_health_1)
                    .centerCrop()
                    .into(holder.imgHighlight);
        } else {
            holder.imgHighlight.setImageResource(
                    item.imageRes != 0 ? item.imageRes : R.drawable.onboard_health_1
            );
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHighlightClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HighlightVH extends RecyclerView.ViewHolder {
        CardView cardRoot;
        ImageView imgHighlight;
        TextView txtMeta;
        TextView txtTitle;

        public HighlightVH(@NonNull View itemView) {
            super(itemView);
            cardRoot = (CardView) itemView;
            imgHighlight = itemView.findViewById(R.id.imgHighlight);
            txtMeta = itemView.findViewById(R.id.txtHighlightMeta);
            txtTitle = itemView.findViewById(R.id.txtHighlightTitle);
        }
    }
}
