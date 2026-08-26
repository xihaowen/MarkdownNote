# Known issues

以下项目不阻断 `1.0.0` 发布：

- `targetSdk` 按本次发布范围保持 35，`compileSdk` 为 36；升级 target 前需完成 Android 16 行为变更测试。
- lint 会提示若干 AndroidX、Compose、Room、WorkManager、AGP 和 Gradle 可更新版本。本次只升级兼容 Kotlin 2.2 与安全发布所需的版本，后续由 Dependabot 分批提交并逐项回归。
- lint 的 `UseKtx` 建议和一个装箱性能提示不影响正确性，避免在发布准备阶段进行无关机械重构。
- `androidx.security:security-crypto:1.1.0` 已弃用其高层 API，但仍是本版本按计划使用的稳定依赖；迁移到直接 Android Keystore 存储需要单独的数据兼容方案。
- 构建工具可能提示部分 AndroidX 原生库无法剥离调试符号，AGP 会按原样安全打包。

任何依赖升级都应重新执行完整质量门禁和 release 冒烟测试。
