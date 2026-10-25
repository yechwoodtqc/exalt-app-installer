# YW app installer

- **Application ID:** `com.lge.lgmdmservice`
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** 22 (kept for the legacy Android 6 device)
- **Version:** 2.1 (versionCode 12)

## Main app

The app now has two in-app sections:

1. **Install APK** — choose an APK from the device's document picker. The app streams it into Android's `PackageInstaller` and Android handles package/signature checks and any required user confirmation.
2. **filterLGhacked.com** — opens `https://filterlghacked.com/` inside the app's WebView. JavaScript, DOM storage, cookies and third-party cookies are enabled to support modern web pages and Cloudflare challenges. TLS certificate failures are rejected, not bypassed.

Cloudflare challenge success depends on the device's Android System WebView version, network, cookies and the site's own security policy. This implementation does not spoof clients or bypass Cloudflare security. Older Android 6 WebView builds may be too old for some modern site features.

## Accounts → Sync now notification

The existing Android Accounts trigger and notification flow are intentionally preserved.

1. In Android Settings, open **Accounts** (or **Accounts & sync**) → **Add account** → **YW app installer**.
2. Open the account and use **Sync now**.
3. `AccountSyncAdapterService.onPerformSync()` accepts only manual sync requests and ignores automatic/initialization callbacks.
4. `ActivationNotification.showActivationNotification()` posts the existing notification with **Activate admin** and **Install update** actions.
5. The admin action opens Android's system device-admin confirmation. The receiver declares no device-admin policy powers.
6. The local update action continues to read `/storage/emulated/0/YWAI.apk`, validate package/version/signing identity, and submit the APK to `PackageInstaller`. Android may still require confirmation.

The APK installed through the new in-app picker is separate from the notification's local self-update action. Both paths use Android's PackageInstaller; neither grants silent-install privileges.

## Android package ID note

The package ID is now `com.lge.lgmdmservice`. Android treats this as a different package from earlier builds using a different application ID; it cannot update those installs in place.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The artifact is named `yw-app-installer-debug`.
