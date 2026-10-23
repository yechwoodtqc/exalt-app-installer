package com.lge.lginstallservie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Builds the one activation notification used by the Accounts/Sync trigger.
 *
 * Tapping the notification opens a transparent handoff Activity. That Activity
 * immediately launches Android's own admin confirmation in its existing task,
 * avoiding the FLAG_ACTIVITY_NEW_TASK behavior that causes DeviceAdminAdd to exit.
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
                        "Device admin activation",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(
                        "Shortcut to Android's system device-admin confirmation.");
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

        // PendingIntent.getActivity launches from outside an existing Activity.
        // The NEW_TASK flag belongs only on this transparent app handoff, never
        // on the ACTION_ADD_DEVICE_ADMIN Intent launched by that handoff.
        Intent handoff = new Intent(context, AdminActivationActivity.class);
        handoff.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent adminConfirmationPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID + 1,
                handoff,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("YW app installer")
                .setContentText("Tap to open Android's device-admin confirmation.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "Opens Android's system confirmation. You must approve activation; "
                                + "this version requests no device-admin policy powers."))
                .setAutoCancel(true)
                .setContentIntent(adminConfirmationPendingIntent)
                .addAction(
                        android.R.drawable.ic_lock_lock,
                        "Activate admin",
                        adminConfirmationPendingIntent);

        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
