package com.example.skincancerai;

public class QuickActionItem {
    public final int iconRes;
    public final String title;
    public final String subtitle;
    public final String action;

    public QuickActionItem(int iconRes, String title, String subtitle, String action) {
        this.iconRes = iconRes;
        this.title = title;
        this.subtitle = subtitle;
        this.action = action;
    }
}
