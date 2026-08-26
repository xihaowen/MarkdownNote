# Contributing

感谢你为 MarkdownNote 做出贡献。

1. 从 `main` 创建功能分支。
2. 保持修改聚焦，不提交密钥、签名文件、构建产物或用户数据。
3. 新功能和缺陷修复应包含对应测试。
4. 提交前运行：

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

涉及数据库 schema 时必须增加显式 Room migration；涉及 AI 网络协议时需说明数据边界与失败行为。
