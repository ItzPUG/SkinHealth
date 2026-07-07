package com.example.skincancerai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class VnExpressNewsScraper {

    private static final String TAG_URL = "https://vnexpress.net/tag/benh-da-98294";

    public List<WebNewsItem> fetchSkinNews() throws Exception {
        List<WebNewsItem> result = new ArrayList<>();

        Document tagDoc = Jsoup.connect(TAG_URL)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get();

        Set<String> articleUrls = new LinkedHashSet<>();

        // Lấy tất cả link bài viết từ trang tag
        Elements links = tagDoc.select("h2 a, h3 a, a[href*='.html']");
        for (Element link : links) {
            String href = link.absUrl("href");
            if (href == null || href.trim().isEmpty()) continue;
            if (!href.contains("vnexpress.net")) continue;
            if (!href.endsWith(".html")) continue;
            articleUrls.add(href);
            if (articleUrls.size() >= 12) break;
        }

        for (String articleUrl : articleUrls) {
            try {
                WebNewsItem item = parseArticle(articleUrl);
                if (item != null) {
                    result.add(item);
                }
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private WebNewsItem parseArticle(String articleUrl) throws Exception {
        Document doc = Jsoup.connect(articleUrl)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get();

        String title = firstNonEmpty(
                meta(doc, "property", "og:title"),
                text(doc, "h1"),
                text(doc, "title")
        );

        String summary = firstNonEmpty(
                meta(doc, "property", "og:description"),
                text(doc, "p.description"),
                text(doc, "meta[name=description]")
        );

        String imageUrl = firstNonEmpty(
                meta(doc, "property", "og:image"),
                attr(doc, "figure img", "abs:src"),
                attr(doc, "img", "abs:src")
        );

        String dateText = firstNonEmpty(
                text(doc, ".date"),
                text(doc, ".header-content .date"),
                ""
        );

        String category = firstNonEmpty(
                text(doc, ".breadcrumb li:last-child"),
                text(doc, ".menu-breadcrumb a:last-child"),
                "Bệnh da"
        );

        String content = extractContent(doc);

        if (title == null || title.trim().isEmpty()) return null;

        return new WebNewsItem(
                title.trim(),
                safe(summary),
                safe(content),
                safe(dateText),
                safe(category),
                articleUrl,
                safe(imageUrl)
        );
    }

    private String extractContent(Document doc) {
        Elements paragraphs = doc.select("article p, .fck_detail p, .Normal");
        StringBuilder sb = new StringBuilder();

        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.isEmpty()) continue;

            // bỏ caption ảnh quá ngắn nếu muốn
            if (text.startsWith("Ảnh:") && text.length() < 40) continue;

            if (sb.length() > 0) sb.append("\n\n");
            sb.append(text);
        }

        return sb.toString().trim();
    }

    private String meta(Document doc, String attrKey, String attrValue) {
        Element el = doc.selectFirst("meta[" + attrKey + "=" + attrValue + "]");
        if (el == null) return "";
        return el.attr("content");
    }

    private String text(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el != null ? el.text() : "";
    }

    private String attr(Document doc, String selector, String attr) {
        Element el = doc.selectFirst(selector);
        return el != null ? el.attr(attr) : "";
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
