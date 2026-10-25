package com.lge.lgmdmservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.FileChooserParams;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final String WEBSITE_URL = "https://filterlghacked.com/";
    private static final int REQUEST_SELECT_APK = 1201;
    private static final int REQUEST_WEB_FILE = 1202;

    private static final int COLOR_BACKGROUND = Color.rgb(12, 16, 30);
    private static final int COLOR_PANEL = Color.rgb(21, 27, 47);
    private static final int COLOR_PANEL_LIGHT = Color.rgb(30, 38, 63);
    private static final int COLOR_CYAN = Color.rgb(83, 232, 214);
    private static final int COLOR_VIOLET = Color.rgb(157, 112, 255);
    private static final int COLOR_TEXT = Color.rgb(241, 245, 255);
    private static final int COLOR_MUTED = Color.rgb(166, 177, 203);

    private FrameLayout screenContainer;
    private LinearLayout installTab;
    private LinearLayout webTab;
    private Button installTabButton;
    private Button webTabButton;
    private WebView webView;
    private ProgressBar webProgress;
    private ValueCallback<Uri[]> webFileChooserCallback;
    private boolean websiteLoaded;
    private boolean showingWebsite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.rgb(8, 11, 22));
            getWindow().setNavigationBarColor(Color.rgb(8, 11, 22));
        }

        buildInterface();
        setupWebsite();
        showInstallScreen();

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri apkUri = getIntent().getData();
            if (apkUri != null) {
                installApk(apkUri);
            } else {
                Toast.makeText(this, "No APK file was provided.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(22), dp(24), dp(22), dp(16));

        TextView brand = new TextView(this);
        brand.setText("YW  /  ANDROID TOOLS");
        brand.setTextColor(COLOR_CYAN);
        brand.setTextSize(11);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(brand);

        TextView title = new TextView(this);
        title.setText("App Installer");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(5);
        header.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Install an APK or open the companion site.");
        subtitle.setTextColor(COLOR_MUTED);
        subtitle.setTextSize(14);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(5);
        header.addView(subtitle, subtitleParams);
        root.addView(header);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(16), 0, dp(16), dp(14));
        installTabButton = createTabButton("Install APK");
        webTabButton = createTabButton("filterLGhacked.com");
        tabs.addView(installTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));
        LinearLayout.LayoutParams webButtonParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        webButtonParams.leftMargin = dp(10);
        tabs.addView(webTabButton, webButtonParams);
        installTabButton.setOnClickListener(v -> showInstallScreen());
        webTabButton.setOnClickListener(v -> showWebsiteScreen());
        root.addView(tabs);

        screenContainer = new FrameLayout(this);
        LinearLayout.LayoutParams screenParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        screenParams.leftMargin = dp(16);
        screenParams.rightMargin = dp(16);
        screenParams.bottomMargin = dp(14);
        root.addView(screenContainer, screenParams);

        installTab = createInstallScreen();
        screenContainer.addView(installTab, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8), dp(4), dp(8), dp(15));
        TextView footerText = new TextView(this);
        footerText.setText("ANDROID 6+  •  SYSTEM INSTALLER");
        footerText.setTextColor(Color.rgb(105, 119, 151));
        footerText.setTextSize(10);
        footerText.setTypeface(Typeface.DEFAULT_BOLD);
        footer.addView(footerText);
        root.addView(footer);

        setContentView(root);
    }

    private LinearLayout createInstallScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(22));
        content.setBackground(rounded(COLOR_PANEL, 22, COLOR_PANEL_LIGHT, 1));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView apkMark = new TextView(this);
        apkMark.setText("APK");
        apkMark.setTextColor(COLOR_CYAN);
        apkMark.setTextSize(18);
        apkMark.setTypeface(Typeface.DEFAULT_BOLD);
        apkMark.setGravity(Gravity.CENTER);
        apkMark.setBackground(rounded(Color.rgb(23, 49, 62), 18, COLOR_CYAN, 1));
        content.addView(apkMark, new LinearLayout.LayoutParams(dp(76), dp(76)));

        TextView heading = new TextView(this);
        heading.setText("Install an application");
        heading.setTextColor(COLOR_TEXT);
        heading.setTextSize(22);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headingParams.topMargin = dp(23);
        content.addView(heading, headingParams);

        TextView description = new TextView(this);
        description.setText("Choose an APK file stored on this device. The file will be handed to Android PackageInstaller, which performs the installation and shows any required confirmation.");
        description.setTextColor(COLOR_MUTED);
        description.setTextSize(14);
        description.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descriptionParams.topMargin = dp(10);
        content.addView(description, descriptionParams);

        Button chooseButton = new Button(this);
        chooseButton.setText("Choose APK file");
        chooseButton.setAllCaps(false);
        chooseButton.setTextColor(Color.rgb(10, 24, 35));
        chooseButton.setTextSize(16);
        chooseButton.setTypeface(Typeface.DEFAULT_BOLD);
        chooseButton.setBackground(rounded(COLOR_CYAN, 14, COLOR_CYAN, 1));
        chooseButton.setOnClickListener(v -> openApkPicker());
        LinearLayout.LayoutParams chooseParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        chooseParams.topMargin = dp(28);
        content.addView(chooseButton, chooseParams);

        TextView safetyNote = new TextView(this);
        safetyNote.setText("Android still enforces package signatures, install permissions, and system confirmation. This app does not silently bypass those checks.");
        safetyNote.setTextColor(Color.rgb(138, 152, 183));
        safetyNote.setTextSize(12);
        safetyNote.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(18);
        content.addView(safetyNote, noteParams);

        return wrapperScrollView(scrollView);
    }

    private LinearLayout wrapperScrollView(ScrollView scrollView) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return wrapper;
    }

    private void setupWebsite() {
        webTab = new LinearLayout(this);
        webTab.setOrientation(LinearLayout.VERTICAL);
        webTab.setBackground(rounded(COLOR_PANEL, 18, COLOR_PANEL_LIGHT, 1));

        FrameLayout browserFrame = new FrameLayout(this);
        webTab.addView(browserFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        webView = new WebView(this);
        webView.setBackgroundColor(COLOR_PANEL);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) {
            cookies.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleWebNavigation(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleWebNavigation(request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                webProgress.setVisibility(View.VISIBLE);
                webProgress.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                webProgress.setProgress(100);
                webProgress.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                    WebResourceError error) {
                if (request.isForMainFrame()) {
                    webProgress.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,
                            "The page could not be loaded. Check the connection and try again.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                    android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) {
                    webProgress.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,
                            "The website returned HTTP " + response.getStatusCode() + ".",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Never ignore certificate errors; a valid secure connection is required.
                handler.cancel();
                webProgress.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        "Secure connection failed. The certificate was not accepted.",
                        Toast.LENGTH_LONG).show();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                webProgress.setProgress(newProgress);
                if (newProgress >= 100) {
                    webProgress.setVisibility(View.GONE);
                } else if (showingWebsite) {
                    webProgress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (webFileChooserCallback != null) {
                    webFileChooserCallback.onReceiveValue(null);
                }
                webFileChooserCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose a file"),
                            REQUEST_WEB_FILE);
                    return true;
                } catch (Exception exception) {
                    webFileChooserCallback = null;
                    filePathCallback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "No file picker is available.",
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                    android.webkit.JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                    android.webkit.JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message,
                    String defaultValue, android.webkit.JsPromptResult result) {
                EditText input = new EditText(MainActivity.this);
                input.setText(defaultValue);
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok,
                                (dialog, which) -> result.confirm(input.getText().toString()))
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }
        });

        browserFrame.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        webProgress.setMax(100);
        webProgress.setProgress(0);
        webProgress.setVisibility(View.GONE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP);
        browserFrame.addView(webProgress, progressParams);

        Button reloadButton = createTabButton("Reload website");
        reloadButton.setTextColor(COLOR_TEXT);
        reloadButton.setBackground(rounded(COLOR_PANEL_LIGHT, 12, COLOR_PANEL_LIGHT, 1));
        reloadButton.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });
        LinearLayout.LayoutParams reloadParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        reloadParams.setMargins(dp(12), dp(8), dp(12), dp(12));
        webTab.addView(reloadButton, reloadParams);

        screenContainer.addView(webTab, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webTab.setVisibility(View.GONE);
    }

    private boolean handleWebNavigation(String url) {
        if (url == null) return true;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null) return true;
        if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)
                || ("about".equalsIgnoreCase(scheme) && "about:blank".equals(url))) {
            return false;
        }
        // Do not let untrusted page links launch arbitrary intents, file URLs, or content URLs.
        return true;
    }

    private void openApkPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        try {
            startActivityForResult(intent, REQUEST_SELECT_APK);
        } catch (Exception firstFailure) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            fallback.setType("*/*");
            try {
                startActivityForResult(Intent.createChooser(fallback, "Select an APK"),
                        REQUEST_SELECT_APK);
            } catch (Exception secondFailure) {
                Toast.makeText(this, "No file picker is available on this device.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void installApk(Uri apkUri) {
        PackageInstaller.Session session = null;
        try {
            if (apkUri == null) throw new IllegalArgumentException("No APK was selected.");
            String scheme = apkUri.getScheme();
            if (scheme != null && !"content".equalsIgnoreCase(scheme)
                    && !"file".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("The selected file location is not supported.");
            }

            ContentResolver resolver = getContentResolver();
            InputStream input = resolver.openInputStream(apkUri);
            if (input == null) throw new IllegalStateException("Cannot read the selected APK.");

            PackageInstaller installer = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = installer.createSession(params);
            session = installer.openSession(sessionId);

            try (InputStream source = input;
                 OutputStream output = session.openWrite("base.apk", 0, -1)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = source.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                session.fsync(output);
            }

            Intent statusIntent = new Intent(this, InstallStatusReceiver.class);
            statusIntent.setAction("com.lge.lgmdmservice.INSTALL_STATUS." + sessionId);
            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) {
                pendingFlags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent statusPendingIntent = PendingIntent.getBroadcast(
                    this, sessionId, statusIntent, pendingFlags);
            session.commit(statusPendingIntent.getIntentSender());
            session.close();
            session = null;

            Toast.makeText(this,
                    "APK submitted. Follow Android's installation prompt if one appears.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception exception) {
            if (session != null) {
                try { session.abandon(); } catch (Exception ignored) { }
                try { session.close(); } catch (Exception ignored) { }
            }
            String detail = exception.getMessage();
            Toast.makeText(this,
                    "Could not start APK installation"
                            + (detail == null ? "." : ": " + detail),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showInstallScreen() {
        showingWebsite = false;
        installTab.setVisibility(View.VISIBLE);
        webTab.setVisibility(View.GONE);
        installTabButton.setTextColor(Color.rgb(10, 24, 35));
        installTabButton.setBackground(rounded(COLOR_CYAN, 14, COLOR_CYAN, 1));
        webTabButton.setTextColor(COLOR_MUTED);
        webTabButton.setBackground(rounded(COLOR_PANEL_LIGHT, 14, COLOR_PANEL_LIGHT, 1));
        if (webView != null) webView.onPause();
    }

    private void showWebsiteScreen() {
        showingWebsite = true;
        installTab.setVisibility(View.GONE);
        webTab.setVisibility(View.VISIBLE);
        webTabButton.setTextColor(Color.rgb(10, 24, 35));
        webTabButton.setBackground(rounded(COLOR_CYAN, 14, COLOR_CYAN, 1));
        installTabButton.setTextColor(COLOR_MUTED);
        installTabButton.setBackground(rounded(COLOR_PANEL_LIGHT, 14, COLOR_PANEL_LIGHT, 1));
        if (webView != null) {
            webView.onResume();
            if (!websiteLoaded) {
                websiteLoaded = true;
                webProgress.setVisibility(View.VISIBLE);
                webView.loadUrl(WEBSITE_URL);
            }
        }
    }

    private Button createTabButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(COLOR_MUTED);
        button.setBackground(rounded(COLOR_PANEL_LIGHT, 14, COLOR_PANEL_LIGHT, 1));
        if (Build.VERSION.SDK_INT >= 21) {
            button.setStateListAnimator(null);
        }
        return button;
    }

    private GradientDrawable rounded(int fillColor, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_APK) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                installApk(data.getData());
            } else {
                Toast.makeText(this, "No APK was selected.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_WEB_FILE) {
            if (webFileChooserCallback != null) {
                Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                        ? new Uri[] { data.getData() } : null;
                webFileChooserCallback.onReceiveValue(result);
                webFileChooserCallback = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (showingWebsite && webView != null && webView.canGoBack()) {
            webView.goBack();
        } else if (showingWebsite) {
            showInstallScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showingWebsite && webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webFileChooserCallback != null) {
            webFileChooserCallback.onReceiveValue(null);
            webFileChooserCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
