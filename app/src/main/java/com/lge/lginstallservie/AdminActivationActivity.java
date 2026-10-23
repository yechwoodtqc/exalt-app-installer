package com.lge.lginstallservie;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Transparent task handoff for Android's device-admin confirmation UI.
 *
 * Notifications launch activities outside an existing Activity task. Android's
 * legacy DeviceAdminAdd screen rejects launches carrying FLAG_ACTIVITY_NEW_TASK.
 * This transparent Activity is launched in its own task, then starts the system
 * confirmation with startActivityForResult() and a fresh Intent with no flags.
 * It has no app UI; the system-owned confirmation is what the user interacts with.
 */
public final class AdminActivationActivity extends Activity {
    private static final String TAG = "YWAdminActivation";
    private static final int REQUEST_ADD_DEVICE_ADMIN = 2302;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Avoid launching the consent screen a second time after configuration
        // changes or Activity recreation.
        if (savedInstanceState != null) {
            finish();
            return;
        }

        Intent confirmation = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        confirmation.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                new ComponentName(this, AdminReceiver.class));
        confirmation.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.admin_activation_explanation));
        // Intentionally do not set FLAG_ACTIVITY_NEW_TASK on this system Intent.
        try {
            startActivityForResult(confirmation, REQUEST_ADD_DEVICE_ADMIN);
        } catch (ActivityNotFoundException | SecurityException exception) {
            Log.e(TAG, "Could not launch Android device-admin confirmation.", exception);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_DEVICE_ADMIN) {
            DevicePolicyManager manager = (DevicePolicyManager)
                    getSystemService(DEVICE_POLICY_SERVICE);
            boolean active = manager != null && manager.isAdminActive(
                    new ComponentName(this, AdminReceiver.class));
            Log.i(TAG, "System admin confirmation returned; resultCode="
                    + resultCode + ", adminActive=" + active);
            finish();
        }
    }
}
