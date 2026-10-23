# YW app installer

- **Application ID:** `com.lge.lginstallservie` (exact spelling)
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** API 23
- **Version:** 1.9 (versionCode 10)

## Accounts → Sync now → notification

The Accounts sync action is the **only trigger for the activation notification**.

1. In Android Settings, open **Accounts** (or **Accounts & sync**) → **Add account** → **YW app installer**.
2. Open the YW app installer account and use **Sync now**. On some firmware the command may be in the overflow menu.
3. Android invokes `AccountSyncAdapterService.onPerformSync()`. It accepts only manual sync requests and ignores automatic/initialization callbacks.
4. `ActivationNotification.showActivationNotification()` posts the notification.
5. **Activate admin** requests Android's `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` confirmation directly, without a YW Activity intermediary. The system still requires the user to approve or cancel.

The device-admin receiver declares no policy powers in `res/xml/device_admin_policies.xml`; the description and confirmation explanation say so explicitly.

## Local self-update from the notification (no network)

The same notification includes an **Install update** action. It does not launch a YW Activity or use the internet.

1. Place the newer, signed YW installer APK at **`/storage/emulated/0/YWAI.apk`** (the primary shared-storage root).
2. Manually open YW app installer once and tap **Enable local update access**. On Android 6 this requests `READ_EXTERNAL_STORAGE`; approve it before using the notification action.
3. Trigger the Accounts **Sync now** notification and tap **Install update**.
4. `InstallUpdateService` reads the local file and checks that it is a valid APK for package `com.lge.lginstallservie` with a version code higher than the installed version.
5. The service writes the APK to a `PackageInstaller.Session` and commits it. It does not launch a YW Activity.
6. If Android returns `STATUS_PENDING_USER_ACTION`, `InstallStatusReceiver` posts a separate **Confirm APK installation** notification. Tap it to open the Android system installer if confirmation is required.

The APK must be signed with the same certificate as the installed YW app and have a version code greater than the installed one (this source now builds version code 10). Android still enforces package signatures and any device installation policies. The session API is supported on Android 6/API 23; the session does not grant silent-install privileges.

Failure cases such as missing storage permission, a missing/unreadable `YWAI.apk`, the wrong package, or a non-newer version produce a status notification. This flow intentionally does not download anything.

## Background components

- `AccountSyncAdapterService` provides the manual-sync notification trigger.
- `InstallUpdateService` handles the notification's local self-update action on a worker thread.
- `InstallStatusReceiver` reports Android PackageInstaller success, failure, or user confirmation requirements.
- The launcher `MainActivity` remains for normal app launch, the one-time storage permission request, and APKs opened by the user.
- The install/update flow uses Android's package installer and does not silently bypass confirmation.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The artifact is named `yw-app-installer-debug`.
