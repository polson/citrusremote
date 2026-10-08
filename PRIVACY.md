# Privacy Policy for CitrusRemote

*Effective Date: October 2026*  
*Published by ReggieSoft*  
*Online version: [https://reggiesoft.com/citrusremote/privacy/](https://reggiesoft.com/citrusremote/privacy/)*

CitrusRemote is a free, open-source Android remote control for Apple TV devices. Privacy and data security are core design principles of the application.

## 1. Zero Data Collection

CitrusRemote does not collect, record, track, transmit, or sell personal information of any kind. There are:
- No user accounts or login systems
- No telemetry or analytics frameworks
- No advertising networks or tracking SDKs
- No background tracking or location monitoring

## 2. Local Network Communication

All communication occurs strictly within your local Wi-Fi or Ethernet network:
- **Device Discovery**: The app broadcasts ZeroConf/mDNS queries on your local network to discover Apple TV units.
- **Apple TV Control**: The app connects directly to the local IP address of your selected Apple TV using Apple's Companion protocol. No connection traffic or device metadata is routed to external servers.

## 3. Pairing Credentials & Local Storage

When pairing with an Apple TV, authentication tokens are generated:
- All credentials are saved strictly on-device in private Android app storage (`AppleTVPrefs`).
- Android backup rules in CitrusRemote explicitly exclude credentials from Google Cloud backups and device-to-device transfers.
- Credentials remain on your device until you unpair the device or clear app data.

## 4. Keyboard Input

When using the keyboard input feature:
- Text is transmitted directly over the encrypted local connection to the connected Apple TV.
- Typed text and keystrokes are never logged, cached to disk, or sent to external servers.

## 5. Open Source Transparency

CitrusRemote is open source under the MIT license. You can inspect the source code, verify network and data handling practices, and build the application yourself.

## 6. Contact

If you have questions or concerns about this privacy policy, contact:
- Email: [support@reggiesoft.com](mailto:support@reggiesoft.com)
- Web: [https://reggiesoft.com](https://reggiesoft.com)
