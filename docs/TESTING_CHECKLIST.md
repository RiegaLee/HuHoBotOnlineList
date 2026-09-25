# 测试与发布检查

## 自动验证

```powershell
.\gradlew.bat clean build
```

应满足：

- 所有 `*Test` 测试通过。
- 新版内置底图的空服预览写入 `build/preview/default-empty-preview.png`。
- 外部底图和高亮底图预览保持透明材质卡片，不出现旧机器人 Logo 或黑色卡片。
- Minecraft 头像四角像素完整，没有圆角裁切或蓝色在线状态圆点。
- 本地测试名单统一显示 JAR 内置的 Minecraft Steve 正脸，不生成随机头像，也不访问皮肤网络。
- UUID 磁盘缓存与下载任务拒绝回退测试通过。
- `verifyAddonJar` 通过，产物为 Java 8 字节码且不捆绑宿主或服务端实现类。

## Bukkit/Spigot 实机检查

- 在 Spigot 系服务端启动；插件不要求任何 Paper 专用接口。
- HuHoBot 启动较慢时，OnlineList 能等待客户端并完成 Addon 注册。
- `/附属插件` 中能看到 `HuHoBotOnlineList 1.22.0`。
- QQ 指令面板和群消息均能触发 `/在线列表`。
- `/在线列表 2` 能生成正确分页，越界页码返回文字提示。
- 空服务器、1 人、27 人、28 人和 65 人布局正常。
- 同群冷却与并发生成保护生效。

## 底图与界面检查

- 未提供外部 PNG 时使用新版内置渐变底图。
- PNG 位于 `plugins/HuHoBotOnlineList/assets/custom/backgrounds/` 时可替换底图。
- `cover` 保持比例并居中裁切，`stretch` 填满完整动态画布。
- 玩家卡片、信息区、页脚、边框和文字在明暗底图上仍清晰。
- 文件缺失时安全回退内置底图；非 PNG、超 16 MiB 或超 3200 万像素时给出明确错误。
- `../`、绝对路径和含目录的文件名不能逃逸指定目录。

## 头像检查

- Bukkit Profile 可用时显示真实头像。
- SkinsRestorer 可用时能作为后备来源。
- Profile 临时缺失或服务器重启后可使用 UUID 磁盘缓存。
- 网络不可用且无缓存时仍显示内置 Steve，不产生空白头像。
- Bukkit 玩家对象只在主线程读取，网络下载和渲染在线程池执行。
