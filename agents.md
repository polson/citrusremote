# Agent Instructions

- Remember going forward to `git commit` after each phase of a plan is complete.
- After every successful change, commit and push it.
- Always verify that changes compile after implementing something (run `./gradlew assembleDebug` or similar build command).

## Android Build Environment

- This project targets Android API 36.1 and Chaquopy Python 3.12. If the build reports a missing SDK, check the host installation before concluding it is absent. The current Bazzite host's JetBrains Runtime is at `~/.local/share/JetBrains/Toolbox/apps/rider/jbr`; Python 3.12 is at `~/.local/bin/python3.12`. These may need to be added explicitly to `JAVA_HOME` and `PATH` in the restricted shell.
- If no Android SDK is available, install the required packages (`platforms;android-36.1`, `build-tools;36.1.0`, and `platform-tools`) into `/tmp/fruityremote-android-sdk` and set both `ANDROID_HOME` and `ANDROID_SDK_ROOT` to that directory. Use `/tmp/fruityremote-gradle-home` for `GRADLE_USER_HOME` if the default Gradle home is not writable.
- `app/build.gradle.kts` currently points Chaquopy to the macOS-only `/opt/homebrew/bin/python3.12`. For a Linux build, temporarily point it to the installed Python 3.12 executable and restore the file immediately afterward. Confirm `app/build.gradle.kts` has no diff before staging.
- The restricted shell may block host network access and writes under `.git`. For explicitly requested fetch, commit, or push operations, retry with escalated host access instead of treating those sandbox failures as missing tools or repository problems.
