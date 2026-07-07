package com.example.skincancerai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AppChatbotEngine {

    private final List<FaqEntry> knowledge = new ArrayList<>();
    private String lastCategory = "";

    public AppChatbotEngine() {
        seedKnowledge();
    }
    public ChatbotResponse ask(String rawQuestion) {
        return ask(rawQuestion, "");
    }
    public ChatbotResponse ask(String rawQuestion, String scanContext) {
        String q = normalize(rawQuestion);
        String context = normalize(scanContext);
        if (q.isEmpty()) {
            return new ChatbotResponse(
                    "Bạn có thể hỏi mình về kết quả nguy cơ, tái kiểm tra, reminder, cách chụp ảnh, lịch sử quét hoặc khi nào nên đi khám.",
                    false,
                    defaultSuggestions()
            );
        }
        if (context.contains("nguy co cao")) {

            if (q.contains("co nguy hiem khong")
                    || q.contains("lam sao")
                    || q.contains("nen lam gi")) {

                return new ChatbotResponse(
                        "Kết quả gần đây của bạn đang ở mức nguy cơ cao. Bạn nên theo dõi sát hơn và nên đi khám bác sĩ da liễu để được kiểm tra chính xác.",
                        false,
                        Arrays.asList(
                                "Khi nào nên đi khám?",
                                "Tái kiểm tra là gì?",
                                "Nguy cơ cao nghĩa là gì?"
                        )
                );
            }
        }

        if (looksLikeDiagnosisQuestion(q)) {
            return new ChatbotResponse(
                    "Mình không thể xác định bạn mắc bệnh gì chỉ từ câu hỏi này. App chỉ hỗ trợ sàng lọc và theo dõi, không thay thế chẩn đoán của bác sĩ.",
                    false,
                    Arrays.asList(
                            "Nguy cơ cao nghĩa là gì?",
                            "Khi nào nên đi khám?",
                            "App này có chẩn đoán bệnh không?"
                    )
            );
        }

        FaqEntry best = null;
        int bestScore = 0;

        for (FaqEntry entry : knowledge) {
            int score = scoreEntry(q, entry);
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }

        if (best != null && bestScore >= 2) {
            lastCategory = best.category;
            return new ChatbotResponse(best.answer, true, followupSuggestions(best.category));
        }
        if (lastCategory.equals("risk")) {

            if (q.contains("co nguy hiem")
                    || q.contains("co sao khong")
                    || q.contains("co can lo")) {

                return new ChatbotResponse(
                        "Kết quả nguy cơ không phải là chẩn đoán xác định, nhưng nếu nguy cơ cao hoặc tổn thương thay đổi theo thời gian thì nên đi khám bác sĩ da liễu.",
                        true,
                        followupSuggestions("risk")
                );
            }
        }
        String categoryHint = guessCategory(q);
        if (categoryHint != null) {
            return new ChatbotResponse(
                    "Mình chưa chắc đã hiểu đúng ý bạn, nhưng có vẻ bạn đang hỏi về " + categoryHint + ". Bạn có thể hỏi rõ hơn theo một trong các gợi ý dưới đây.",
                    false,
                    suggestionsForCategory(categoryHint)
            );
        }

        return new ChatbotResponse(
                "Mình chưa hiểu rõ câu hỏi này lắm. Bạn có thể hỏi theo cách khác hoặc chọn một gợi ý bên dưới nhé.",
                false,
                defaultSuggestions()
        );
    }

    private int scoreEntry(String q, FaqEntry entry) {
        int score = 0;
        for (String keyword : entry.keywords) {
            String k = normalize(keyword);
            if (q.contains(k)) {
                score += 2;
            } else {
                for (String token : k.split("\\s+")) {
                    if (!token.isEmpty() && q.contains(token)) {
                        score += 1;
                    }
                }
            }
        }
        return score;
    }

    private boolean looksLikeDiagnosisQuestion(String q) {
        return q.contains("toi bi benh gi")
                || q.contains("toi mac gi")
                || q.contains("co phai ung thu")
                || q.contains("co chac la ung thu")
                || q.contains("chan doan")
                || q.contains("chuan doan")
                || q.contains("xac dinh benh");
    }

    private String guessCategory(String q) {
        if (containsAny(q, "nguy co", "ket qua", "cao", "thap", "trung binh")) return "kết quả nguy cơ";
        if (containsAny(q, "tai kiem tra", "kiem tra lai", "follow up", "theo doi")) return "tái kiểm tra";
        if (containsAny(q, "nhac", "reminder", "thong bao")) return "reminder";
        if (containsAny(q, "anh", "chup", "camera", "mo", "toi", "sang")) return "cách chụp ảnh";
        if (containsAny(q, "lich su", "so sanh", "tien trien")) return "lịch sử và so sánh";
        if (containsAny(q, "bac si", "di kham", "kham")) return "khi nào nên đi khám";
        return null;
    }

    private boolean containsAny(String q, String... values) {
        for (String v : values) {
            if (q.contains(normalize(v))) return true;
        }
        return false;
    }

    private List<String> defaultSuggestions() {
        return Arrays.asList(
                "Nguy cơ cao nghĩa là gì?",
                "Tái kiểm tra là gì?",
                "Cách chụp ảnh để kết quả tốt hơn?",
                "Khi nào nên đi khám?"
        );
    }

    private List<String> followupSuggestions(String category) {
        if ("risk".equals(category)) {
            return Arrays.asList(
                    "Nguy cơ trung bình là gì?",
                    "Nguy cơ thấp là gì?",
                    "Khi nào nên đi khám?"
            );
        }
        if ("scan".equals(category)) {
            return Arrays.asList(
                    "Ảnh bị mờ thì làm sao?",
                    "Vì sao app bắt chụp lại?",
                    "Cách chụp ảnh để kết quả tốt hơn?"
            );
        }
        if ("followup".equals(category)) {
            return Arrays.asList(
                    "Tái kiểm tra là gì?",
                    "So sánh hai lần quét để làm gì?",
                    "Nhắc tái kiểm tra dùng để làm gì?"
            );
        }
        if ("limits".equals(category)) {
            return Arrays.asList(
                    "App này có chẩn đoán bệnh không?",
                    "Nguy cơ cao nghĩa là gì?",
                    "Khi nào nên đi khám?"
            );
        }
        return defaultSuggestions();
    }

    private List<String> suggestionsForCategory(String categoryHint) {
        if ("kết quả nguy cơ".equals(categoryHint)) {
            return Arrays.asList(
                    "Nguy cơ cao nghĩa là gì?",
                    "Nguy cơ trung bình là gì?",
                    "Nguy cơ thấp là gì?"
            );
        }
        if ("tái kiểm tra".equals(categoryHint)) {
            return Arrays.asList(
                    "Tái kiểm tra là gì?",
                    "So sánh hai lần quét để làm gì?",
                    "Khi nào cần tái kiểm tra?"
            );
        }
        if ("reminder".equals(categoryHint)) {
            return Arrays.asList(
                    "Reminder dùng để làm gì?",
                    "Nhắc tái kiểm tra là gì?",
                    "Tại sao cần nhắc lại?"
            );
        }
        if ("cách chụp ảnh".equals(categoryHint)) {
            return Arrays.asList(
                    "Cách chụp ảnh để kết quả tốt hơn?",
                    "Ảnh bị mờ thì làm sao?",
                    "Vì sao app bắt chụp lại?"
            );
        }
        if ("lịch sử và so sánh".equals(categoryHint)) {
            return Arrays.asList(
                    "So sánh hai lần quét để làm gì?",
                    "Lịch sử quét dùng để làm gì?",
                    "Tóm tắt tiến triển là gì?"
            );
        }
        return defaultSuggestions();
    }

    private void seedKnowledge() {
        knowledge.add(new FaqEntry(
                "risk_high",
                Arrays.asList(
                        "nguy cơ cao",
                        "kết quả nguy cơ cao",
                        "mức nguy cơ cao",
                        "nguy hiểm cao"
                ),
                "Nguy cơ cao nghĩa là kết quả sàng lọc AI cho thấy tổn thương cần được chú ý hơn. Đây không phải là chẩn đoán xác định, nhưng bạn nên đi khám bác sĩ da liễu sớm nhất có thể.",
                "risk"
        ));

        knowledge.add(new FaqEntry(
                "risk_medium",
                Arrays.asList("nguy cơ trung bình", "trung bình"),
                "Nguy cơ trung bình nghĩa là tổn thương nên được theo dõi sát hơn. Bạn nên tái kiểm tra nếu tổn thương thay đổi hoặc kéo dài.",
                "risk"
        ));

        knowledge.add(new FaqEntry(
                "risk_low",
                Arrays.asList("nguy cơ thấp", "thấp"),
                "Nguy cơ thấp nghĩa là kết quả hiện tại chưa cho thấy mức đáng lo cao. Tuy nhiên bạn vẫn nên theo dõi, và tái kiểm tra nếu tổn thương thay đổi kích thước, màu sắc hoặc bờ viền.",
                "risk"
        ));

        knowledge.add(new FaqEntry(
                "followup_meaning",
                Arrays.asList("tái kiểm tra", "kiem tra lai", "follow up", "theo doi"),
                "Tái kiểm tra là lần quét tiếp theo của cùng một hồ sơ để so sánh với lần trước. Nó giúp bạn theo dõi xem nguy cơ đang ổn định, tăng lên hay cải thiện.",
                "followup"
        ));

        knowledge.add(new FaqEntry(
                "reminder_meaning",
                Arrays.asList("reminder", "nhac", "nhắc tái kiểm tra", "thong bao"),
                "Reminder giúp bạn nhớ thời điểm nên tái kiểm tra. Nó hữu ích khi bạn muốn theo dõi tổn thương theo định kỳ.",
                "followup"
        ));

        knowledge.add(new FaqEntry(
                "photo_tips",
                Arrays.asList("cách chụp", "chụp ảnh", "camera", "ảnh mờ", "ánh sáng"),
                "Để kết quả tốt hơn, bạn nên chụp nơi đủ sáng, giữ máy chắc tay, đưa vùng da vào giữa khung hình và tránh ảnh quá tối, quá sáng hoặc bị mờ.",
                "scan"
        ));

        knowledge.add(new FaqEntry(
                "retake_reason",
                Arrays.asList("vì sao chụp lại", "app bắt chụp lại", "không phải da", "ảnh lỗi"),
                "Nếu app yêu cầu chụp lại, thường là vì ảnh chưa đủ rõ, quá tối, quá sáng hoặc không chứa đủ vùng da để phân tích.",
                "scan"
        ));

        knowledge.add(new FaqEntry(
                "compare_meaning",
                Arrays.asList("so sánh", "compare", "hai lần quét", "tiến triển"),
                "Chức năng so sánh giúp bạn xem sự thay đổi giữa hai lần quét, từ đó theo dõi diễn tiến theo thời gian thay vì chỉ xem một kết quả đơn lẻ.",
                "followup"
        ));

        knowledge.add(new FaqEntry(
                "doctor_visit",
                Arrays.asList("khi nào đi khám", "đi khám", "bác sĩ", "da liễu"),
                "Bạn nên đi khám bác sĩ da liễu nếu kết quả ở mức nguy cơ cao, hoặc nếu tổn thương thay đổi rõ theo thời gian, chảy máu, đau, loét hoặc khiến bạn lo lắng.",
                "limits"
        ));

        knowledge.add(new FaqEntry(
                "app_limit",
                Arrays.asList("app này có chẩn đoán không", "thay bác sĩ", "có chẩn đoán bệnh không"),
                "App này chỉ hỗ trợ sàng lọc và theo dõi, không thay thế chẩn đoán của bác sĩ.",
                "limits"
        ));
    }

    private String normalize(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replace('đ', 'd');
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}
