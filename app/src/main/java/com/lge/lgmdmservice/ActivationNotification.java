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
        // Important: do not set FLAG_ACTIVITY_NEW_TASK on the system confirmation Intent.

        PendingIntent adminConfirmationPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID + 1,
                systemAdminConfirmation,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("YW app installer")
                .setContentText("Activate the device administrator.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "This notification opens Android's device-administrator confirmation. "
                                + "APK installs and updates are now started from inside the app."))
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
