package com.lge.lgmdmservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

/**
 * Handles installation results for APKs selected inside MainActivity.
 * When Android requires user approval, open the system installer directly
 * instead of routing ordinary installs through an app-generated notification.
 */
public class InstallStatusReceiver extends BroadcastReceiver {
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
}
