package com.example.skincancerai;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!AppPreferences.isAppNotificationsEnabled(context)) {
            Log.w(TAG, "App notifications disabled → skip");
            return;
        }

        String profileId = intent.getStringExtra("profileId");
        String checkId = intent.getStringExtra("checkId");
        String resultLabel = intent.getStringExtra("resultLabel");

        Log.d(TAG, "Receiver fired: profileId=" + profileId + ", checkId=" + checkId);

        ReminderService.createNotificationChannel(context);

        // 👉 Mở lịch sử khi click
        Intent openIntent = new Intent(context, HistoryActivity.class);
        openIntent.putExtra("openProfileId", profileId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                ReminderService.notificationId(profileId, checkId),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ======================
        // TEXT
        // ======================
        String title = "Đến giờ tái quét";

        String normalized = TextSanitizer.normalizeResultLabel(resultLabel);
        String body;

        if (TextSanitizer.isHighRisk(normalized)) {
            body = "Nhắc bạn kiểm tra lại hồ sơ có kết quả nguy cơ cao.";
        } else if (TextSanitizer.isMediumRisk(normalized)) {
            body = "Nhắc bạn theo dõi lại hồ sơ có kết quả nguy cơ trung bình.";
        } else {
            body = "Nhắc bạn tái quét để tiếp tục theo dõi tổn thương da.";
        }

        Bitmap appLargeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_app);

        Person bot = new Person.Builder()
                .setName("SkinHealth")
                .build();

        NotificationCompat.MessagingStyle style =
                new NotificationCompat.MessagingStyle(bot)
                        .setConversationTitle("Nhắc tái quét")
                        .addMessage(body, System.currentTimeMillis(), bot);

        // ======================
        // BUILD NOTIFICATION
        // ======================
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, ReminderService.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_app)
                        .setLargeIcon(appLargeIcon)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setSubText("Nhắc theo dõi hồ sơ")
                        .setStyle(style)
                        .setWhen(System.currentTimeMillis())
                        .setShowWhen(true)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Android < 8
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri soundUri = Settings.System.DEFAULT_NOTIFICATION_URI;
            builder.setSound(soundUri);
            builder.setVibrate(new long[]{0, 250, 180, 250});
        }

        // Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No POST_NOTIFICATIONS permission");
                return;
            }
        }

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "System notification disabled");
            return;
        }

        if (ReminderService.isReminderChannelBlocked(context)) {
            Log.w(TAG, "Channel blocked");
            return;
        }

        int notifyId = ReminderService.notificationId(profileId, checkId);

        try {
            manager.notify(notifyId, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Notify failed", e);
        }

        Log.d(TAG, "Notification posted");
    }
}
