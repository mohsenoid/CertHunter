# CertHunter 🛡️

**CertHunter** is a lightweight, modern Android utility tool built with **Jetpack Compose**. It allows developers and security researchers to inspect the
signing certificate fingerprints (SHA-256, SHA-1) and X.509 details of any application installed on their device.

## 🚀 Features

- **App Inspection:** Lists all user and system applications installed on the device.
- **Real-time Search:** Filter apps instantly by App Name or Package Name.
- **Signature Extraction:** Retrieves valid signing fingerprints:
    - SHA-256
    - SHA-1
- **X.509 Parsing:** Decodes the raw certificate to display:
    - Owner (Subject DN)
    - Issuer
    - Serial Number
    - Validity Period (Valid From / Valid Until)
- **Clipboard Support:** Tap any field in the details dialog to copy the value to the clipboard.
- **Modern UI:** Built entirely with Jetpack Compose and Material 3 design.

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Concurrency:** Kotlin Coroutines (Dispatchers.IO for background loading)
- **Security API:** `java.security.MessageDigest`, `java.security.cert.X509Certificate`
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 37 (Android 15)

## 📸 Screenshots

| **App List & Search**                               | **Certificate Details**                                   |
|-----------------------------------------------------|-----------------------------------------------------------|
| ![screenshot_app_list.png](screenshot_app_list.png) | ![screenshot_cert_dialog.png](screenshot_cert_dialog.png) |

## 🔑 Permissions & Privacy

This application requires the following specific permission to function on Android 11 (API 30) and above:

XML

```
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

**Why?**

Due to Android's *Package Visibility* changes, apps cannot see other installed apps by default. Since the core purpose of CertHunter is to inspect *other* apps,
this permission is mandatory.

*Note: No data leaves the device. All processing is done locally.*

## 💻 Installation

1. Clone the repository:

   Bash

   ```
   git clone https://github.com/mohsenoid/CertHunter.git
   ```

2. Open the project in **Android Studio** (Hedgehog or newer recommended).

3. Sync Gradle files.

4. Run on an emulator or physical device (Android 7.0+).

## 🧩 Code Highlight

How we extract signatures across different Android versions (Legacy vs Modern API):

Kotlin

```
val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    // Android 9+ (API 28+)
    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
} else {
    // Legacy (API 24-27)
    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
}
```

## 🚢 Release Flow

Releases are triggered manually via GitHub Actions — no tags are pushed by hand and no version numbers are typed. The workflow reads the current version from `build.gradle.kts`, increments it, builds the artifacts, and publishes everything automatically.

### Regular release (from `main`)

1. Ensure `main` is in a releasable state.
2. Go to **Actions → Release → Run workflow**.
3. Select branch **`main`** and choose the bump type:
   - **`minor`** — new feature release (e.g. `1.2.0` → `1.3.0`)
   - **`major`** — breaking change release (e.g. `1.3.0` → `2.0.0`)
   - **`patch`** — bug fix on current main (e.g. `1.3.0` → `1.3.1`)
4. The workflow will:
   - Read the current `versionName` from `app/build.gradle.kts` and compute the next version.
   - Patch `versionCode` / `versionName` in `app/build.gradle.kts`.
   - Build a signed AAB and APK.
   - Upload the AAB as a **draft** to the Play Store **internal testing** track.
   - Commit the version bump, create the tag (e.g. `v1.3.0`) at that commit, and push both.
   - Create a GitHub Release with the AAB and APK attached.
5. In **Google Play Console → Internal testing**, review and publish the draft to promote it to testers or production.

### Hotfix release (from a branch)

1. Create a branch from the last release tag: `git checkout -b hotfix/1.2.x v1.2.0`.
2. Cherry-pick or commit the fix onto that branch and push it.
3. Go to **Actions → Release → Run workflow**.
4. Select the **`hotfix/1.2.x`** branch and choose **`patch`**.
5. The workflow reads `1.2.0` from that branch's `build.gradle.kts`, bumps to `1.2.1`, and runs the same steps as above, tagging the hotfix commit.

### versionCode formula

`versionCode = MAJOR × 1,000,000 + MINOR × 1,000 + PATCH`

| Version | versionCode |
|---------|------------|
| 1.2.0   | 1002000    |
| 1.3.0   | 1003000    |
| 1.2.1   | 1002001    |

Hotfix codes are always lower than the next minor release, preserving correct Play Store ordering.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

Copyright 2026 Mohsen Mirhoseini

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
