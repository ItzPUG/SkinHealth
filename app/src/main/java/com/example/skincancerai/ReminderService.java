package com.example.skincancerai;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ReminderService {

    private static final String TAG = "ReminderService";

    // Đổi CHANNEL_ID mới để tránh dính channel cũ bị silent
    public static final String CHANNEL_ID = "scan_reminder_v4";
    private static final String CHANNEL_NAME = "Nhắc tái quét";
    private static final String CHANNEL_DESC = "Thông báo nhắc người dùng tái quét đúng hẹn";

    private ReminderService() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            Log.d(TAG, "Channel already exists: " + CHANNEL_ID
                    + ", importance=" + existing.getImportance());
            return;
        }

        Uri soundUri = Settings.System.DEFAULT_NOTIFICATION_URI;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(CHANNEL_DESC);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 250, 180, 250});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(soundUri, audioAttributes);

        manager.createNotificationChannel(channel);
        Log.d(TAG, "Created channel: " + CHANNEL_ID);
    }

    public static void scheduleAt(Context context,
                                  String profileId,
                                  String checkId,
                                  String resultLabel,
                                  long triggerAtMillis) {
        createNotificationChannel(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        PendingIntent pendingIntent = buildReminderPendingIntent(
                context,
                profileId,
                checkId,
                resultLabel
        );

        Log.d(TAG, "scheduleAt triggerAtMillis=" + triggerAtMillis);
        Log.d(TAG, "scheduleAt utc=" + new java.util.Date(triggerAtMillis).toString());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                boolean canExact = alarmManager.canScheduleExactAlarms();
                Log.d(TAG, "canScheduleExactAlarms=" + canExact);

                if (canExact) {
                    scheduleWithAlarmClockOrExact(alarmManager, context, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                    Log.w(TAG, "Exact alarm not granted, fallback to setAndAllowWhileIdle()");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scheduleWithAlarmClockOrExact(alarmManager, context, triggerAtMillis, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled with setExact() [KITKAT-L]");
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled with set() [legacy]");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException scheduling alarm, fallback to set()", e);
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }

        scheduleBackupWorker(context, profileId, checkId, resultLabel, triggerAtMillis);
    }

    public static void cancel(Context context, String profileId, String checkId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = buildReminderPendingIntent(
                context,
                profileId,
                checkId,
                null
        );

        alarmManager.cancel(pendingIntent);
        NotificationManagerCompat.from(context).cancel(notificationId(profileId, checkId));
        WorkManager.getInstance(context).cancelUniqueWork(workName(profileId, checkId));
        Log.d(TAG, "Reminder cancelled for profileId=" + profileId + ", checkId=" + checkId);
    }

    private static PendingIntent buildReminderPendingIntent(Context context,
                                                            String profileId,
                                                            String checkId,
                                                            String resultLabel) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("profileId", profileId);
        intent.putExtra("checkId", checkId);
        intent.putExtra("resultLabel", resultLabel);

        return PendingIntent.getBroadcast(
                context,
                notificationId(profileId, checkId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static int notificationId(String profileId, String checkId) {
        String raw = String.valueOf(profileId) + "_" + String.valueOf(checkId);
        return raw.hashCode();
    }

    private static void scheduleWithAlarmClockOrExact(AlarmManager alarmManager,
                                                      Context context,
                                                      long triggerAtMillis,
                                                      PendingIntent operation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent showIntent = new Intent(context, MainActivity.class);
            showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent showPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmClockInfo info = new AlarmClockInfo(triggerAtMillis, showPendingIntent);
            alarmManager.setAlarmClock(info, operation);
            Log.d(TAG, "Scheduled with setAlarmClock()");
            return;
        }

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation
        );
        Log.d(TAG, "Scheduled with setExactAndAllowWhileIdle()");
    }

    private static void scheduleBackupWorker(Context context,
                                             String profileId,
                                             String checkId,
                                             String resultLabel,
                                             long triggerAtMillis) {
        long delayMs = Math.max(0L, triggerAtMillis - System.currentTimeMillis());
        Data data = new Data.Builder()
                .putString("profileId", profileId)
                .putString("checkId", checkId)
                .putString("resultLabel", resultLabel)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(workName(profileId, checkId))
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                workName(profileId, checkId),
                ExistingWorkPolicy.REPLACE,
                request
        );
        Log.d(TAG, "Backup worker scheduled, delayMs=" + delayMs);
    }

    private static String workName(String profileId, String checkId) {
        return "reminder_" + String.valueOf(profileId) + "_" + String.valueOf(checkId);
    }

    public static boolean isReminderChannelBlocked(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return false;
        NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
        return channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
    }
}
