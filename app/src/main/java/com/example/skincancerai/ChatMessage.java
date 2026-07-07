package com.example.skincancerai;

public class ChatMessage {
    public static final int ROLE_USER = 0;
    public static final int ROLE_BOT = 1;

    public final int role;
    public final String text;
    public final long createdAt;

    public ChatMessage(int role, String text) {
        this.role = role;
        this.text = text;
        this.createdAt = System.currentTimeMillis();
    }
}
