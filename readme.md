# IIDX 谱面浏览器

当前版本：0.4.11

一个纯 Android 的 beatmania IIDX 谱面浏览与本地播放器应用，使用 Kotlin 和 Jetpack Compose 实现。

## 当前功能

- 从 Textage 获取歌曲元数据，谱面在用户打开具体难度时按需获取并缓存。
- 首页支持曲名/曲师搜索，以及按版本和等级筛选。
- 支持 SP / DP、难度切换、搜索、BPM 变化、变拍号和长押谱面。
- 提供本地谱面播放器：播放/暂停、进度拖动、前后小节、Hi-Speed、1P / 2P、MIRROR、RANDOM 轨道配置、小节线和变速线显示。
- 支持登录 BJM 并同步成绩；登录流程使用应用内 WebView。
- 首页菜单提供谱面数据更新、BJM 成绩同步、GitHub Release 更新检查和项目主页入口。
- 设置中可以关闭每天自动检查更新；发现新版本后可在应用内下载 APK 并发起安装。

## 构建

```bash
./gradlew -p android :app:testDebugUnitTest :app:assembleDebug
```

Debug APK 输出在：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接的 Android 设备或模拟器：

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

## 项目主页

<https://github.com/HarroYuz/iidx-chart-viewer>
