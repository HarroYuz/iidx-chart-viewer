# IIDX 谱面浏览器

当前版本：1.1.1

一个纯 Android 的 beatmania IIDX 谱面浏览与本地播放器应用，使用 Kotlin 和 Jetpack Compose 实现。

## 当前功能

- 从 Textage 获取曲目元数据，谱面在用户打开具体难度时按需获取并缓存。
- 曲目列表支持曲风/曲名/曲师搜索，搜索维度使用相连的多选按钮；版本和等级筛选支持展开与收起动画。
- 曲目详情提供独立的难度雷达切换栏，难度卡片继续用于打开谱面；曲目详情和谱面浏览均显示成绩达成时间。
- 支持 SP / DP、难度切换、搜索、BPM 变化、变拍号和长押谱面。
- 提供本地谱面播放器：播放/暂停、进度拖动、前后小节、Hi-Speed、Floating Hi-Speed（绿字）、可选不随 BPM 变化的固定流速、1P / 2P、DP FLIP、MIRROR、RANDOM 轨道配置、小节线和变速线显示。
- 支持登录 BJM 并同步成绩，按 BJM 音乐数据库匹配曲目、难度、EX SCORE 和 MISS COUNT；登录流程使用应用内 WebView。
- 设置中支持全量数据同步，也可以分别同步 Textage 曲目库、BJM 曲目库和用户成绩库；已同步的数据索引会保存到本地。
- 首页菜单提供谱面数据更新、GitHub Release 更新检查和项目主页入口。
- 曲目卡片与谱面播放器之间提供连续展开与返回动画；设置中的“禁用视觉效果”默认关闭，开启后直接切换页面。
- 设置中可以关闭每天自动检查更新；发现新版本后可在应用内下载 APK 并发起安装。

## 雷达与成绩匹配

雷达及参考 NOTE 数来自 BJM 的公开 LDJ 数据库，保存在本地并检查更新。雷达采用统一的 0–200 刻度，以最高维度决定填充和描边颜色；同分按固定轴序选择颜色。六个色系参考 [KONAMI 的雷达选择界面](https://p.eagate.573.jp/game/2dx/33/event/arena_mode/index.html)，并调整为适合本应用浅色背景的颜色。

BJM 成绩接口没有独立的 DJ RATE 字段，其网站使用另一份 NOTE 数据库和 EX SCORE 计算等级。本应用保存这一参考 NOTE 数和计算等级，仅在参考 NOTE 数与目标谱面一致、EX SCORE 合法、DJ RATE 一致时展示成绩。缺少依据的旧成绩继续保留在历史记录中，同步到相同记录后补充依据。这个校验可排除 NOTE 数不同的版本，无法区分 NOTE 数相同但编排不同的谱面版本。

Textage 当前街机版未收录的曲目以红色标题标识，并区分已删除与家用版曲目。曲目候选优先选可浏览的当前收录版本；成绩历史在 NOTE 数校验通过的候选中优先选当前收录版本。旧版可能缺少对应 BJM 成绩或雷达，详情页会说明原因。

## 项目结构

这是原生 Android 项目，早期 Web 原型已移除，不再需要 Node.js 或 npm。

```text
android/app/src/main/
├── java/com/harroyuz/iidxchartviewer/
│   ├── MainActivity.kt       # Activity 入口与系统事件
│   ├── app/                  # ViewModel 状态、异步任务与事件
│   ├── data/
│   │   ├── local/            # 本地存储、缓存和 JSON 编解码
│   │   └── remote/           # Textage、BJM、GitHub 客户端与解析器
│   ├── domain/               # 数据模型、曲目匹配、成绩、同步与播放器规则
│   └── ui/
│       ├── auth/             # BJM 登录
│       ├── catalog/          # 曲库与曲目详情
│       ├── history/          # 成绩历史和日历
│       ├── player/           # 播放、Canvas 绘制和设置
│       ├── settings/         # 数据同步与版本更新
│       ├── components/       # 通用界面组件
│       └── theme/            # 颜色、字体与形状
└── res/                      # Android 资源
```

业务规则不依赖 Android 界面。界面通过回调请求 ViewModel 修改状态；登录、安装和退出等系统操作由 Activity 处理。重构保留了 applicationId、存储键和缓存格式，可沿用已有数据。

## 构建

需要 JDK 17 或更新版本、Android SDK 36；在 `android/local.properties` 中配置本机 `sdk.dir`。也可直接用 Android Studio 打开 `android/` 目录。

```bash
./gradlew -p android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK 输出在：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接的 Android 设备或模拟器：

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

## 固定签名

`android/signing/` 包含用于保持 APK 更新兼容性的签名文件，已加入 Git 忽略列表，不会提交到仓库。需要生成与现有 APK 相同签名的设备，应先通过安全方式复制该目录，再执行上面的构建命令；未复制时会回退到本机默认 debug 签名，生成的 APK 不能覆盖已有安装包。

## 项目主页

<https://github.com/HarroYuz/iidx-chart-viewer>
