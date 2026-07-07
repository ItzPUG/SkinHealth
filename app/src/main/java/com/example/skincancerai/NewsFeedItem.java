package com.example.skincancerai;

public class NewsFeedItem {
    public final String title;
    public final String summary;
    public final String content;
    public final String dateText;
    public final String category;
    public final String articleUrl;
    public final String imageUrl;
    public final int imageRes;

    public NewsFeedItem(String title,
                        String summary,
                        String content,
                        String dateText,
                        String category,
                        String articleUrl,
                        String imageUrl,
                        int imageRes) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.dateText = dateText;
        this.category = category;
        this.articleUrl = articleUrl;
        this.imageUrl = imageUrl;
        this.imageRes = imageRes;
    }

    public boolean hasRemoteImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }
}
