package com.lge.lgmdmservice;

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
import android.content.pm.Signature;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Starts a local self-update from the primary shared-storage root /YWAI.apk.
 * Notification action -> service -> PackageInstaller; no YW Activity is launched.
 * Android may still request the user's confirmation.
 */
public class InstallUpdateService extends IntentService {
    public static final String ACTION_INSTALL_LOCAL_UPDATE =
            "com.lge.lgmdmservice.action.INSTALL_LOCAL_UPDATE";

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

        PackageInstaller installer = null;
        PackageInstaller.Session session = null;
        int sessionId = -1;
        try {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                postStatus("Storage access unavailable",
                        "This build expects Android's install-time storage permission. Install this "
                                + "version once with Android's package installer and approve its "
                                + "requested permissions, then retry the notification action.");
                return;
            }

            File apk = new File(Environment.getExternalStorageDirectory(), "YWAI.apk");
            if (!apk.isFile() || !apk.canRead() || apk.length() <= 0) {
                postStatus("Update APK not found",
                        "Place a readable, non-empty update at /storage/emulated/0/YWAI.apk.");
                return;
            }

            PackageManager packageManager = getPackageManager();
            // GET_SIGNATURES is retained for compatibility with this API-23 app.
            PackageInfo candidate = packageManager.getPackageArchiveInfo(
                    apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            if (candidate == null || candidate.packageName == null) {
                postStatus("Invalid update APK", "YWAI.apk could not be parsed as a signed APK.");
                return;
            }
            if (!getPackageName().equals(candidate.packageName)) {
                postStatus("Wrong APK",
                        "YWAI.apk is for package " + candidate.packageName
                                + ", not " + getPackageName() + ".");
                return;
            }

            PackageInfo installed = packageManager.getPackageInfo(
                    getPackageName(), PackageManager.GET_SIGNATURES);
            long installedVersion = getVersionCode(installed);
            long candidateVersion = getVersionCode(candidate);
            if (candidateVersion <= installedVersion) {
                postStatus("No newer update",
                        "YWAI.apk version code " + candidateVersion
                                + " is not newer than installed version " + installedVersion + ".");
                return;
            }

            if (installed.signatures != null && installed.signatures.length > 0
                    && candidate.signatures != null && candidate.signatures.length > 0
                    && !sameSigners(installed.signatures, candidate.signatures)) {
                postStatus("Signing key mismatch",
                        "YWAI.apk is not signed with the same certificate as the installed app. "
                                + "Android will reject it as an update.");
                return;
            }

            installer = packageManager.getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            // These hints are API 21+, so they are supported on Android 6/API 23.
            params.setAppPackageName(getPackageName());
            params.setSize(apk.length());

            sessionId = installer.createSession(params);
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
                    "com.lge.lgmdmservice.LOCAL_UPDATE_STATUS." + sessionId);
            int statusFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            // Android 12+ needs a mutable callback because PackageInstaller adds status extras.
            if (Build.VERSION.SDK_INT >= 31) {
                statusFlags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent statusPendingIntent = PendingIntent.getBroadcast(
                    this, sessionId, statusIntent, statusFlags);

            session.commit(statusPendingIntent.getIntentSender());
            session.close();
            session = null;

            postStatus("Update submitted",
                    "Android PackageInstaller is processing YWAI.apk. If it asks for confirmation, "
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
            } else if (sessionId >= 0 && installer != null) {
                try {
                    installer.abandonSession(sessionId);
                } catch (Exception ignored) {
                    // The session may already have been committed or removed.
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

    private boolean sameSigners(Signature[] installed, Signature[] candidate) {
        if (installed.length != candidate.length) return false;
        boolean[] matched = new boolean[candidate.length];
        for (Signature installedSigner : installed) {
            boolean found = false;
            for (int i = 0; i < candidate.length; i++) {
                if (!matched[i] && installedSigner.equals(candidate[i])) {
                    matched[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
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
