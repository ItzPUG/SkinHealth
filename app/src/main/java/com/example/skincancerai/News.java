package com.example.skincancerai;

public class News {
    public String title;
    public String description;
    public String imageUrl;
    public String source;
    public String url;

    public News() {}

    public News(String title, String description, String imageUrl, String source, String url) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.source = source;
        this.url = url;
    }
}
