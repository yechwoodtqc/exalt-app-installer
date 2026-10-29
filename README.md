# YW app installer

- **Application ID:** `com.lge.lgmdmservice`
- **Minimum Android:** 6.0 (API 23)
- **Version:** 2.6 (versionCode 17)

## App unlock and authenticator setup
On first launch, the app uses the existing access code (`ys90`) until an authenticator is enrolled. Unlock the app, open **Settings → Set up authenticator**, then add the displayed setup key to a TOTP authenticator app and enter its current six-digit code to verify setup. After enrollment, the app uses rotating six-digit authenticator codes at launch instead of the static access code. Reconfiguration is available from Settings. Keep a secure backup of the setup key; losing it may lock you out. The app checks a 30-second TOTP period with a one-step clock tolerance.

## Compact APK installer
The interface is compact. The filterlghacked.com website/WebView is removed. The app no longer has an APK `ACTION_VIEW` intent filter, so APKs opened from file managers will not launch this app; select them through **Choose APK file** inside the app. In-app installs use Android PackageInstaller and required system confirmations.

## Accounts notification path
The existing **Accounts → Sync now** trigger remains. Its notification exposes **Install update** only; the **Activate admin** action has been removed. The local update action reads `/storage/emulated/0/YWAI.apk` and validates package identity, version and signing certificate.

Updates must use the same application ID and signing certificate, with a higher version code.

## Build
GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch.
