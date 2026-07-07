package com.example.skincancerai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAuthAdapter extends RecyclerView.Adapter<OnboardingAuthAdapter.PageVH> {

    private final List<OnboardingAuthPage> items;

    public OnboardingAuthAdapter(List<OnboardingAuthPage> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_auth_page, parent, false);
        return new PageVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH holder, int position) {
        OnboardingAuthPage item = items.get(position);
        holder.imgSlide.setImageResource(item.imageRes);
        holder.txtTitle.setText(item.title);
        holder.txtDesc.setText(item.description);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class PageVH extends RecyclerView.ViewHolder {
        ImageView imgSlide;
        TextView txtTitle;
        TextView txtDesc;

        public PageVH(@NonNull View itemView) {
            super(itemView);
            imgSlide = itemView.findViewById(R.id.imgSlide);
            txtTitle = itemView.findViewById(R.id.txtSlideTitle);
            txtDesc = itemView.findViewById(R.id.txtSlideDesc);
        }
    }
}
