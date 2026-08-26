# Architecture

MarkdownNote 采用单 Activity、Jetpack Compose、MVVM 和分层数据访问结构。

- `data/local`：Room 实体、DAO、数据库和 migration。
- `data/repository`：笔记、待办和 SAF 文件夹访问。
- `data/ai`：Provider 抽象、协议实现、人格、记忆和后台整合。
- `ui`：按功能划分的 Screen、ViewModel 和共享组件。
- `worker`：回收站清理与 AI 后台任务。

`MainActivity` 的 NavHost 只负责首页、笔记阅读/编辑和 AI 人格编辑。设置、回收站和文件夹浏览使用统一的 Glass Sheet 容器。

应用数据库当前为版本 7。Release schema 变更必须提供显式 migration，不能依赖 destructive fallback。
