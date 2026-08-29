# PASS-001 | 构建 | 通过 | assembleDebug 构建成功

- 描述：`./gradlew assembleDebug` 在 Java 17 / Gradle 8.14 下一次通过，产物 18M，version 0.0.0(local)，仅 Kotlin 警告无编译错误。
- 复现：`cd /workspace/pim-platform/src/client-android && ./gradlew assembleDebug`
- 预期：BUILD SUCCESSFUL，生成 `app/build/outputs/apk/debug/app-debug.apk`
- 实际：BUILD SUCCESSFUL in 1m 04s，109 tasks，产物 `/workspace/pim-platform/src/client-android/app/build/outputs/apk/debug/app-debug.apk` (18M)，`output-metadata.json` versionCode 1
- 证据：`app/build/outputs/apk/debug/app-debug.apk`、`app/build/outputs/apk/debug/output-metadata.json`、`gradle-wrapper.properties:distributionUrl=gradle-8.14-bin.zip`、log `BUILD SUCCESSFUL in 1m 04s`
