# HuHoBotOnlineList

HuHoBotOnlineList 是 HuHoBotPenguin 的 Spigot/Paper 附属插件。QQ群用户发送 `/在线列表` 后，插件会直接读取当前 Bukkit 服务端的在线玩家，生成带玩家头像的雾蓝玻璃风格图片并回复原消息。

插件直接读取当前 Bukkit 服务端状态，并在附属插件内完成图片渲染、PNG 编码与 QQ 图片回复。

## 功能

- 使用 `/在线列表 [页码]` 查询当前服务器在线玩家。
- 默认每页 27 人，采用 3 列 × 9 行布局。
- 管理员优先显示，管理员组与普通玩家组内分别按名称排序。
- 读取玩家当前 Minecraft 皮肤并显示头像；失败时自动使用占位头像。
- 根据人数自动拼接可延伸的三段式底图。
- 图片底部显示 `第 X / Y 页`。
- 图片消息只附带一个不可见空格，不额外发送人数说明文字。
- 提供本地假玩家与完整 QQ 发图链路测试。
- 内置图片字节回复 API，便于适配不同 HuHoBot 分支。

## 运行环境

- Spigot 或 Paper 1.16.5 及以上
- Java 8 及以上
- HuHoBotPenguin Spigot 适配器 v1.5.0+

当前构建目标为 Java 8 字节码。已实测环境：

- Paper 1.21.11
- Java 21
- HuHoBotPenguin 1.2.2+

插件依赖 HuHoBotPenguin 提供 QQ 机器人连接和扩展注册 API。`plugin.yml` 使用 `softdepend` 保证常见安装环境中的加载顺序，同时允许插件在不同 HuHoBot 分支上尝试兼容入口。

v1.0.0 起使用 HuHoBotPenguin v1.5.0+ 的扩展 API（`registerAddon` + `registerBotCommand`），命令会自动出现在 `/帮助` 和 `/addons` 中。

## 下载

请从当前项目的 Release 页面下载 `HuHoBotOnlineList-x.y.z.jar`。也可以按照下方“构建”章节自行编译。

[下载最新版本](https://github.com/RiegaLee/HuHoBotOnlineList/releases/latest)
## 安装

1. 安装并正确配置 HuHoBotPenguin Spigot 适配器。
2. 将 `HuHoBotOnlineList-x.y.z.jar` 放入服务器的 `plugins/` 目录。
3. 完整启动一次服务器，让插件生成 `plugins/HuHoBotOnlineList/config.yml`。
4. 按需修改配置文件。
5. 完整重启服务器。
6. 在 HuHoBot 所在 QQ 群发送 `/在线列表`。

插件正常启动时，控制台会显示类似信息：

```text
[HuHoBotOnlineList] 在线列表已就绪，主题：雾蓝玻璃，图片回复：JAR 内置字节 API
```

若 HuHoBot 的 QQ 客户端尚未就绪，插件会等待客户端启动后自动注册命令。

## QQ 群使用

查看第一页：

```text
/在线列表
```

查看指定页：

```text
/在线列表 2
```

页码必须是正整数。请求不存在的页码时，机器人会回复当前总页数：

```text
没有第 2 页，当前共 1 页。
```

## 管理员识别与排序

以下任一条件满足时，玩家会显示为 `ADMIN` 并排在普通玩家之前：

- 玩家是服务器 OP。
- 玩家拥有 `huhobotonlinelist.priority` 权限。
- 玩家拥有 `sorting.admin-permission` 所配置的自定义权限。

管理员组和普通玩家组内均按玩家名称排序。默认权限声明：

```yaml
permissions:
  huhobotonlinelist.priority:
    default: op
```

## 配置说明

配置文件位置：

```text
plugins/HuHoBotOnlineList/config.yml
```

### 基础配置

```yaml
# QQ 群命令，不需要前导斜杠。
bot-command: "在线列表"

# 兼容命令入口是否请求加入 QQ 指令面板。
push-command-menu: true

# 图片左上角显示的服务器名称。
server-name: "MinecraftServer"

# 同一个群连续请求的冷却时间，单位为秒。
cooldown-seconds: 3
```

保持 `bot-command: "在线列表"` 时，插件会优先使用 HuHoBot 原生 `BaseCommand` 入口。自定义命令名称会使用扩展 API（`registerAddon` + `registerBotCommand`）与 `OnBotCommand` 事件兼容入口。

原生命令是否显示在 QQ 指令面板中由宿主 HuHoBot 的指令面板配置决定；`push-command-menu` 主要用于兼容入口。

### 分页与排序

```yaml
pagination:
  # 三列布局推荐保持 27，即 3 列 × 9 行。
  page-size: 27

sorting:
  admin-first: true
  admin-permission: "huhobotonlinelist.priority"
```

### 图片渲染

```yaml
render:
  columns: 3
  font-family: ""
  footer-text: "POWERED BY HuHoBot"
```

- `columns`：玩家卡片列数，允许 1 至 3，当前底图推荐使用 3。
- `font-family`：指定服务器系统中已安装的字体。留空时自动选择常见中文字体。
- `footer-text`：图片底部水印文字。

Linux 精简系统若没有中文字体，建议安装 Noto Sans CJK，并设置：

```yaml
render:
  font-family: "Noto Sans CJK SC"
```

### 玩家皮肤

```yaml
skin:
  enabled: true
  connect-timeout-ms: 3000
  read-timeout-ms: 5000
  cache-size: 200
  download-threads: 4
```

皮肤地址从玩家当前 `PlayerProfile` 或 `GameProfile` 获取。下载失败、离线模式没有皮肤或服务器无法访问皮肤地址时，图片仍会正常生成，只是改用占位头像。

### 图片发送限制

```yaml
image-api:
  max-bytes: 20971520
```

插件会在上传前检查 PNG 字节大小。超出限制时不会继续上传，并在服务器日志中记录原因。

### 消息文本

```yaml
messages:
  busy: "在线列表正在生成，请稍候再试。"
  failed: "在线列表生成或发送失败，请稍后再试。"
  page-usage: "用法：/{command} [页码]，页码必须是正整数。"
  page-out-of-range: "没有第 {page} 页，当前共 {pages} 页。"
```

支持的占位符：

- `{command}`：当前 QQ 命令名称。
- `{page}`：用户请求的页码。
- `{pages}`：当前总页数。

## 假玩家测试

### 本地图片预览

在服务器控制台或拥有 `huhobotonlinelist.admin` 权限的玩家执行：

```text
/huhobotonlinelist test 65 2
```

参数依次为：

```text
/huhobotonlinelist test <玩家数> [页码] [名称模板]
```

示例会生成 65 名带本地像素头像的假玩家，并输出第 2 页：

```text
plugins/HuHoBotOnlineList/test-preview-65-page-2.png
```

测试超长玩家名称：

```text
/huhobotonlinelist test 65 3 VeryLongMinecraftPlayerName_{index3}
```

名称模板支持 `{index}`、`{index2}` 和 `{index3}`，分别生成 `1`、`01`、`001` 形式的序号。

### 完整 QQ 发图测试

临时修改配置：

```yaml
test:
  fake-player-count: 65
  fake-player-name-template: "TestPlayer{index3}"
  fake-admin-count: 3
```

重启服务器后，在 QQ 群依次发送：

```text
/在线列表
/在线列表 2
/在线列表 3
/在线列表 4
```

前三条命令用于检查三页图片，第四条用于检查超出范围提示。测试完成后务必恢复：

```yaml
test:
  fake-player-count: 0
```

## HuHoBot 多分支适配

HuHoBot 附属插件开发入口可参考：[Spigot 附属插件开发教程](https://huhobot.txssb.cn/develop/spigot/)。

插件按以下顺序尝试接入 HuHoBot：

1. 使用 HuHoBot 原生 `BaseCommand` 注册 `/在线列表`。
2. 原生入口不可用时，使用扩展 API（`registerAddon` + `registerBotCommand`）注册命令，命令会自动出现在 `/帮助` 和 `/addons` 中。
3. 同时注册 `OnBotCommand` 事件监听，处理自定义逻辑（如图片生成）。

图片回复按以下顺序尝试：

1. HuHoBot 分支在群消息事件上公开的 `replyImage(byte[])`、`replyWithImage(...)` 等方法。
2. QQ SDK 的 `MessageChain.image(byte[])`。
3. QQ SDK 底层文件接口：先上传 `file_data`，再使用返回的 `file_info` 回复原消息。

公共图片接口为：

```text
cn.huohuas001.huhobot.onlinelist.api.ImageReplyApi
```

插件启动时会把该接口注册到 Bukkit `ServicesManager`。针对其他 HuHoBot 分支进行适配时，通常只需要扩展：

```text
src/main/java/cn/huohuas001/huhobot/onlinelist/image/ReflectiveImageReplyApi.java
```

`src/main/java/cn/huohuas001/bot/events/commands/` 中的类只是编译期 ABI 桩。Gradle 构建会把它们排除在最终 JAR 外，运行时始终使用宿主 HuHoBot 自己的类。

## 常见问题

### QQ 指令面板提示“超出数量限制”

这是 QQ 指令面板的数量限制，不代表命令处理器注册失败。命令没有出现在面板时，仍可手动发送 `/在线列表`。

### 图片中出现方框或乱码

服务器系统缺少中文字体。安装 Noto Sans CJK 后设置 `render.font-family`，再重启服务器。

### 玩家头像没有显示

检查服务器是否能访问玩家资料中的皮肤地址，也可以适当增加皮肤连接与读取超时。头像失败不会阻止在线列表生成。

### 插件回复“正在生成，请稍候再试”

同一 QQ 群已有图片生成任务正在运行，或请求仍处于 `cooldown-seconds` 冷却时间内。

## 数据与隐私

- QQ 群 OpenID 与消息 ID 只在内存中用于回复原消息和计算群冷却，不写入磁盘。
- 插件不保存在线玩家快照。
- 玩家皮肤头像仅缓存在服务器内存中，不写入磁盘。
- 测试预览只在管理员主动执行本地测试命令时写入插件数据目录。

## 构建

Windows：

```powershell
.\gradlew.bat clean check jar
```

Linux 或 macOS：

```bash
./gradlew clean check jar
```

构建完成后，插件 JAR 位于：

```text
build/libs/HuHoBotOnlineList-1.0.0.jar
```
## 许可证

本项目基于 [GNU Affero General Public License v3.0](LICENSE) 发布。

构建脚本会强制以 UTF-8 处理 `plugin.yml`，并将编译期 HuHoBot ABI 桩排除在最终 JAR 外。

## 当前范围

当前版本读取单个 Spigot/Paper 服务端的实时 Bukkit 玩家列表。跨服聚合、Velocity/BungeeCord 玩家合并和多个服务端统一分页不在当前版本范围内。
