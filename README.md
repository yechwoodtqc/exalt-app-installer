# YW app installer

- **Application ID:** `com.lge.lgmdmservice`
- **Minimum Android:** 6.0 (API 23)
- **Version:** 2.5 (versionCode 16)

## Access code
The app opens with a code gate. Default code: `ys90`. After unlocking, open **Settings** to change it; the current code and confirmation of the new code are required. The code is stored in app-private preferences. This is a basic app-level gate, not device-grade security.

## Compact APK installer
The interface is compact. The filterlghacked.com website/WebView is removed. The app no longer has an APK `ACTION_VIEW` intent filter, so APKs opened from file managers will not launch this app; select them through **Choose APK file** inside the app. In-app installs use Android PackageInstaller and required system confirmations.

## Accounts notification path
The existing **Accounts → Sync now** trigger, The Accounts → Sync now notification now exposes **Install update** only; the **Activate admin** action has been removed. The local update action reads `/storage/emulated/0/YWAI.apk` and should validate package identity, version and signing certificate.

Updates must use the same application ID and signing certificate, with a higher version code.

## Build
GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch.
