# IIDX Chart Viewer（原生 Android）

这个目录是当前目标 APK 的纯 Android 实现，使用 Kotlin + Jetpack Compose。根目录的 Vite 页面只是早期原型，不是最终运行入口。

## 数据和交互

- BJM 登录：仅此流程使用 Android WebView，登录完成后自动返回主界面并同步成绩。
- BJM 成绩：通过原生 HTTP + gRPC-Web 请求获取，Cookie 由 Android `CookieManager` 持久保存，成绩快照保存到 `SharedPreferences`。
- Textage：首次启动由原生 `HttpURLConnection` 获取 `/score/` 声明的同域 `titletbl.js`、`datatbl.js`、`actbl.js`、`cstbl*.js`、`scrlist.js` 等数据表，在 APK 内关联歌曲元数据、难度、NOTE 数和 BPM；后续启动重新检查这些脚本。用户点进具体难度后，才获取谱面页面及同源脚本，并把该难度的谱面 JSON 缓存到应用私有目录。不打开 Textage 页面、不发送外部浏览器 Intent。
- 首次元数据初始化页面显示总进度和当前曲名；铺面时序数据按需加载。
- 谱面：在 Compose `Canvas` 中本地绘制；详情页提供流速、播放/暂停和重置。Textage 页面格式未识别时会明确提示，不会把伪造数据当作真实谱面。

## 构建

```text
ANDROID_SDK_ROOT=/Users/jhhuang/Library/Android/sdk \
  /Users/jhhuang/code/stock-tracer/gradlew -p android :app:assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`
