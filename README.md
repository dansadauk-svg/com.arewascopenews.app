# Arewa Scope Android App v8 AdMob

This version keeps the working Arewa Scope WebView app, Firebase push notifications, splash/loading screen, signed release workflow, and adds AdMob banner support.

## Important Firebase step

Before building, replace the placeholder file with your real Firebase file:

`app/google-services.json`

The Firebase Android package name must remain:

`com.arewascopenews.app`

## AdMob test setup

This version uses Google AdMob test IDs by default:

- Test App ID: `ca-app-pub-3940256099942544~3347511713`
- Test Banner Ad Unit ID: `ca-app-pub-3940256099942544/6300978111`

Build and test with these first. Do not click real ads on your own device.

## Replace with real AdMob IDs before public release

Edit:

`app/src/main/res/values/strings.xml`

Replace:

```xml
<string name="admob_app_id">YOUR_REAL_ADMOB_APP_ID</string>
<string name="admob_banner_ad_unit_id">YOUR_REAL_BANNER_AD_UNIT_ID</string>
```

## Debug build

Run:

`Actions -> Build Debug APK`

## Release build

Add these GitHub repository secrets first:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Then run:

`Actions -> Build Signed Release APK and AAB`

Upload the `.aab` file to Google Play Console.
