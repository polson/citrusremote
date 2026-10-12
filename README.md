# CitrusRemote

<p align="center">
  <img src="play_store_assets/feature_graphic_1024x500.png" alt="CitrusRemote Feature Banner" width="700" />
</p>

<p align="center">
  <strong>Fast, private Android remote control for Apple TV.</strong><br />
  No ads • No tracking • Free & Open Source
</p>

---

CitrusRemote is an Android remote application designed for Apple TV. It communicates directly over your local Wi-Fi network using tvOS's native Companion protocol, providing immediate button response, media playback controls, and native phone keyboard typing.

## Screenshots

| Device Discovery (Light) | Remote Controls (Light) | Device Discovery (Dark) | Remote Controls (Dark) |
| :---: | :---: | :---: | :---: |
| <img src="play_store_assets/screenshot_01_discovery.png" width="200" alt="Discovery Screen Light" /> | <img src="play_store_assets/screenshot_02_remote.png" width="200" alt="Remote Screen Light" /> | <img src="play_store_assets/screenshot_03_discovery_dark.png" width="200" alt="Discovery Screen Dark" /> | <img src="play_store_assets/screenshot_04_remote_dark.png" width="200" alt="Remote Screen Dark" /> |

## Features

- **Quick Local Discovery**: Discovers Apple TV devices automatically over ZeroConf / Bonjour on your local Wi-Fi.
- **Tactile Media Controls**: Ergonomic D-pad navigation, volume control, mute, play/pause, and tvOS home/back navigation.
- **Direct Keyboard Input**: Type logins, search queries, and passwords into tvOS text fields using your Android device's native keyboard.
- **Dark Mode Native**: Thoughtfully styled for low-light living room environments and OLED screens.
- **Privacy by Design**:
  - Zero analytics, telemetry, or advertising SDKs.
  - No user accounts or external servers required.
  - Pairing credentials stay on your device and are excluded from cloud backups.

## Requirements

- **Android**: Android 7.0 (API level 24) or higher.
- **Apple TV**: Apple TV HD (4th generation) or Apple TV 4K running tvOS.
- **Network**: Android phone and Apple TV must be connected to the same local Wi-Fi or Ethernet network.

## Architecture & Tech Stack

- **UI**: 100% Kotlin Jetpack Compose and Material 3.
- **Architecture**: MVVM with unidirectional data flow and Kotlin Coroutines / StateFlow.
- **Dependency Injection**: Hilt.
- **tvOS Protocol**: [pyatv](https://github.com/postlund/pyatv) executed via [Chaquopy](https://chaquo.com/chaquopy/) (embedded Python 3.12 runtime).

## Development & Building

### Build from Source

CitrusRemote builds with standard Gradle:

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

### Testing with the Mock Apple TV Server

To test pairing and navigation without a physical Apple TV:

1. **Set up the mock server once** from the repository root:
   ```bash
   python3.12 -m venv .venv
   source .venv/bin/activate
   git clone https://github.com/postlund/pyatv.git .venv/pyatv_repo
   python -m pip install -e .venv/pyatv_repo
   ```

2. **Start the Mock Server** from the repository root:
   ```bash
   source .venv/bin/activate
   python mock_appletv.py
   ```
   Leave it running. It must report Companion port `49152` and PIN `1111`.

3. **Connect a device**:
   - Run the debug build on an Android Emulator or physical Android phone.
   - Navigate to the **Discovered Devices** screen.
   - Tap the **Wrench/Build icon** in the top right corner to inject a "Mock Apple TV (Dev)" device.
   - Keep the mock server address at `10.0.2.2` to connect through the emulator bridge.
   - Once pairing connects, enter PIN `1111`. The PIN field stays disabled until the mock Companion service responds.

   To use a physical Android phone, enter the computer's local IPv4 address in the mock server dialog. Keep the phone and computer on the same Wi-Fi network and allow incoming connections to ports `49152` and `49153` on the computer.
