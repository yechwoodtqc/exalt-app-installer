package com.lge.lgmdmservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Builds the one activation notification used by the Accounts/Sync trigger.
 * Its main action opens Android's admin confirmation directly. The separate
 * update action calls a background service and never launches a YW Activity.
 */
public final class ActivationNotification {
    private static final int NOTIFICATION_ID = 2301;
    private static final String CHANNEL_ID = "admin_activation_reminder";

    private ActivationNotification() { }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "App update",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(
                        "User-requested app update action.");
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showActivationNotification(Context context) {
        createNotificationChannel(context);
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) return;

        Intent update = new Intent(context, InstallUpdateService.class);
        update.setAction(InstallUpdateService.ACTION_INSTALL_LOCAL_UPDATE);
        PendingIntent updateIntent = PendingIntent.getService(
                context, NOTIFICATION_ID + 2, update,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("YW app installer")
                .setContentText("Install the update from device storage.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "Tap Install update to install the prepared YWAI.apk update."))
                .setAutoCancel(true)
                .addAction(android.R.drawable.stat_sys_download_done,
                        "Install update", updateIntent);

        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
