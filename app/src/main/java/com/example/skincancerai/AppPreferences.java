package com.example.skincancerai;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPreferences {

    private static final String PREF_NAME = "skincancer_ai_prefs";
    private static final String KEY_APP_NOTIFICATIONS = "app_notifications_enabled";
    private static final String KEY_REMINDER_SOUND = "reminder_sound_enabled";
    private static final String KEY_REMINDER_VIBRATE = "reminder_vibrate_enabled";
    private static final String KEY_NEWS_NOTIFICATIONS = "news_notifications_enabled";
    private static final String KEY_DATA_PROCESSING_CONSENT = "data_processing_consent";

    private AppPreferences() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isAppNotificationsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_APP_NOTIFICATIONS, true);
    }

    public static void setAppNotificationsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_APP_NOTIFICATIONS, enabled).apply();
    }

    public static boolean isReminderSoundEnabled(Context context) {
        return prefs(context).getBoolean(KEY_REMINDER_SOUND, true);
    }

    public static void setReminderSoundEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_REMINDER_SOUND, enabled).apply();
    }

    public static boolean isReminderVibrateEnabled(Context context) {
        return prefs(context).getBoolean(KEY_REMINDER_VIBRATE, true);
    }

    public static void setReminderVibrateEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_REMINDER_VIBRATE, enabled).apply();
    }

    public static boolean isNewsNotificationsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NEWS_NOTIFICATIONS, false);
    }

    public static void setNewsNotificationsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NEWS_NOTIFICATIONS, enabled).apply();
    }

    public static boolean isDataProcessingConsentGranted(Context context) {
        return prefs(context).getBoolean(KEY_DATA_PROCESSING_CONSENT, true);
    }

    public static void setDataProcessingConsent(Context context, boolean granted) {
        prefs(context).edit().putBoolean(KEY_DATA_PROCESSING_CONSENT, granted).apply();
    }
}
