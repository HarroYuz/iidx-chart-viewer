# 动画性能排查（2026-09-15）

## 已观察到的问题

模拟器为 Medium_Phone_API_36.1，1080×2400、约 2 GiB RAM。采样时系统约 1 GiB 交换空间已使用，应用的 Swap PSS 约 68 MiB；CPU 统计包含大量 major page faults 和 kswapd 活动。上一轮还出现过系统与应用无响应。因此该模拟器确实存在系统内存压力，不能把它的所有掉帧都归因于动画。

现有 1.1.3 包为 Debug。它的累计渲染统计包含 2,175 帧，掉帧 21.10%、P50 20 ms、P95 105 ms。这是混合操作记录，不是单段动画的基准。

代码检查发现：

- 详情切换使用 Compose 布局、共享元素和图层，没有需要整批预加载的动画图片。
- 雷达原先每次绘制都创建路径、计算三角函数和请求文字布局。
- 打开谱面会立即为其他难度准备缓存，且先获取页面再检查磁盘，容易与首次页面切换争用资源。
- 启动时还会初始化 WebView/CookieManager、加载本地索引并首次构建界面；这些与 ART/JIT 首次执行成本、系统换页都可能叠加。未改动登录机制。

## 探索性对照

以下短样本是在最终 1.2.0 动画修正之前采集，每组仅三次“曲库→详情→曲库”。Debug 与 Release 的曲库滚动位置不同，系统负载也不受控，不能据此宣称固定提升百分比。

| 样本 | 帧数 | 掉帧率 | P50 | P95 | P99 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1.1.3 Debug | 106 | 30.19% | 36 ms | 150 ms | 550 ms |
| 优化中的 Release，未强制 AOT | 88 | 28.41% | 26 ms | 97 ms | 650 ms |
| 同一 Release，模拟器强制 AOT | 106 | 27.36% | 27 ms | 121 ms | 150 ms |

通过 `cmd package compile -m speed -f` 在模拟器上做代码预编译后，长尾有所变化，但中位数与掉帧率没有明显变化，不能确认 JIT 是动画卡顿的主因。新安装后的首帧启动用时约 9.47 秒，预编译后重启约 2.07 秒；WebView/文件缓存和系统负载也已变化，不能把差值全归于预编译。`am start -W` 统计的是 Activity 首帧，不代表曲库数据完全就绪。

## 已实施的优化与边界

- 提供同签名、非 Debug 的 R8 Release 包；保留 Rhino 运行所需反射类及资源。
- APK 含依赖库提供的 Baseline Profile；未生成应用专用 Profile。直接安装 APK 后的编译时机仍由 Android 系统决定。模拟器的强制 AOT 实验不代表用户安装 APK 后自动处于同一状态。
- 雷达按组件尺寸与数据缓存网格、路径、文字布局，不缓存全库图像。
- 入场动画用图层平移；前景与外框缩放分离，并按屏幕底边计算起点。
- 谱面预解析延后 750 ms，先查磁盘、最多串行准备两个难度，切换或返回时取消。低 RAM、系统低内存、memoryClass 小于 128 MB 或可用内存小于 192 MiB 时跳过。读取缓存、解析及写缓存均在后台。
- 不启动隐藏页面/播放器预加载；启动加载页可禁用视觉效果，设置保持到下次启动。

## 实机复测

使用 Release 包，在相同设备、相同曲库位置、相同设置下分别记录首次进入及多次操作后的结果。还需分别测试“禁用视觉效果”开启和关闭，避免混入同步、录屏等额外负载。

```bash
adb shell dumpsys gfxinfo com.harroyuz.iidxchartviewer reset
# 手动执行固定次数的相同导航
adb shell dumpsys gfxinfo com.harroyuz.iidxchartviewer
adb shell dumpsys meminfo com.harroyuz.iidxchartviewer
```

若实机仍明显卡顿，应针对具体交互采集 Perfetto/Macrobenchmark，并生成应用专用 Baseline Profile；不应仅凭冷启动现象增加整页内存预加载。

参考：[Compose 性能配置](https://developer.android.com/develop/ui/compose/performance)、[Baseline Profile](https://developer.android.com/develop/ui/compose/performance/baseline-profiles)、[Compose 阶段与性能](https://developer.android.com/develop/ui/compose/performance/phases)。
