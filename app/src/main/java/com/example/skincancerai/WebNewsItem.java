package com.example.skincancerai;

public class WebNewsItem {
    public final String title;
    public final String summary;
    public final String content;
    public final String dateText;
    public final String category;
    public final String articleUrl;
    public final String imageUrl;

    public WebNewsItem(String title,
                       String summary,
                       String content,
                       String dateText,
                       String category,
                       String articleUrl,
                       String imageUrl) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.dateText = dateText;
        this.category = category;
        this.articleUrl = articleUrl;
        this.imageUrl = imageUrl;
    }
}
