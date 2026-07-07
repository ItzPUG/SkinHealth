package com.example.skincancerai;

public class HistoryItem {
    public String profileId;
    public String profileName;
    public SkinCheck skinCheck;

    public HistoryItem() {
    }

    public HistoryItem(String profileId, String profileName, SkinCheck skinCheck) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.skinCheck = skinCheck;
    }
}