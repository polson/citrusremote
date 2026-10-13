# Privacy Policy for CitrusRemote

*Effective Date: October 2026*  
*Published by ReggieSoft*  
*Online version: [https://reggiesoft.com/citrusremote/privacy/](https://reggiesoft.com/citrusremote/privacy/)*

CitrusRemote is a free, open-source Android remote control for Apple TV devices. Privacy and data security are core design principles of the application.

## 1. Information Sent to ReggieSoft

CitrusRemote has no accounts or developer-operated backend. The app does not send app usage, device details, pairing information, or keyboard content to ReggieSoft, advertising networks, or analytics services. There are:
- No user accounts or login systems
- No telemetry or analytics frameworks
- No advertising networks or tracking SDKs
- No background tracking or location monitoring

To provide remote-control features, CitrusRemote communicates directly with Apple TV devices you select on your local network. Those local transfers are described below.

## 2. Local Network Communication

Remote-control communication occurs on your local Wi-Fi or Ethernet network:
- **Device Discovery**: The app uses ZeroConf/mDNS to discover Apple TV devices. It reads each discovered device's name, model, and local IP address to show it in the app. These details are not uploaded to a server.
- **Pairing and Control**: When you start pairing, the PIN displayed by Apple TV is used in the Companion pairing exchange. Pairing data and the resulting credentials are exchanged directly with the selected Apple TV. After pairing, remote commands use the authenticated Companion connection, which encrypts messages using keys shared between the app and Apple TV. CitrusRemote does not use a cloud relay.

## 3. Pairing Credentials & Local Storage

When pairing with an Apple TV, authentication credentials are generated:
- Credentials are saved in private Android app storage (`AppleTVPrefs`) and used to reconnect to that Apple TV.
- CitrusRemote's Android backup rules exclude `AppleTVPrefs.xml` from cloud backups and device-to-device transfers.
- Credentials remain in app storage until you unpair the device or clear app data.

## 4. Keyboard Input

When using the keyboard input feature:
- CitrusRemote reads the focused text from Apple TV into the app so you can edit it. Text you type is sent directly to the selected Apple TV over the encrypted Companion connection. It may include logins or passwords.
- CitrusRemote does not save keyboard text to disk or send it to ReggieSoft or a cloud service.
- The Android keyboard app you choose also handles keystrokes according to its own privacy practices.

## 5. Open Source Transparency

CitrusRemote is open source under the MIT license. You can inspect the source code, verify network and data handling practices, and build the application yourself.

## 6. Contact

If you have questions or concerns about this privacy policy, contact:
- Email: [support@reggiesoft.com](mailto:support@reggiesoft.com)
- Web: [https://reggiesoft.com](https://reggiesoft.com)
