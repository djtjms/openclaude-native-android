# OpenClaude Native Android

Native Android chat interface for [OpenClaude](https://github.com/gitlawb/openclaude) — an on-device AI coding agent.

Built with Jetpack Compose, NDK/JNI, and GitHub Actions CI. No Termux build required.

## Architecture

```
┌───────────────────────────────┐
│  OpenClaude Native Android    │
│                               │
│  ┌─────────────────────────┐  │
│  │  Compose UI             │  │
│  │  (MainActivity.kt)      │  │
│  └────────┬────────────────┘  │
│           │ Flow<ChatEvent>   │
│  ┌────────▼────────────────┐  │
│  │  EngineClient           │  │
│  │  ┌─────┴──────┐        │  │
│  │  │ Exec mode  │Http    │  │
│  │  │(openclaude│ fallback│  │
│  │  │ binary)   │(bridge) │  │
│  │  └─────┬──────┘        │  │
│  └────────┼───────────────┘  │
│  ┌────────▼───────────────┐  │
│  │  EngineNative (JNI)    │  │
│  │  libocengine.so        │  │
│  └────────────────────────┘  │
└───────────────────────────────┘
```

- **Exec mode** (default when `openclaude` binary is found): spawns the CLI directly via `ProcessBuilder` — no bridge server needed.
- **HTTP mode** (fallback): POSTs to the existing Termux bridge server at `http://127.0.0.1:8787/api/chat`.
- **JNI layer**: `libocengine.so` compiled from C++ via CMake, loaded via `System.loadLibrary`.

## Repo Layout

```
openclaude-native-android/
├── .github/workflows/
│   └── android-build.yml    # CI: build + sign + upload APK
├── android/
│   ├── build.gradle.kts     # Root build config (AGP 8.4.2, Kotlin 2.0.20)
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── gradlew              # Gradle wrapper script
│   ├── gradle/wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── app/
│       ├── build.gradle.kts # App module (Compose, NDK, deps)
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/com/openclaude/
│           │   ├── MainActivity.kt    # Compose UI
│           │   ├── EngineClient.kt    # Chat engine adapter
│           │   └── EngineNative.kt    # JNI bindings
│           ├── cpp/
│           │   ├── CMakeLists.txt
│           │   └── native-lib.cpp
│           └── res/values/
│               ├── strings.xml
│               └── themes.xml
├── plugins/
│   ├── README.md            # Plugin authoring guide
│   └── hello-world/         # Sample plugin
│       ├── plugin.json
│       └── run.sh
├── .gitignore
├── LICENSE                  # Apache-2.0
└── README.md
```

## Build

### CI (recommended)

Push to GitHub — the workflow builds, signs (if secrets configured), and uploads the APK:

```bash
git push origin main
# → APK available in Actions → workflow run → Artifacts
```

### Local

Prerequisites: Android SDK 34, NDK 26.3.11579264, JDK 17.

```bash
cd android
./gradlew assembleRelease
# → android/app/build/outputs/apk/release/app-release.apk
```

## Configuration

| Environment variable | Default | Description |
|---|---|---|
| `BRIDGE_URL` | `http://127.0.0.1:8787` | HTTP fallback bridge URL |
| `OPENCLAUDE_BIN` | `openclaude` | Path to openclaude binary |

## Signing (CI)

Set these secrets in your GitHub repo:

- `SIGNING_KEY` — base64-encoded keystore
- `KEY_ALIAS` — key alias
- `KEY_STORE_PASSWORD` — keystore password
- `KEY_PASSWORD` — key password

Without secrets, the unsigned APK is still produced and uploaded (installable with `adb install`).

## Plugins

See `plugins/README.md` for how to write and install OpenClaude plugins.

## License

Apache-2.0