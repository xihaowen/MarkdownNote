# Release checklist

- [ ] `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` 全部通过
- [ ] lint 为 0 Error，不使用 baseline 隐藏问题
- [ ] APK 签名验证通过
- [ ] 依赖树不含已删除的商业化 SDK、公式排版扩展或远程 Mermaid 运行库
- [ ] Manifest 只声明实际使用的权限
- [ ] API Key、keystore、`local.properties` 和构建产物未被 Git 跟踪
- [ ] Android 8、13、14/15 的核心流程完成冒烟验证
- [ ] 手机与平板布局完成冒烟验证
- [ ] 低风险依赖更新告警已评估；通过 Dependabot 后续维护

当前非阻断告警与取舍见 [KNOWN_ISSUES.md](KNOWN_ISSUES.md)。
