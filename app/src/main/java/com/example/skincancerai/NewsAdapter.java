package com.example.skincancerai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    public interface OnNewsClickListener {
        void onNewsClick(NewsItem item);
    }

    private final List<NewsItem> items = new ArrayList<>();
    private final OnNewsClickListener listener;

    public NewsAdapter(OnNewsClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NewsItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = items.get(position);

        holder.txtTitle.setText(item.title);
        holder.txtDesc.setText(item.description);
        holder.txtMeta.setText(item.dateText);
        holder.imgNews.setImageResource(item.imageRes);

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNewsClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        CardView cardRoot;
        ImageView imgNews;
        TextView txtTitle;
        TextView txtDesc;
        TextView txtMeta;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = (CardView) itemView;
            imgNews = itemView.findViewById(R.id.imgNews);
            txtTitle = itemView.findViewById(R.id.txtNewsItemTitle);
            txtDesc = itemView.findViewById(R.id.txtNewsItemDesc);
            txtMeta = itemView.findViewById(R.id.txtNewsItemMeta);
        }
    }
}
