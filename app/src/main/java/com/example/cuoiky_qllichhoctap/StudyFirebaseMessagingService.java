package com.example.cuoiky_qllichhoctap;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.cuoiky_qllichhoctap.data.AdminPortalClient;
import com.example.cuoiky_qllichhoctap.data.AuthRepository;
import com.example.cuoiky_qllichhoctap.model.AuthUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class StudyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "admin_announcements";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        String email = currentEmail();
        if (!TextUtils.isEmpty(email)) {
            new AdminPortalClient().syncFcmToken(email, token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String title = "Thông báo từ quản trị";
        String body = "";
        if (message.getNotification() != null) {
            if (!TextUtils.isEmpty(message.getNotification().getTitle())) {
                title = message.getNotification().getTitle();
            }
            body = message.getNotification().getBody();
        }
        if (TextUtils.isEmpty(body)) {
            body = message.getData().get("body");
        }
        if (TextUtils.isEmpty(body)) {
            body = "Bạn có thông báo mới từ web quản trị.";
        }
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannel();
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                Math.abs((title + body).hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_study_planner)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(this).notify(Math.abs((title + body).hashCode()), builder.build());
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Thông báo quản trị", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Thông báo được gửi từ web quản trị Study Planner");
        manager.createNotificationChannel(channel);
    }

    private String currentEmail() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getEmail())) {
            return firebaseUser.getEmail();
        }
        AuthUser localUser = new AuthRepository(this).getCurrentUser();
        return localUser == null ? "" : localUser.getEmail();
    }
}
