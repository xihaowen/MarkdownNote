# MarkdownNote

MarkdownNote 是一个使用 Kotlin、Jetpack Compose、Room 和 Material 3 构建的本地优先 Android Markdown 笔记应用。

## 功能

- 逐行实时 Markdown 编辑、阅读、大纲、字数统计和语法高亮
- 笔记收藏、搜索、回收站与待办事项
- 通过 Storage Access Framework 打开外部 Markdown 文件夹
- 本地打包的 Mermaid 图表渲染
- 可选的 OpenAI 兼容、Anthropic 和 Gemini AI 服务
- 手机和平板自适应界面

数学公式会保留原始 Markdown 内容，但当前版本不进行 LaTeX 排版。

## 构建

要求：JDK 17、Android SDK 36。

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

Release 签名和 GitHub Release 流程见 [发布指南](docs/RELEASING.md)。

## 数据与隐私

笔记默认保存在本地。只有在用户主动使用 AI 功能时，相关指令、选中内容、笔记上下文和启用的记忆上下文才会发送到用户选择的 AI 服务。详见 [隐私说明](PRIVACY.md)。

## 贡献与安全

- [贡献指南](CONTRIBUTING.md)
- [安全策略](SECURITY.md)
- [架构说明](docs/ARCHITECTURE.md)
- [第三方许可](THIRD_PARTY_NOTICES.md)

## 维护者

- [xihaowen](https://github.com/xihaowen)

## License

Copyright 2026 MarkdownNote contributors.

Licensed under the [Apache License 2.0](LICENSE).
