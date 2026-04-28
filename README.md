# GitHub Repository Browser

一个基于 Android 原生 Java 开发的 GitHub 仓库搜索 App。项目支持仓库搜索、分页加载、下拉刷新、失败重试、搜索历史、仓库收藏和详情页展示，适合作为 Android 面试项目展示。

## 功能特性

- GitHub 仓库搜索：支持输入关键字搜索 GitHub 仓库。
- 分页加载：基于 Paging 3 按页加载搜索结果，支持列表自动加载更多。
- 本地缓存：搜索结果先写入 Room，再由 PagingSource 提供给列表展示。
- 下拉刷新与失败重试：支持刷新当前搜索结果，并在网络失败时提供重试入口。
- 搜索历史：本地保存最近搜索词，点击历史记录可快速重新搜索。
- 仓库收藏：支持收藏/取消收藏仓库，收藏数据独立持久化。
- 详情页降级展示：详情页先展示列表传入的基础数据，再异步请求最新详情，弱网或失败时仍可展示基础内容。
- README 预览：详情页通过 WebView 打开 GitHub 仓库 README 区域。

## 技术栈

- Java
- MVVM
- Paging 3
- Room
- Retrofit

项目中还使用了 LiveData、RemoteMediator、OkHttp、Gson、RecyclerView、SwipeRefreshLayout、Material Components、JUnit 和 Espresso。

## 项目架构

项目采用 MVVM 分层：

```text
Activity / Adapter
        ↓
ViewModel
        ↓
Repository
        ↓
Room + Retrofit + Paging
```

核心数据流：

```text
GitHub API -> RemoteMediator -> Room -> PagingSource -> RecyclerView
```

说明：

- `Activity`：负责 UI 渲染、用户交互和页面跳转。
- `ViewModel`：负责页面状态管理，例如当前搜索词、详情状态、收藏状态。
- `Repository`：统一封装网络、本地数据库和分页数据来源。
- `RemoteMediator`：负责从 GitHub API 拉取分页数据，并写入 Room。
- `PagingSource`：负责从 Room 分页读取数据并提供给列表。
- `Room`：保存搜索结果缓存、分页 key、搜索历史和收藏数据。

## 目录结构

```text
app/src/main/java/com/example/myapplication
├── MainActivity.java                  # 首页：搜索、历史、仓库列表、加载状态
├── core
│   ├── error                          # 统一错误映射
│   └── log                            # 日志工具
├── data
│   ├── local                          # Room 数据库、DAO、Entity
│   ├── remote                         # Retrofit API、DTO、网络客户端
│   └── repository                     # Repository 与 RemoteMediator
└── ui
    ├── detail                         # 仓库详情页
    ├── favorite                       # 收藏列表页
    └── adapter/viewmodel              # 首页列表、加载状态、搜索历史
```

## 核心亮点

### 1. Paging 3 + Room + RemoteMediator

项目没有手动维护 `page++`，而是通过 Paging 3 管理分页状态。`RemoteMediator` 负责网络请求和缓存同步，`PagingSource` 负责从 Room 中分页读取数据。这样 UI 层只依赖 Room 这一套稳定数据源，加载状态、刷新、重试和加载更多都交给 Paging 统一处理。

### 2. MVVM + LiveData

项目将页面交互、状态管理和数据访问拆分到 `Activity / ViewModel / Repository`。页面通过 LiveData 被动订阅数据变化，减少手动刷新 UI 的代码，并具备生命周期感知能力。

### 3. 搜索历史、收藏与详情页降级

搜索历史使用 `keyword` 作为主键实现自然去重，并通过 `searchedAt` 按最近使用排序。收藏数据单独存储在 `favorite_repos` 表中，避免和临时搜索缓存耦合。详情页先展示列表传入的 fallback 数据，再请求最新详情，接口失败时也不会出现空白页。

## 本地运行

1. 使用 Android Studio 打开项目。
2. 等待 Gradle Sync 完成。
3. 连接 Android 设备或启动模拟器。
4. 运行 `app` 模块。

也可以在命令行执行：

```powershell
.\gradlew.bat assembleDebug
```

运行单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

## 测试

项目包含：

- JVM 单元测试：验证错误映射等纯 Java 逻辑。
- Instrumentation 测试：验证收藏逻辑和首页基础流程。

相关目录：

```text
app/src/test
app/src/androidTest
```

## 面试介绍示例

这是一个 GitHub 仓库搜索 App。我主要使用 Java 原生 Android 开发，整体采用 MVVM 架构。列表部分使用 `Paging 3 + Room + RemoteMediator` 实现网络分页和本地缓存，UI 只从 Room 读取数据，网络请求成功后再驱动本地数据刷新。项目还实现了搜索历史、仓库收藏和详情页降级展示，弱网或详情接口失败时仍能展示基础信息，整体更接近真实 App 的数据流和用户体验。

## 后续可优化方向

- 引入 Hilt 做依赖注入，减少手动创建 Repository 和数据库实例。
- 使用 Kotlin、Coroutine、Flow 重构异步和状态流。
- 支持不同搜索词分别缓存，给 `repos` 和 `remote_keys` 增加 `query` 维度。
- 增加更完整的数据库 Migration，替代 `fallbackToDestructiveMigration`。
- 优化 WebView README 展示，或使用 GitHub Markdown API 自定义渲染。
