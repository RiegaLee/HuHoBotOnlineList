# 修改日志

## 1.22.0 - 2026-09-23

以未重构版本 `1.21.3` 为版本基线，本次版本完成 OnlineList Addon、渲染界面和头像链路的整体重构。

### HuHoBot Addon 接入

- 工程迁移到独立的 `huhobotAddon/HuHoBotOnlineList` 目录。
- 直接对接当前 HuHoBot 主线的 `QClient.registerCommand(Addon, BaseCommand)`。
- 移除旧 `OnBotCommand` 后备接入与编译期 HuHoBot ABI 桩。
- 保持 Java 8 字节码和 Spigot API 1.16.5 基线，不引入 Paper API、paperweight 或 NMS。

### 在线列表界面

- 新版透明材质 UI 成为唯一渲染路径。
- 完全移除旧机器人 Logo、三段雾蓝底图资源及旧主题运行分支。
- 内置无 Logo 默认渐变底图；外部 PNG 缺失或关闭时仍使用新版界面。
- 支持安全的本地 PNG 底图及 `cover`、`stretch` 两种适配方式。
- 卡片使用低透明浅烟灰纵向渐变、1.2 px 白色描边和轻量文字阴影，不使用重度模糊或深色卡片遮罩。
- 高亮壁纸支持自适应整图压暗，默认材质透明度为 `0.12`、压暗上限为 `0.38`。
- 标题固定为无 Logo 的透明胶囊；“在线列表”使用 JAR 内嵌字体子集渲染。
- 玩家区域采用“列表容器 + 单人卡片”两层结构，完整保留 Minecraft 头像方形像素。
- 取消头像旁的蓝色在线状态圆点，在线人数统一显示为 `9/100` 格式。

### 玩家头像

- 优先使用 Bukkit Profile，必要时兼容读取底层 GameProfile textures。
- 可选接入 SkinsRestorer v15+ 作为后备皮肤来源。
- 支持 64×32、64×64 及等比例高分辨率皮肤，叠加正脸和帽子层。
- 最后一次成功头像同时写入有界内存缓存和 UUID 磁盘缓存。
- 下载任务拒绝、Profile 暂时缺失或服务器重启后均可尝试历史头像。
- 所有真实皮肤入口失败时使用代码内置的 Minecraft Steve 正脸。
- 本地测试名单取消旧随机头像，统一使用同一份内置 Minecraft Steve 正脸并走正式渲染管线。

### 构建与验证

- 构建脚本检查最终 JAR 的必需资源与 Bukkit/Spigot 依赖边界。
- 禁止把 HuHoBot、QQ SDK、Bukkit、Paper 或 NMS 类打入 Addon JAR。
- 删除旧主题图片的构建要求，并增加“旧主题资源不得重新进入 JAR”的校验。
- 增加默认底图、自定义底图、高亮底图、完整头像像素、磁盘缓存和任务拒绝测试。

## 1.21.3

- 重构前的 OnlineList 版本基线。
