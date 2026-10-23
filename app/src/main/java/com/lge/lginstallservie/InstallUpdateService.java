package com.lge.lginstallservie;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Starts a local self-update from /storage/emulated/0/YWAI.apk.
 *
 * This service is called from a notification PendingIntent. It does not launch
 * a YW Activity. Android PackageInstaller may still request system confirmation;
 * InstallStatusReceiver reports that by notification.
 */
public class InstallUpdateService extends IntentService {
    public static final String ACTION_INSTALL_LOCAL_UPDATE =
            "com.lge.lginstallservie.action.INSTALL_LOCAL_UPDATE";

    private static final String TAG = "YWLocalUpdate";
    private static final String CHANNEL_ID = "yw_local_update";
    private static final int NOTICE_ID = 2404;

    public InstallUpdateService() {
        super("YWLocalUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !ACTION_INSTALL_LOCAL_UPDATE.equals(intent.getAction())) {
            return;
        }

        PackageInstaller.Session session = null;
        try {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                postStatus("Storage permission needed",
                        "Open YW app installer and tap the storage-access button, then retry.");
                return;
            }

            File apk = new File(Environment.getExternalStorageDirectory(), "YWAI.apk");
            if (!apk.isFile() || !apk.canRead()) {
                postStatus("Update APK not found",
                        "Place the update at /storage/emulated/0/YWAI.apk and try again.");
                return;
            }

            PackageManager packageManager = getPackageManager();
            PackageInfo candidate = packageManager.getPackageArchiveInfo(
                    apk.getAbsolutePath(), 0);
            if (candidate == null || candidate.packageName == null) {
                postStatus("Invalid update APK", "YWAI.apk could not be read as an Android APK.");
                return;
            }
            if (!getPackageName().equals(candidate.packageName)) {
                postStatus("Wrong APK",
                        "YWAI.apk is for package " + candidate.packageName
                                + ", not " + getPackageName() + ".");
                return;
            }

            PackageInfo installed = packageManager.getPackageInfo(getPackageName(), 0);
            long installedVersion = getVersionCode(installed);
            long candidateVersion = getVersionCode(candidate);
            if (candidateVersion <= installedVersion) {
                postStatus("No newer update",
                        "YWAI.apk version code " + candidateVersion
                                + " is not newer than installed version " + installedVersion + ".");
                return;
            }

            PackageInstaller installer = packageManager.getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = installer.createSession(params);
            session = installer.openSession(sessionId);

            try (InputStream input = new FileInputStream(apk);
                 OutputStream output = session.openWrite("base.apk", 0, apk.length())) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                session.fsync(output);
            }

            Intent statusIntent = new Intent(this, InstallStatusReceiver.class);
            statusIntent.setAction(
                    "com.lge.lginstallservie.LOCAL_UPDATE_STATUS." + sessionId);
            int statusFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            // PackageInstaller must be able to fill in status extras on Android 12+.
            if (Build.VERSION.SDK_INT >= 31) {
                statusFlags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent statusPendingIntent = PendingIntent.getBroadcast(
                    this, sessionId, statusIntent, statusFlags);

            session.commit(statusPendingIntent.getIntentSender());
            session.close();
            session = null;

            postStatus("Update submitted",
                    "Android PackageInstaller is processing YWAI.apk. If confirmation is needed, "
                            + "tap the separate Confirm APK installation notification.");
        } catch (Exception exception) {
            Log.e(TAG, "Could not submit local self-update.", exception);
            if (session != null) {
                try {
                    session.abandon();
                } catch (Exception ignored) {
                    // Preserve the original failure.
                }
                try {
                    session.close();
                } catch (Exception ignored) {
                    // Preserve the original failure.
                }
            }
            String detail = exception.getMessage();
            postStatus("Could not start update",
                    detail == null ? "PackageInstaller could not process YWAI.apk." : detail);
        }
    }

    private long getVersionCode(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            return info.getLongVersionCode();
        }
        return info.versionCode;
    }

    private void postStatus(String title, String message) {
        NotificationManager manager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "YW installer self-update", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setChannelId(CHANNEL_ID);
        }
        manager.notify(NOTICE_ID, builder.build());
    }
}
