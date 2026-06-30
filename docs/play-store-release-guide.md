# Arewa Scope Play Store Release Guide

## Current app version

- Package name: `com.arewascopenews.app`
- Version name: `1.7.0`
- Version code: `70`
- Website loaded in app: `https://arewascope.com.ng/`
- FCM topic: `news`

## What changed in this build

- Removed the separate `SplashActivity` to reduce the double-splash feeling.
- Added one main loading screen inside `MainActivity`.
- Loading screen stays for at least 4 seconds and also waits for the first page load before disappearing.
- Added user-friendly notification permission explanation before Android asks for permission.
- Added signed release workflow for APK and AAB.
- Added privacy policy page content in `docs/privacy-policy-wordpress-page.html`.

## Files you must protect

Never share these publicly:

- Firebase Service Account JSON/private key
- Upload keystore `.jks`
- Keystore password
- Key alias password

The `google-services.json` file is not the same as the Firebase Service Account JSON. `google-services.json` belongs in `app/google-services.json` for the Android app build.

## GitHub release signing secrets

Add these in:

`GitHub repo > Settings > Secrets and variables > Actions > New repository secret`

Required secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## How to create KEYSTORE_BASE64

After creating your upload keystore file, convert it to base64.

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Clipboard
```

Mac/Linux:

```bash
base64 -w 0 upload-keystore.jks
```

Paste the copied value into the GitHub secret called `KEYSTORE_BASE64`.

## Build outputs

Run this workflow for testing:

`Actions > Build Debug APK`

Run this workflow for Play Store release:

`Actions > Build Signed Release APK and AAB`

After success, download:

- `ArewaScope-signed-release-apk` for direct sharing/testing outside Play Store.
- `ArewaScope-signed-release-aab` for Google Play Console upload.

For Google Play, upload the `.aab` file, not the APK.

## Privacy policy

Create a WordPress page using the HTML from:

`docs/privacy-policy-wordpress-page.html`

Suggested URL:

`https://arewascope.com.ng/privacy-policy/`

Use that URL in Google Play Console.
