package com.lge.lginstallservice;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView message = new TextView(this);
        message.setText("YW app installer\n\n"
                + "The device-admin activation notification is triggered through Settings > "
                + "Accounts > YW app installer > Sync now. Its main action opens Android's "
                + "system-owned device-admin confirmation directly.\n\n"
                + "The notification's Install update action reads a local APK at "
                + "/storage/emulated/0/YWAI.apk and submits it to Android PackageInstaller "
                + "without launching a YW Activity. Android may still require confirmation. "
                + "No internet connection is used for this update.\n\n"
                + "This Android 6-compatible build requests shared-storage access at install "
                + "time so the notification action does not need to open an Activity.");
        layout.addView(message);

        Button settingsButton = new Button(this);
        settingsButton.setText("Open notification settings");
        settingsButton.setOnClickListener(v -> openNotificationSettings());
        layout.addView(settingsButton);

        TextView status = new TextView(this);
        status.setPadding(0, padding / 2, 0, 0);
        if (Build.VERSION.SDK_INT >= 24) {
            NotificationManager manager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            boolean enabled = manager != null && manager.areNotificationsEnabled();
            status.setText(enabled
                    ? "App notifications are enabled. Manual account sync triggers the "
                            + "notification with both admin and local-update actions."
                    : "App notifications are currently blocked. Enable them in app settings.");
        } else {
            status.setText("If no notification appears, check this app's notification settings.");
        }
        layout.addView(status);
        setContentView(layout);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri apkUri = getIntent().getData();
            if (apkUri != null) installApk(apkUri);
            else Toast.makeText(this, "No APK file was provided.", Toast.LENGTH_LONG).show();
        }
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private void installApk(Uri apkUri) {
        PackageInstaller.Session session = null;
        try {
            ContentResolver resolver = getContentResolver();
            InputStream input = resolver.openInputStream(apkUri);
            if (input == null) throw new IllegalStateException("Cannot read the selected APK.");

            PackageInstaller installer = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(
                            PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = installer.createSession(params);
            session = installer.openSession(sessionId);

            try (InputStream source = input;
                 OutputStream output = session.openWrite("base.apk", 0, -1)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
                session.fsync(output);
            }

            Intent statusIntent = new Intent(this, InstallStatusReceiver.class);
            statusIntent.setAction("com.lge.lginstallservice.INSTALL_STATUS." + sessionId);
            PendingIntent statusPendingIntent = PendingIntent.getBroadcast(
                    this, sessionId, statusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));
            session.commit(statusPendingIntent.getIntentSender());
            Toast.makeText(this, "APK sent to Android PackageInstaller.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not start APK installation: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            if (session != null) {
                try { session.abandon(); } catch (Exception ignored) { }
            }
        }
    }
}
