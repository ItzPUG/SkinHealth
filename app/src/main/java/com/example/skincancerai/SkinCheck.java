package com.example.skincancerai;

public class SkinCheck {

    public String id;
    public String resultLabel;
    public float confidence;
    public long createdAt;

    public String imageBase64;

    public boolean reminderEnabled;
    public int reminderDays;
    public long reminderAt;

    public String note;

    // Follow-up research flow
    public boolean isFollowUp;
    public String followUpFromId;
    public String bodyPart;
    // Lesion tracking case
    public String lesionCaseId;
    public String lesionCaseTitle;

    public SkinCheck() {
    }

    public SkinCheck(String id, String label, float confidence) {
        this(id, label, confidence, null);
    }

    public SkinCheck(String id, String label, float confidence, String imageBase64) {
        this.id = id;
        this.resultLabel = label;
        this.confidence = confidence;
        this.createdAt = System.currentTimeMillis();
        this.reminderEnabled = false;
        this.reminderDays = 7;
        this.reminderAt = 0L;
        this.imageBase64 = imageBase64;
        this.note = "";
        this.isFollowUp = false;
        this.followUpFromId = "";
        this.bodyPart = "";
        this.lesionCaseId = "";
        this.lesionCaseTitle = "";
    }
}
