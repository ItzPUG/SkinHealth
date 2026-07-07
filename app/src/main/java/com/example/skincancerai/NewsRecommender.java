package com.example.skincancerai;

import java.util.ArrayList;
import java.util.List;

public class NewsRecommender {

    public static List<News> filterByAIResult(
            List<News> allNews,
            String aiLabel
    ) {

        List<News> result = new ArrayList<>();
        String key = aiLabel.toLowerCase();

        for (News n : allNews) {
            String content = (n.title + " " + n.description).toLowerCase();

            if (key.contains("melanoma") || key.contains("ung thư")) {
                if (content.contains("ung thư") || content.contains("u ác"))
                    result.add(n);
            } else if (key.contains("eczema") || key.contains("viêm")) {
                if (content.contains("viêm da") || content.contains("dị ứng"))
                    result.add(n);
            } else {
                if (content.contains("da"))
                    result.add(n);
            }
        }

        return result.isEmpty() ? allNews : result;
    }
}
