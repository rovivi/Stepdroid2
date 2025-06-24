# Contributing to StepDroid

Thank you for considering contributing to StepDroid! Please follow these steps to get your development environment ready:

1. **Android SDK**: Install the Android SDK (API 34 or newer). Set the `ANDROID_HOME` environment variable or create a `local.properties` file with:
   ```
   sdk.dir=/path/to/Android/sdk
   ```
   This project uses **Jetpack Compose**, so you need JDK 17 and a recent version of Android Studio.
2. **Build**: Use the Gradle wrapper to build the project:
   ```
   ./gradlew assembleDebug
   ```
   To build a release APK run:
   ```
   ./gradlew assembleRelease
   ```
3. **Static Analysis**: Run Android Lint before submitting a pull request:
   ```
   ./gradlew lint
   ```
   Lint requires the Android SDK and may fail if the SDK is not installed.
   You can also run all checks with:
   ```
   ./gradlew check
   ```
4. **Pull Request**: Create a branch for your changes and open a pull request describing your updates.

We appreciate fixes, feature requests, and general improvements. Have fun hacking on StepDroid!
