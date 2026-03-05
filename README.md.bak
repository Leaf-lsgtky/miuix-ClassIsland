# 叶屿课表 LeafIsland

一款简洁的 Android 课程表应用，采用 [miuix-compose](https://github.com/miuix-kotlin-multiplatform/miuix) 构建，原生适配 HyperOS 设计风格。

## 功能

- **ICS 导入** — 导入 `.ics` 格式的课程表文件（支持 WakeUp 课程表等应用导出）
- **今明两日课程** — 首页清晰展示今天和明天的课程安排，包括时间、节次、地点、教师
- **课前提醒** — 自定义提前 10-60 分钟通知提醒，支持开机自动恢复闹钟
- **HyperOS 超级岛** — 适配 Focus Island，摘要态显示课程名与教室，展开态显示完整信息，支持"详情"快捷跳转
- **通知自动清除** — 上课后 5 分钟自动清除提醒通知

## 截图

> TODO

## 技术栈

- Kotlin 2.3 + Jetpack Compose
- [miuix-compose](https://github.com/miuix-kotlin-multiplatform/miuix) UI 组件库
- AndroidX Navigation3
- AlarmManager 精确闹钟调度
- HyperOS Focus Island param_v2 协议

## 构建

项目使用 GitHub Actions 自动构建，查看 [Actions](../../actions) 页面获取最新构建产物。

本地构建需要 JDK 21：

```bash
./gradlew assembleDebug
```

## 使用说明

1. 安装应用后，点击右上角 **+** 按钮导入 `.ics` 课程表文件
2. 首页自动展示今天和明天的课程
3. 点击左上角 **设置** 图标，调整提前提醒时间（拖动滑块或点击输入精确分钟数）
4. HyperOS 设备上课前提醒会以超级岛形式展示

## 许可证

Apache-2.0
