# 项目接续总览

## 当前状态

- 当前版本：`1.22.0`
- 上一版基线：`1.21.3`
- 项目地址：`https://github.com/RiegaLee/HuHoBotOnlineList`
- 构建系统：Gradle Wrapper 8.14.5
- 编译目标：Java 8 字节码
- 服务端接口：Spigot API 1.16.5（仅使用 Bukkit/Spigot API，不使用 Paper API 或 NMS）
- HuHoBot 接入：当前主线的 `QClient.registerCommand(Addon, BaseCommand)`
- 必需宿主：`HuHoBotPenguin`
- 可选软依赖：SkinsRestorer

## 执行链路

```text
QQ群 /在线列表 [页码]
  -> HuHoBot Addon + BaseCommand
  -> Bukkit 主线程捕获不可变玩家快照
  -> 渲染线程分页并生成图片
  -> 皮肤线程解析、下载和缓存头像
  -> OnlineListRenderer 绘制唯一的透明材质 UI
  -> ImageReplyApi 以 PNG 字节回复原 QQ 消息
```

## 界面与底图

- 新版透明材质 UI 是唯一渲染路径，不再包含旧机器人 Logo 或三段雾蓝主题。
- 未提供外部 PNG 时使用内置无 Logo 渐变底图。
- 外部底图目录：`plugins/HuHoBotOnlineList/assets/custom/backgrounds/`。
- 支持 `cover` 和 `stretch`；文件名、16 MiB 大小与 3200 万像素上限均经过校验。
- 卡片使用浅烟灰透明渐变、细白描边和轻量文字阴影；默认不做玻璃模糊。

## 头像链路

- Bukkit Profile → 底层 GameProfile → SkinsRestorer → 内存缓存 → UUID 磁盘缓存 → 内置 Steve。
- 头像保持完整方形像素，不做圆角裁切，也不叠加在线状态圆点。
- 磁盘缓存目录：`plugins/HuHoBotOnlineList/cache/avatars/`。

## 关键约束

- 最终 JAR 不得包含 HuHoBot、QQ SDK、Bukkit、Paper 或 NMS 类。
- Bukkit 玩家对象只在主线程读取。
- PNG 渲染、HTTP 头像下载和 SkinsRestorer 查询不得阻塞 Bukkit 主线程。
- 头像解析失败不能阻止图片生成。
- 正式环境保持 `test.fake-player-count: 0`。
- 外部底图缺失时必须回退到新版内置底图，不能恢复旧主题。

## 构建

先构建所依赖的 HuHoBot Bot 模块，然后运行：

```powershell
.\gradlew.bat clean build
```

产物：`build/libs/HuHoBot-OnlineList-1.22.0.jar`。
