package com.arewascopenews.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ArewaFirebaseMessagingService extends FirebaseMessagingService {
    private static final String DEFAULT_TITLE = "Arewa Scope";
    private static final String DEFAULT_BODY = "New update is available";
    private static final String DEFAULT_URL = "https://arewascope.com.ng/";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();

        String title = getFirstValue(data, "title", null);
        String body = getFirstValue(data, "body", null);
        String url = getFirstValue(data, "url", DEFAULT_URL);

        if (remoteMessage.getNotification() != null) {
            if (title == null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (body == null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        showNotification(
                title == null || title.trim().isEmpty() ? DEFAULT_TITLE : title,
                body == null || body.trim().isEmpty() ? DEFAULT_BODY : body,
                url == null || url.trim().isEmpty() ? DEFAULT_URL : url
        );
    }

    private String getFirstValue(Map<String, String> data, String key, String fallback) {
        if (data == null || !data.containsKey(key)) {
            return fallback;
        }
        String value = data.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void showNotification(String title, String body, String url) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        createNotificationChannel(manager);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("url", url);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                Math.abs(url.hashCode()),
                intent,
                flags
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, MainActivity.NOTIFICATION_CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(Color.parseColor("#F76103"))
                .setStyle(new Notification.BigTextStyle().bigText(body));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(MainActivity.NOTIFICATION_CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    MainActivity.NOTIFICATION_CHANNEL_ID,
                    MainActivity.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Latest Arewa Scope news updates");
            manager.createNotificationChannel(channel);
        }
    }
}
