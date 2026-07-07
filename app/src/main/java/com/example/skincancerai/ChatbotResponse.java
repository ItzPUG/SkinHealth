package com.example.skincancerai;

import java.util.List;

public class ChatbotResponse {
    public final String answer;
    public final boolean confident;
    public final List<String> suggestions;

    public ChatbotResponse(String answer, boolean confident, List<String> suggestions) {
        this.answer = answer;
        this.confident = confident;
        this.suggestions = suggestions;
    }
}
