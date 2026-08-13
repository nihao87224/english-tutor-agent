# Android

M0-T03 Android 工程基线。

## Baseline

- Kotlin
- Jetpack Compose + Material 3
- Hilt
- ViewModel + StateFlow
- 单向数据流
- Gradle Wrapper
- Debug 构建

## Modules

```text
app  Android application entrypoint, Hilt application graph and minimal Compose shell
```

M0-T03 只初始化 `app` 模块。后续 `core-*` 和 `feature-*` 模块按 M1/M2/M3 纵向闭环逐步增加。

## Commands

From this directory:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

On Unix-like shells:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Local SDK

If `ANDROID_HOME` or `ANDROID_SDK_ROOT` is not set, create a local-only `local.properties` file:

```text
sdk.dir=<absolute Android SDK path>
```

Do not commit `local.properties`.

## UI prototype

```text
docs/ui/english-tutor-agent-ui-prototype-v1.html
```

不要一次性把原型所有页面实现为空壳。按 M1/M2/M3 纵向闭环逐步实现。

## Out of scope for M0-T03

- Business screens
- Network API calls
- Room, DataStore, WorkManager and Media3 integration
- Push-to-Talk recording state machine
- Release signing configuration
