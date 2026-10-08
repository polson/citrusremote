# CitrusRemote Google Play Release Checklist

Release target: Google Play, package `com.reggiesoft.citrusremote`.

## Current status

- [x] Application ID is `com.reggiesoft.citrusremote`; app name is CitrusRemote.
- [x] `targetSdk` is 36, which meets Google Play's mobile app target API requirement effective August 31, 2026. Check the [current Play target API policy](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en) again when submitting.
- [ ] Complete the source, privacy, QA, and Play Console items below before production release.

## 1. Close release blockers in the app

- [x] Restrict the **Add Mock Device** control to dev builds only via `BuildConfig.DEBUG` (hidden in production release builds).
- [x] Remove PIN values from error logs in `DeviceRepository` and `ChaquopyAppleTvRemoteService`. Review remaining logs and exception messages to ensure they do not expose PINs, pairing credentials, or typed keyboard text.
- [x] Protect saved Apple TV credentials by excluding `AppleTVPrefs.xml` from cloud backup and device transfer in `backup_rules.xml` and `data_extraction_rules.xml`.
- [x] Fix the instrumented smoke test: update expected application ID to `com.reggiesoft.citrusremote`.
- [x] Review the release toolchain before creating the bundle. Confirmed AGP, Chaquopy Python 3.12, and arm64-v8a/x86_64 wheels build and package cleanly.
- [ ] Reassess the pinned Android Gradle Plugin version (`9.1.0-rc01`) before production. Prefer a stable version compatible with Chaquopy, or document why the release candidate is retained, then rebuild and verify the release bundle.

## 2. Configure release signing and version

- [x] Configure Google Play release upload keystore (`release.jks`).
- [x] Configure `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` in `~/.gradle/gradle.properties`.
- [x] Remove the debug-signing fallback in `app/build.gradle.kts`. A release build must fail if the upload key is missing, and the uploaded AAB must be signed with the intended upload key.
- [ ] Set a monotonically increasing `versionCode` for every AAB uploaded to Play. Current values are `versionCode = 1` and `versionName = "1.0"`; retain them for the first upload only if that code has not already been used in Play Console.
- [x] Ensure Chaquopy can find Python 3.12 on the release builder. Confirmed Python 3.12 at `~/.local/bin/python3.12`.

## 3. Build and verify the release artifact

- [x] Run the local unit tests (`./gradlew testDebugUnitTest`).
- [x] Build the Play bundle: `./gradlew bundleRelease`.
- [x] Confirm `app/build/outputs/bundle/release/app-release.aab` exists and is signed with the intended upload key.
- [ ] Re-run the release build and relevant tests from the final source revision. The currently present AAB predates the latest source and dependency updates; do not upload it as the final release candidate.
- [ ] Verify the rebuilt AAB's signing certificate matches the configured upload key and that its `versionCode` has not already been used in Play Console.
- [ ] Confirm the final bundle supports 16 KB page-size devices and schedule a runtime check in a 16 KB environment. Google Play's current deadline for app updates is February 1, 2027; the existing older bundle's native ELF load segments were 16 KB aligned, but recheck the final bundle. See [Android's 16 KB page-size guidance](https://developer.android.com/guide/practices/page-sizes).
- [ ] Install and smoke-test a release APK on physical Android hardware. Verify Wi-Fi Apple TV discovery, pairing and PIN entry, navigation and playback commands, keyboard text entry, reconnect behavior, and lockout/error handling with a real Apple TV.
- [ ] Exercise the internal testing track before production and resolve any crashes or device-specific issues found there.
- [ ] If the Play Console account is a personal account created after November 13, 2023, complete a closed test with at least 12 testers continuously opted in for 14 days, then apply for production access. See [Google Play's testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en-en). For other account types or older personal accounts, confirm the testing requirements shown in that Play Console account.

## 4. Complete privacy information and Play Console declarations

- [x] Publish an active privacy policy URL (hosted at `https://reggiesoft.com/citrusremote/privacy/` and [`PRIVACY.md`](file:///Users/admin/Development/citrusremote/PRIVACY.md)) and provide an in-app launcher on [`DiscoveryScreen`](file:///Users/admin/Development/citrusremote/app/src/main/java/com/reggiesoft/citrusremote/ui/discovery/DiscoveryScreen.kt).
- [x] Re-check that the privacy policy URL is publicly reachable (verified HTTP 200) and matches the policy linked in the app and Play listing.
- [ ] Audit the app and bundled dependencies, then complete the Play Console Data Safety form to match actual collection, sharing, and backup behavior. The current policy and listing say no personal information is transmitted, while keyboard text is sent to the Apple TV; reconcile these statements and classify transmissions under the current [Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) before submitting the form. Do not use the previous blanket claim that credentials are never transmitted until backup behavior has been resolved and verified.
- [ ] Complete the remaining App content declarations, including ads, target audience, content rating, app access/reviewer instructions, and any permission declarations Play Console requests.
- [ ] Create the Play Console app under `com.reggiesoft.citrusremote` and opt into Play App Signing.
- [ ] Complete any required developer identity/contact verification and app package registration in Play Console using dedicated account `reggiesoft42@gmail.com`; resolve account or policy tasks that block submission. See [Play Console requirements](https://support.google.com/googleplay/android-developer/answer/10788890?hl=en).
- [ ] Give reviewers the instructions and resources needed to evaluate the app, including an Apple TV or another workable review path if requested.

## 5. Prepare the store listing and rollout

- [x] Add an open-source `LICENSE` file (MIT) to the repository root.
- [x] Switch GitHub repository visibility to public (verified public at `https://github.com/polson/citrusremote`).
- [x] Set up Cloudflare Email Routing for `support@reggiesoft.com` forwarding to `reggiesoft42@gmail.com`.
- [x] Verify/export the 512 × 512 px store icon (`play_store_assets/icon_512x512.png`).
- [x] Prepare a 1024 × 500 px feature graphic (`play_store_assets/feature_graphic_1024x500.png`).
- [x] Prepare and verify app screenshots for light and dark modes (`play_store_assets/screenshot_01_discovery.png` through `04_remote_dark.png`).
- [x] Write the short description (up to 80 characters) and full description (up to 4,000 characters) in `play_store_assets/store_listing.md`.
- [ ] Upload the signed AAB to internal testing, invite testers, and review the app on a range of supported Android versions and screen sizes.
- [ ] Promote the verified release through any planned closed testing, then complete the Play production review and rollout.

## Release notes for the next upload

Before each later upload, increment `versionCode`, update `versionName` as appropriate, rebuild the signed AAB, and repeat release smoke testing. Keep this checklist current as code and Google Play requirements change.
