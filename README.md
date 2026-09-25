# HuHoBotOnlineList

HuHoBotPenguin 的 Minecraft 服务器在线列表附属插件。在 QQ 群发送 `/在线列表`，即可查看在线人数、玩家名称和头像。

## 功能

- 每页最多显示 27 名玩家，支持 `/在线列表 2` 翻页；
- 支持管理员置顶和 SkinsRestorer；
- 支持自定义 PNG 底图和卡片透明度；
- 玩家头像获取失败时使用内置 Minecraft Steve 头像。

## 运行环境

- Minecraft 1.16.5 或更高版本的 Bukkit、Spigot 兼容服务端；
- Java 8 或更高版本；
- HuHoBotPenguin；
- SkinsRestorer v15+（可选）。

## 安装

1. 安装并配置 HuHoBotPenguin。
2. 从[插件中心](https://addon.txssb.cn/plugin?id=plg_6ab355bbed9212.93066841)下载 `HuHoBot-OnlineList-1.22.0.jar`，放入服务器的 `plugins/` 目录。
3. 启动服务器，在 QQ 群发送 `/在线列表`。

## 自定义底图

将 PNG 图片命名为 `online-list.png`，放入：

```text
plugins/HuHoBotOnlineList/assets/custom/backgrounds/
```

重启服务器后生效。图片会保持比例并居中裁切。

服务器名称、查询间隔和每页人数可在 `plugins/HuHoBotOnlineList/config.yml` 中调整。完整的服主说明见[插件中心 README](docs/PLUGIN_CENTER_README.md)。

## 源码构建

构建需要 JDK 21 和当前 HuHoBot 主线的 Bot JAR。默认从相邻目录读取：

```text
../PenguinClient-Main/common/Bot/build/libs/common-Bot-1.5.0.jar
```

在项目目录执行 `./gradlew build`（Windows 使用 `.\gradlew.bat build`）。构建产物位于 `build/libs/HuHoBot-OnlineList-1.22.0.jar`，目标字节码为 Java 8。其他构建和验证说明见[构建文档](docs/BUILD_AND_DEPLOY.md)。

## 许可

项目代码遵循 [AGPL-3.0](LICENSE)。标题字体子集遵循 SIL Open Font License 1.1，说明见[字体许可](src/main/resources/fonts/NOTICE.txt)。
