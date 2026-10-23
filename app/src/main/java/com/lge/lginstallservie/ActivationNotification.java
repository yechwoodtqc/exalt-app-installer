package com.lge.lginstallservie;

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
 * It points directly at Android's system-owned device-admin confirmation.
 * No YW Activity is used as an intermediary.
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
                        "User-requested shortcut to Android's device-admin confirmation.");
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

        ComponentName adminComponent = new ComponentName(context, AdminReceiver.class);
        Intent systemAdminConfirmation = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        systemAdminConfirmation.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        systemAdminConfirmation.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "YW app installer is requesting legacy device-administrator activation. "
                        + "This version declares no device-admin policy powers and does not "
                        + "request password changes, forced locking, data wiping, camera control, "
                        + "or device-owner status. Approve only if you want to enable this "
                        + "admin component.");
        // Important: do not set FLAG_ACTIVITY_NEW_TASK on this system Intent.

        PendingIntent adminConfirmationPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID + 1,
                systemAdminConfirmation,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("YW app installer")
                .setContentText("Tap to open Android's device-admin activation confirmation.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "This opens Android's system-owned confirmation directly. "
                                + "Activation still requires your approval."))
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
