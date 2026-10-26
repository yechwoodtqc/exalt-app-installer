package com.lge.lgmdmservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

/**
 * In-app picker installs open Android's confirmation screen directly.
 * Installs initiated through the Accounts notification keep a notification
 * confirmation step, since the user may have left the app.
 */
public class InstallStatusReceiver extends BroadcastReceiver {
    private static final String UPDATE_ACTION_PREFIX =
            "com.lge.lgmdmservice.LOCAL_UPDATE_STATUS.";
    private static final String CHANNEL_ID = "apk_install_status";
    private static final int NOTIFICATION_ID = 2401;

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation == null) {
                Toast.makeText(context, "Android did not provide an install confirmation screen.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (intent.getAction() != null
                    && intent.getAction().startsWith(UPDATE_ACTION_PREFIX)) {
                showConfirmationNotification(context, confirmation);
            } else {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirmation);
                } catch (RuntimeException exception) {
                    Toast.makeText(context,
                            "Android could not open the install confirmation. Return to the app and retry.",
                            Toast.LENGTH_LONG).show();
                }
            }
            return;
        }

        if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(context, "APK installed successfully.",
                    Toast.LENGTH_LONG).show();
        } else {
            String detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            String message = "APK installation failed";
            if (detail != null && !detail.trim().isEmpty()) {
                message += ": " + detail;
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    private void showConfirmationNotification(Context context, Intent confirmation) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "APK installation", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 2402, confirmation, pendingFlags);

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Confirm APK installation")
                .setContentText("Tap to continue in Android's installation screen.")
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
