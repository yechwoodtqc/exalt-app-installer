# YW app installer

- **Application ID:** `com.lge.lgmdmservice` (exact spelling)
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** 22 (legacy install-time permissions for the Android 6 device)
- **Version:** 2.0 (versionCode 11)

## Accounts → Sync now → notification

The Accounts sync action is the **only trigger for the activation notification**.

1. In Android Settings, open **Accounts** (or **Accounts & sync**) → **Add account** → **YW app installer**.
2. Open the YW app installer account and use **Sync now**.
3. Android invokes `AccountSyncAdapterService.onPerformSync()`. It accepts only manual sync requests and ignores automatic/initialization callbacks.
4. `ActivationNotification.showActivationNotification()` posts the notification.
5. **Activate admin** requests Android's `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` confirmation directly, without a YW Activity intermediary. The system still requires the user to approve or cancel.

The device-admin receiver declares no policy powers in `res/xml/device_admin_policies.xml`; the description and confirmation explanation say so explicitly.

## Local self-update from the notification (no network and no YW Activity)

The same notification includes an **Install update** action. It does not launch a YW Activity and does not connect to the internet.

1. Put the newer YW Installer APK at **`/storage/emulated/0/YWAI.apk`** (the primary shared-storage root).
2. This version targets API 22 so Android 6 uses the legacy install-time permission model for declared dangerous permissions. That means the notification action does not need to launch an Activity to request `READ_EXTERNAL_STORAGE`.
3. Trigger the Accounts **Sync now** notification and tap **Install update**.
4. `InstallUpdateService` reads the local file and checks that it is a valid APK for package `com.lge.lgmdmservice` with a version code higher than the installed version and signing identity consistent with the installed APK.
5. The service writes the APK to a `PackageInstaller.Session` and commits it. It does not launch a YW Activity.
6. If Android returns `STATUS_PENDING_USER_ACTION`, `InstallStatusReceiver` posts a separate **Confirm APK installation** notification. Tap it to open Android's system installer if confirmation is required.

**One-time bootstrap caveat:** if the currently installed build has not been granted shared-storage access, it cannot read `YWAI.apk` to update itself to this target-22 build. Install this version once through the normal Android APK installer. The legacy permission mode then lets future notification-triggered updates read the shared-storage file without a YW Activity permission prompt.

The APK must be signed with the same certificate as the installed YW app and have a higher version code. Android still enforces package signatures and installation policy; a session does not grant silent-install privileges.

Missing/unreadable `YWAI.apk`, wrong package, signing mismatch, or an equal/lower version code produce a status notification. This flow intentionally does not download anything.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The artifact is named `yw-app-installer-debug`.
