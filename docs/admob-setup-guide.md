# AdMob setup for Arewa Scope

## Current status

This app version includes a bottom banner AdMob placement using official Google test IDs. Use test IDs until the app is ready for release.

## Where to replace AdMob IDs

Open:

`app/src/main/res/values/strings.xml`

Replace:

```xml
<string name="admob_app_id">ca-app-pub-3940256099942544~3347511713</string>
<string name="admob_banner_ad_unit_id">ca-app-pub-3940256099942544/6300978111</string>
```

with your real IDs from AdMob.

## Recommended process

1. Build with test ads first.
2. Confirm the bottom banner space appears and app still loads properly.
3. Create the app in AdMob as an unpublished Android app.
4. Create a banner ad unit.
5. Replace the test IDs with your real AdMob App ID and Banner Ad Unit ID.
6. Build signed AAB.
7. Upload AAB to Google Play Console.
8. Add app-ads.txt to your website when AdMob asks for it.
9. Link your app store listing in AdMob after the app is live.

## Do not click your own ads

Clicking your own live ads can put your AdMob account at risk. Use test ads while developing and testing.
