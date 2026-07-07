package com.example.skincancerai;

public class Result {
    public String label;
    public float confidence;

    public Result(String label, float confidence) {
        this.label = label;
        this.confidence = confidence;
    }
}
