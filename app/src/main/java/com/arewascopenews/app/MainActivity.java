package com.arewascopenews.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends Activity {
    private static final String TAG = "ArewaScope";
    private static final String SITE_URL = "https://arewascope.com.ng/";
    private static final String SITE_HOST = "arewascope.com.ng";
    private static final String FCM_TOPIC = "news";
    private static final String BRAND_ORANGE = "#F76103";
    private static final int MINIMUM_LOADING_MS = 4000;
    private static final int BANNER_RESERVED_HEIGHT_DP = 56;

    public static final String NOTIFICATION_CHANNEL_ID = "arewa_news";
    public static final String NOTIFICATION_CHANNEL_NAME = "Arewa Scope News";

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2001;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout offlineView;
    private LinearLayout loadingView;
    private FrameLayout adContainer;
    private AdView adView;
    private ValueCallback<Uri[]> uploadCallback;

    private boolean firstPageFinished = false;
    private boolean minimumLoadingFinished = false;
    private boolean loadingScreenDismissed = false;
    private boolean notificationExplanationShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor(BRAND_ORANGE));
        getWindow().setNavigationBarColor(Color.parseColor(BRAND_ORANGE));

        createNotificationChannel();
        MobileAds.initialize(this, initializationStatus -> Log.d(TAG, "Google Mobile Ads initialized"));
        buildLayout();
        setupWebView();
        subscribeToNewsTopic();
        startMinimumLoadingTimer();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            firstPageFinished = true;
            maybeHideLoadingScreen();
        } else {
            String initialUrl = extractSafeUrlFromIntent(getIntent());
            loadUrl(initialUrl == null ? SITE_URL : initialUrl);
        }
    }

    private void buildLayout() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.setFitsSystemWindows(true);

        webView = new WebView(this);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        adContainer = buildAdContainer();
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(BANNER_RESERVED_HEIGHT_DP));
        adParams.gravity = Gravity.BOTTOM;
        root.addView(adContainer, adParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        offlineView = buildOfflineView();
        root.addView(offlineView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        loadingView = buildLoadingView();
        root.addView(loadingView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingView.bringToFront();

        setContentView(root);
        loadBottomBannerAd();
    }

    private FrameLayout buildAdContainer() {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.WHITE);
        container.setVisibility(View.GONE);
        return container;
    }

    private void loadBottomBannerAd() {
        try {
            adView = new AdView(this);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(getString(R.string.admob_banner_ad_unit_id));
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "AdMob banner loaded");
                    adContainer.setVisibility(View.VISIBLE);
                    setWebViewBottomMargin(BANNER_RESERVED_HEIGHT_DP);
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    Log.w(TAG, "AdMob banner failed to load: " + adError);
                    adContainer.setVisibility(View.GONE);
                    setWebViewBottomMargin(0);
                }
            });

            FrameLayout.LayoutParams adViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            adViewParams.gravity = Gravity.CENTER;
            adContainer.addView(adView, adViewParams);
            adView.loadAd(new AdRequest.Builder().build());
        } catch (Exception e) {
            Log.w(TAG, "Unable to initialize AdMob banner", e);
            if (adContainer != null) {
                adContainer.setVisibility(View.GONE);
            }
            setWebViewBottomMargin(0);
        }
    }

    private void setWebViewBottomMargin(int bottomMarginDp) {
        if (webView == null) {
            return;
        }
        ViewGroup.LayoutParams params = webView.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
            frameParams.setMargins(0, 0, 0, dp(bottomMarginDp));
            webView.setLayoutParams(frameParams);
        }
    }

    private LinearLayout buildOfflineView() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(24), dp(24), dp(24), dp(24));
        view.setBackgroundColor(Color.WHITE);
        view.setVisibility(View.GONE);

        TextView title = new TextView(this);
        title.setText("No internet connection");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#0B1220"));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        view.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please check your connection and try again.");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.parseColor("#334155"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(10), 0, dp(18));
        view.addView(subtitle);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setAllCaps(false);
        retry.setOnClickListener(v -> {
            firstPageFinished = false;
            minimumLoadingFinished = false;
            loadingScreenDismissed = false;
            loadingView.setAlpha(1f);
            loadingView.setVisibility(View.VISIBLE);
            loadingView.bringToFront();
            startMinimumLoadingTimer();
            loadUrl(webView.getUrl() == null ? SITE_URL : webView.getUrl());
        });
        view.addView(retry);

        return view;
    }

    private LinearLayout buildLoadingView() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(Color.parseColor(BRAND_ORANGE));
        view.setPadding(dp(28), dp(28), dp(28), dp(28));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.splash_logo);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(190), dp(190));
        logoParams.setMargins(0, 0, 0, dp(18));
        view.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("Arewa Scope");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        view.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Loading latest Northern Nigeria updates...");
        subtitle.setTextColor(Color.parseColor("#FFF3EA"));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.setMargins(0, dp(6), 0, dp(22));
        view.addView(subtitle, subParams);

        ProgressBar loader = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        view.addView(loader, new LinearLayout.LayoutParams(dp(34), dp(34)));

        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setCacheMode(isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setUserAgentString(settings.getUserAgentString() + " ArewaScopeAndroidApp/1.7");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                offlineView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                offlineView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                firstPageFinished = true;
                maybeHideLoadingScreen();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (failingUrl != null && failingUrl.equals(view.getUrl())) {
                    firstPageFinished = true;
                    webView.setVisibility(View.GONE);
                    offlineView.setVisibility(View.VISIBLE);
                    maybeHideLoadingScreen();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
                if (newProgress >= 100) {
                    firstPageFinished = true;
                    maybeHideLoadingScreen();
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadCallback != null) {
                    uploadCallback.onReceiveValue(null);
                }
                uploadCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker found", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }

    private void startMinimumLoadingTimer() {
        handler.postDelayed(() -> {
            minimumLoadingFinished = true;
            maybeHideLoadingScreen();
        }, MINIMUM_LOADING_MS);
    }

    private void maybeHideLoadingScreen() {
        if (loadingScreenDismissed || loadingView == null) {
            return;
        }
        if (!firstPageFinished || !minimumLoadingFinished) {
            return;
        }
        loadingScreenDismissed = true;
        loadingView.animate()
                .alpha(0f)
                .setDuration(280)
                .withEndAction(() -> {
                    loadingView.setVisibility(View.GONE);
                    maybeShowNotificationPermissionExplanation();
                })
                .start();
    }

    private void maybeShowNotificationPermissionExplanation() {
        if (notificationExplanationShown) {
            return;
        }
        notificationExplanationShown = true;

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.notification_permission_title))
                    .setMessage(getString(R.string.notification_permission_message))
                    .setPositiveButton("Allow", (dialog, which) -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST))
                    .setNegativeButton("Not now", null)
                    .show();
        }
    }

    private boolean handleUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if ("http".equals(scheme) || "https".equals(scheme)) {
            if (SITE_HOST.equals(host) || ("www." + SITE_HOST).equals(host)) {
                return false;
            }
            openOutside(url);
            return true;
        }

        if ("tel".equals(scheme) || "mailto".equals(scheme) || "whatsapp".equals(scheme) || "intent".equals(scheme)) {
            openOutside(url);
            return true;
        }

        return false;
    }

    private void openOutside(String url) {
        try {
            Intent intent = url.startsWith("intent:") ? Intent.parseUri(url, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open this link", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUrl(String url) {
        if (isOnline()) {
            offlineView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(url);
        } else {
            firstPageFinished = true;
            webView.setVisibility(View.GONE);
            offlineView.setVisibility(View.VISIBLE);
            maybeHideLoadingScreen();
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void subscribeToNewsTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(FCM_TOPIC)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to FCM topic: " + FCM_TOPIC);
                    } else {
                        Log.w(TAG, "FCM topic subscription failed", task.getException());
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Latest Arewa Scope news updates");
                manager.createNotificationChannel(channel);
            }
        }
    }

    private String extractSafeUrlFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        String url = intent.getStringExtra("url");
        if (url == null) {
            url = intent.getStringExtra("link");
        }
        if (url == null) {
            return null;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (("http".equals(scheme) || "https".equals(scheme)) && (SITE_HOST.equals(host) || ("www." + SITE_HOST).equals(host))) {
            return url;
        }
        return SITE_URL;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = extractSafeUrlFromIntent(intent);
        if (url != null && webView != null) {
            loadUrl(url);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && uploadCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (uploadCallback != null) {
            uploadCallback.onReceiveValue(null);
            uploadCallback = null;
        }
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
