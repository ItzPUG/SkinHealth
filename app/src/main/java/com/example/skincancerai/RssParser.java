package com.example.skincancerai;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RssParser {

    public static List<News> parse(String rssUrl, String sourceName) {
        List<News> list = new ArrayList<>();

        try {
            InputStream is = new URL(rssUrl).openStream();
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, "UTF-8");

            boolean insideItem = false;
            String title = null, link = null, desc = null;

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {

                String tag = parser.getName();

                if (event == XmlPullParser.START_TAG) {
                    if ("item".equalsIgnoreCase(tag)) {
                        insideItem = true;
                    } else if (insideItem) {
                        if ("title".equalsIgnoreCase(tag)) {
                            title = parser.nextText();
                        } else if ("link".equalsIgnoreCase(tag)) {
                            link = parser.nextText();
                        } else if ("description".equalsIgnoreCase(tag)) {
                            desc = parser.nextText();
                        }
                    }
                } else if (event == XmlPullParser.END_TAG && "item".equalsIgnoreCase(tag)) {
                    insideItem = false;

                    if (title != null && link != null) {
                        list.add(new News(
                                title,
                                desc != null ? desc.replaceAll("<.*?>", "") : "",
                                null,
                                sourceName,
                                link
                        ));
                    }
                    title = link = desc = null;
                }
                event = parser.next();
            }

            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
