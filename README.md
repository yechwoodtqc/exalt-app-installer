# YW app installer

- **Application ID:** `com.lge.lgmdmservice`
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** 22 (kept for the legacy Android 6 device)
- **Version:** 2.2 (versionCode 13)

## In-app APK installation

Normal APK installs and self-updates are started inside the app:

1. Open **Install APK**.
2. Tap **Choose APK file** and select the APK.
3. The app sends it to Android `PackageInstaller`. If Android requires confirmation, the system installer screen is opened directly from the status callback, rather than through an app-generated notification.
4. Success and failure are shown with a toast.

Android still enforces signing certificates, install permissions and user approval. The app does not silently bypass these checks.

## One-time update transition

The previously installed 2.1 build has an Accounts notification action named **Install update**. That action can be used once to update to this 2.2 build, provided the APK has the same application ID, a higher version code and the same signing certificate as the installed version.

Starting with 2.2, the Accounts notification no longer contains an **Install update** action, and the legacy notification-based update service is no longer registered or packaged. All routine app installations and app self-updates should be initiated from the in-app APK picker.

**Signing caveat:** Android will reject an update signed by a different certificate. If using a debug APK, ensure the new APK is signed with the same debug key as the APK currently installed on the device.

## Accounts → Sync now notification

The existing Accounts trigger is preserved:

1. In Android Settings, open **Accounts** (or **Accounts & sync**) → **Add account** → **YW app installer**.
2. Open the account and use **Sync now**.
3. `AccountSyncAdapterService.onPerformSync()` accepts only manual sync requests and ignores automatic/initialization callbacks.
4. `ActivationNotification.showActivationNotification()` posts the notification with the **Activate admin** action only.
5. The action opens Android's system device-admin confirmation. The device-admin receiver declares no policy powers.

## filterLGhacked.com WebView

The second tab loads `https://filterlghacked.com/` inside a WebView with JavaScript, DOM storage, cookies and third-party cookies enabled. **Clear cache & reload** clears the WebView's cached resources while retaining site cookies and storage.

HTTP 304 usually means the server considers a cached resource unchanged; it is not normally a failure response. A 403 is a real forbidden response and may mean the website or its Cloudflare rules are refusing the request. The app does not bypass certificate errors or Cloudflare security. Android 6 System WebView compatibility and the site's own policy may limit access.

## Package ID

The application ID remains `com.lge.lgmdmservice`, the previously confirmed package name. Android treats a different application ID as a separate application.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The artifact is named `yw-app-installer-debug`.
