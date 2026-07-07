package com.example.skincancerai;

public class SkinLesionCase {
    public String id;
    public String profileId;
    public String profileName;
    public String title;
    public String bodyPart;
    public String description;
    public String status;
    public long createdAt;
    public long updatedAt;
    public long lastScanAt;
    public int scanCount;
    public String latestCheckId;
    public String latestRiskLabel;
    public float latestConfidence;
    public String coverImageBase64;

    public SkinLesionCase() {
    }

    public SkinLesionCase(String id, String profileId, String profileName, String title, String bodyPart, String description) {
        this.id = id;
        this.profileId = profileId;
        this.profileName = profileName;
        this.title = title;
        this.bodyPart = bodyPart;
        this.description = description;
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.lastScanAt = 0L;
        this.scanCount = 0;
        this.latestCheckId = "";
        this.latestRiskLabel = "";
        this.latestConfidence = 0f;
        this.coverImageBase64 = "";
    }
}
