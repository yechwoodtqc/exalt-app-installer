# YW app installer

- **Application ID:** `com.lge.lgmdmservice`
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** 22 (kept for the legacy Android 6 device)
- **Version:** 2.3 (versionCode 14)

## Two install paths

### Install started inside the app

1. Open **Install APK** and tap **Choose APK file**.
2. Select the APK from the device's document picker.
3. The app submits the selected APK to Android `PackageInstaller`.
4. If Android requires approval, the PackageInstaller confirmation screen opens directly. The app does not route this user-initiated install through its own notification. Success/failure is reported in the app with a toast.

Android still enforces signatures, package compatibility, install permissions and user approval. This app does not silently bypass those checks.

### Persistent Accounts notification update path

The **Accounts → Sync now** notification continues to offer **Install update** as a separate option. It reads `/storage/emulated/0/YWAI.apk`, checks that the APK targets this application ID, has a newer version code and matches the installed signing certificate, then submits it to Android `PackageInstaller`. If Android requests confirmation while the user is outside the app, a confirmation notification is posted; tapping it opens Android's system installer.

This is an ongoing supported path, not something removed after the first update. It can be useful for initial/bootstrap updates and for local updates initiated from the notification. The notification also retains **Activate admin**.

For an update to an existing install, use the same application ID (`com.lge.lgmdmservice`) and signing certificate, and a higher version code. Debug APKs built with a different signing key cannot update an APK signed by the old key.

## filterLGhacked.com WebView

The second tab loads `https://filterlghacked.com/` inside the app. JavaScript, DOM storage and cookies/third-party cookies are enabled. The **Clear cache & reload** button clears cached WebView resources while preserving cookies and site storage.

An HTTP **403 Forbidden** means the server or its security policy refused the WebView request. Clearing the cache can help with stale content but cannot override an origin/WAF/Cloudflare block. A 403 dialog offers **Open in browser** as a diagnostic fallback: if the normal device browser loads the site while this WebView gets 403, the block may be WebView/browser compatibility or policy-related. If both get 403, the site/network policy may be responsible. Keep the Android System WebView updated if the device supports it.

Cloudflare challenges require compatible browser functionality, JavaScript, DOM storage and persistent cookies. Some older Android 6 WebView versions or embedded-browser contexts are limited. The app does not spoof browser identity or bypass Cloudflare controls, and it does not ignore TLS certificate errors.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The artifact is named `yw-app-installer-debug`.
