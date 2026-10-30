package com.lge.lgmdmservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

/** Handles installation results for APKs selected inside MainActivity. */
public class InstallStatusReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "app_install_results";

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirmation);
                } catch (RuntimeException exception) {
                    Toast.makeText(context,
                            "Android could not open the install confirmation. Return to the app and retry.",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context,
                        "Android did not provide an install confirmation screen.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (status == PackageInstaller.STATUS_SUCCESS) {
            showInstalledNotification(context, intent);
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

    private void showInstalledNotification(Context context, Intent intent) {
        String packageName = intent.getStringExtra("android.content.pm.extra.PACKAGE_NAME");
        String appName = packageName;
        if (packageName != null && !packageName.trim().isEmpty()) {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                appName = pm.getApplicationLabel(info).toString();
            } catch (PackageManager.NameNotFoundException ignored) {
                // The package name is still useful if Android has not exposed its label.
            }
        }
        if (appName == null || appName.trim().isEmpty()) {
            appName = "App";
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "App installation", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications when an app has finished installing");
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification_installed)
                .setContentTitle("App installed")
                .setContentText(appName + " was installed successfully")
                .setStyle(new Notification.BigTextStyle()
                        .bigText(appName + " was installed successfully"))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());

        manager.notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }
}
