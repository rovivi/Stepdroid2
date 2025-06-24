# StepDroid

StepDroid is a lightweight emulator for Android that allows players to enjoy music and dance games by using StepMania 5 SCC files. This project is perfect for enthusiasts looking to bring their custom dance routines and music tracks on the go.

## Features

- **File Support**: Specifically designed to read and play StepMania 5 SCC files.
- **Portable Dance Experience**: Brings the rhythm game experience to your Android device.
- **Ongoing Development**: Actively developed to add new features and enhance user experience.

## How to Use

1. Install the APK on your Android device.
2. Load your StepMania 5 SCC files into the app.
3. Start playing and enjoy your custom dance routines!

## Development Setup

To build StepDroid you need the Android SDK installed. Set the `ANDROID_HOME` environment variable or create a `local.properties` file with the path to your SDK.

```bash
sdk.dir=/path/to/Android/sdk
```

The project uses **Jetpack Compose**. Ensure you are running JDK 17 and a recent version of Android Studio (Hedgehog or newer). Compose libraries are managed with the Compose BOM so compatible versions are resolved automatically.

Use the Gradle wrapper to build and run static analysis:

```bash
./gradlew assembleDebug
./gradlew lint
```

Recommended build commands:

```bash
# run unit tests and lint
./gradlew check

# create a release APK
./gradlew assembleRelease
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for more information about contributing.

## Contributing

Contributions to StepDroid are welcome! If you have ideas for improvements or new features, please read the `CONTRIBUTING.md` file for more information on how to contribute.

## Extending data sources

Song files can come from multiple locations. The `FileSource` interface abstracts
the origin of the data with `open()`, `read()` and `close()` methods. The default
implementation `LocalFileSource` reads from the filesystem, but additional
sources (e.g. assets, SAF or network downloads) can be added by implementing
`FileSource` and wiring it into the repository responsible for loading songs.

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.
