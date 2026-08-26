# Releasing

## 本地签名构建

签名信息可放在未提交的根目录 `local.properties`，或通过环境变量提供：

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

```bash
./gradlew testDebugUnitTest assembleDebug
./gradlew lintDebug
./gradlew assembleRelease
```

质量任务分成独立 Gradle 调用，以避开 AGP lint 与 KSP release 同时分析临时生成目录时的工具链竞态；每个门禁仍会独立失败并阻止发布。

## GitHub Release

仓库工作流监听 `v*` tag。GitHub 仓库需要配置：

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

工作流会移除 tag 的 `v` 前缀后写入 `APP_VERSION_NAME`，使用 `GITHUB_RUN_NUMBER` 作为 `APP_VERSION_CODE`，验证签名并发布 APK 与 SHA-256 校验文件。
