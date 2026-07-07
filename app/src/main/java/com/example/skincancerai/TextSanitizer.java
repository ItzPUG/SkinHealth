package com.example.skincancerai;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class TextSanitizer {

    public static final int RISK_LOW = 0;
    public static final int RISK_MEDIUM = 1;
    public static final int RISK_HIGH = 2;

    private static final String[] MOJIBAKE_MARKERS = {
            "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?"
    };

    private TextSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) return "";

        String current = input.trim();
        if (current.isEmpty()) return "";

        for (int i = 0; i < 3; i++) {
            String converted = new String(current.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            if (markerScore(converted) < markerScore(current)) {
                current = converted;
            } else {
                break;
            }
        }

        current = current.replace("?", "").trim();
        return current;
    }

    public static String normalizeResultLabel(String label) {
        String clean = sanitize(label);
        String lower = clean.toLowerCase(Locale.ROOT);

        if (lower.contains("nguy cơ cao") || lower.contains("nguy co cao")
                || lower.contains("ác tính") || lower.contains("ac tinh")
                || lower.contains("malignant") || lower.contains("suspicious")) {
            return "Nguy cơ cao";
        }

        if (lower.contains("nguy cơ trung bình") || lower.contains("nguy co trung binh")
                || lower.contains("medium")) {
            return "Nguy cơ trung bình";
        }

        if (lower.contains("nguy cơ thấp") || lower.contains("nguy co thap")
                || lower.contains("lành tính") || lower.contains("lanh tinh")
                || lower.contains("benign") || lower.contains("normal")) {
            return "Nguy cơ thấp";
        }

        return clean;
    }

    public static int riskLevel(String label) {
        String normalized = normalizeResultLabel(label).toLowerCase(Locale.ROOT);

        if (normalized.contains("nguy cơ cao")) return RISK_HIGH;
        if (normalized.contains("nguy cơ trung bình")) return RISK_MEDIUM;
        return RISK_LOW;
    }

    public static boolean isHighRisk(String label) {
        return riskLevel(label) == RISK_HIGH;
    }

    public static boolean isMediumRisk(String label) {
        return riskLevel(label) == RISK_MEDIUM;
    }

    public static boolean isLowRisk(String label) {
        return riskLevel(label) == RISK_LOW;
    }

    private static int markerScore(String text) {
        int score = 0;
        for (String marker : MOJIBAKE_MARKERS) {
            int idx = 0;
            while ((idx = text.indexOf(marker, idx)) >= 0) {
                score++;
                idx += marker.length();
            }
        }
        return score;
    }
}
