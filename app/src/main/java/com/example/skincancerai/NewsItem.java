package com.example.skincancerai;

public class NewsItem {
    public final String id;
    public final String title;
    public final String description;
    public final String content;
    public final String dateText;
    public final String category;
    public final int imageRes;

    public NewsItem(String id,
                    String title,
                    String description,
                    String content,
                    String dateText,
                    String category,
                    int imageRes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.dateText = dateText;
        this.category = category;
        this.imageRes = imageRes;
    }
}
