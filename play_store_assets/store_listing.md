# CitrusRemote — Google Play Store Listing Copy & Metadata

## App Details

- **App Name** (max 30 chars): `CitrusRemote for Apple TV`
- **Short Description** (max 80 chars): `Fast Apple TV control from Android with media buttons and keyboard input.`
- **Default Language**: English (United States) – en-US
- **Application ID**: `com.reggiesoft.citrusremote`

---

## Full Description (max 4,000 chars)

```text
CitrusRemote is a fast, clean, and private Android remote control for Apple TV. Connect in seconds over your local Wi-Fi network to navigate, control playback, and type directly from your Android keyboard.

KEY FEATURES

• Quick Local Discovery: Automatically detects Apple TV units on your local network using standard ZeroConf / Bonjour.
• Tactile Media Controls: D-pad navigation, play/pause, volume control, mute, and menu navigation engineered for comfortable one-handed use.
• Android Keyboard Input: Say goodbye to tedious on-screen cursor typing. Type search queries, logins, and passwords directly into tvOS text fields using your phone's native keyboard.
• Dark Mode Native: Styled in rich dark tones engineered for OLED displays and low-light living room environments.
• Lightweight & Responsive: Built with modern Android Jetpack Compose and native Kotlin for immediate button response and low latency.
• Free & Open Source: No ads, no tracking, and no subscriptions—ever.

PRIVACY BY DESIGN

CitrusRemote puts user privacy first:
• No accounts or registrations required.
• No advertising banners or promotional popups.
• No analytics or behavioral tracking SDKs.
• Zero data collection: all pairing tokens stay private on your device and are excluded from cloud backups. Communication happens strictly within your local home network.

REQUIREMENTS

• Apple TV HD (4th generation) or Apple TV 4K running tvOS.
• Your Android phone and Apple TV must be connected to the same local Wi-Fi or Ethernet network.

SUPPORT & OPEN SOURCE

CitrusRemote is built and maintained by ReggieSoft.
• Website: https://reggiesoft.com/citrusremote
• GitHub: https://github.com/polson/citrusremote
• Privacy Policy: https://reggiesoft.com/citrusremote/privacy/
• Support: support@reggiesoft.com
```

---

## Store Listing Categorization

- **App Category**: Tools or Media & Video
- **Tags**: Remote Control, Apple TV, Media Player, Utility, Open Source
- **Content Rating**: Everyone (PEGI 3, ESRB Everyone)
- **Target Audience**: 18 and older, or All ages (No child-directed content)

---

## Data Safety Questionnaire Cheat Sheet

When filling out the Google Play Data Safety form:

1. **Does your app collect or share any user data?**
   - Select: **No**.
   - Explanation: The app does not collect, store, or share user data with developers or third parties. All network activity is local to the user's home network.

2. **Security practices**:
   - **Data encryption in transit**: Yes (connections to Apple TV use Apple's encrypted Companion protocol).
   - **Data deletion request**: Not applicable (no account or stored data on developer servers).

---

## Release Artifacts

- **Signed AAB**: `app/build/outputs/bundle/release/app-release.aab`
- **Store Icon (512 × 512)**: `play_store_assets/icon_512x512.png`
- **Feature Graphic (1024 × 500)**: `play_store_assets/feature_graphic_1024x500.png`
- **Phone Screenshots (1080 × 2088)**:
  - `play_store_assets/screenshot_01_discovery.png` (Device Discovery — Light Mode)
  - `play_store_assets/screenshot_02_remote.png` (Tactile Remote — Light Mode)
  - `play_store_assets/screenshot_03_discovery_dark.png` (Device Discovery — Dark Mode)
  - `play_store_assets/screenshot_04_remote_dark.png` (Tactile Remote — Dark Mode)
- **Upload Keystore**: Located in `release.jks` with credentials saved in `~/.gradle/gradle.properties`.
- **Dedicated Google / Support Account**: `reggiesoft42@gmail.com` (destination for `support@reggiesoft.com` Cloudflare forwarder and Play Console registration).
