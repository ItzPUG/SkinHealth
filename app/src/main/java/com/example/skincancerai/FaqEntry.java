package com.example.skincancerai;

import java.util.List;

public class FaqEntry {
    public final String id;
    public final List<String> keywords;
    public final String answer;
    public final String category;

    public FaqEntry(String id, List<String> keywords, String answer, String category) {
        this.id = id;
        this.keywords = keywords;
        this.answer = answer;
        this.category = category;
    }
}
