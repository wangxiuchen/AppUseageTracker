# 应用统计 AppSpy

> 看清自己每天用了哪些应用。纯本地、不联网、不上传任何数据。

---

## 功能

**今日页**——实时显示当天每个 App 的使用情况：
- 打开次数（每次切到前台算一次）
- 前台使用时长（仅统计真正在屏幕上的时间，后台挂起不算）
- 顶部汇总：总时长 / 总次数 / 活跃 App 数
- 支持按时长或按次数排序，下拉刷新

**趋势页**——查看选定 App 在过去 7 天或 30 天的走势：
- 可选多个 App 同屏对比折线图
- 时长 / 次数两种指标切换
- 区间汇总：总时长、总次数、日均

**授权引导**——首次打开时引导开启「使用情况访问权限」，并提示小米手机的额外放行步骤。

---

## 截图

| 授权引导 | 今日 | 趋势 |
|:---:|:---:|:---:|
| *(首次打开)* | *(实时数据)* | *(折线对比)* |

---

## 技术栈

| 模块 | 技术 |
|---|---|
| UI | Kotlin + Jetpack Compose + Material 3 |
| 数据来源 | `UsageStatsManager`（系统 API，不 hack 私有数据） |
| 本地存储 | Room（SQLite） |
| 后台任务 | WorkManager（每日快照 + 补抓缺口） |
| 依赖注入 | Hilt |
| 图表 | 自绘 Canvas 折线图 |
| 最低版本 | Android 10（minSdk 29） |
| 目标版本 | Android 15（targetSdk 35） |

---

## 数据说明

- **次数**：每次切到前台算一次，来回切会累加（不去重）。
- **时长**：仅统计真正在前台的时间（`ACTIVITY_RESUMED → ACTIVITY_PAUSED`），后台挂起不计入。
- **范围**：只统计在桌面有启动图标的用户 App，系统组件（桌面、输入法、SystemUI 等）全部过滤。
- **隐私**：所有数据仅存本机 SQLite，无网络权限，不上传任何数据。

---

## 本地编译

本项目使用 GitHub Actions 云端编译，无需本地搭建 Android 开发环境。

如果你有本地环境（JDK 17 + Android SDK），运行：

```bash
./gradlew assembleDebug
```

APK 产物在 `app/build/outputs/apk/debug/app-debug.apk`。

---

## GitHub Actions 云端编译

推送到 `main` 分支后自动触发编译，在仓库的 **Actions** → **Releases** 页面下载 APK。

工作流文件：[`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml)

---

## 安装注意事项

1. 下载 `app-debug.apk` 后，手机需开启「允许安装未知来源应用」。
2. 首次打开按引导开启「使用情况访问权限」。
3. **小米 / 红米（HyperOS / MIUI）**：还需在「手机管家」→「应用管理」→ 找到本 App → 开启「自启动」+ 省电策略设为「无限制」，否则后台统计可能被系统杀掉。
4. **华为 / 荣耀**：在「设置」→「应用」→「应用启动管理」里手动管理本 App，关闭自动管理。

---

## 项目结构

```
app/src/main/java/com/appspy/
├── data/
│   ├── db/              Room 数据库（AppMeta / DailyUsage / SessionRecord）
│   └── repository/      统一数据访问层
├── di/                  Hilt 依赖注入模块
├── ui/
│   ├── permission/      授权引导页
│   ├── today/           今日页 + ViewModel
│   ├── trend/           趋势页 + ViewModel + App 选择器
│   └── theme/           颜色 / 字体 / 主题
├── util/
│   ├── UsageStatsHelper.kt   采集算法（次数 / 时长 / 会话解析）
│   └── TimeFormatter.kt      时长格式化
└── worker/
    └── DailySnapshotWorker.kt  每日后台快照任务
```

---

## v1 不做的事

- 超时提醒 / 使用限制
- 数据导出（CSV / JSON）
- iOS / 桌面端
- 云同步 / 账号
- 桌面小组件
- 小时级细粒度图表

---

## License

MIT
