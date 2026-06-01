package com.example.cuoiky_qllichhoctap;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class EventReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String CHANNEL_ID = "study_event_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannel(context);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        String safeMessage = message == null || message.isEmpty() ? "Bạn có một lịch sắp bắt đầu." : message;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_calendar)
                .setContentTitle(title == null || title.isEmpty() ? "Sắp đến lịch học" : title)
                .setContentText(safeMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(safeMessage))
                .setContentIntent(openAppIntent(context, eventId))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(notificationId(eventId), builder.build());
    }

    private PendingIntent openAppIntent(Context context, String eventId) {
        Intent launchIntent = new Intent(context, MainActivity.class)
                .setAction("com.example.cuoiky_qllichhoctap.OPEN_FROM_REMINDER")
                .putExtra(EXTRA_EVENT_ID, eventId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, notificationId(eventId) + 9000, launchIntent, flags);
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Nhắc lịch học", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Nhắc trước lịch học, lịch thi, deadline và việc cá nhân");
        manager.createNotificationChannel(channel);
    }

    public static int notificationId(String eventId) {
        return eventId == null ? 3000 : Math.abs(eventId.hashCode());
    }
}
