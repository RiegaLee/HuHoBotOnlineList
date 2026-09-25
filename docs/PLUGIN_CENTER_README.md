# HuHoBotOnlineList

在 QQ 群发送 `/在线列表`，即可查看 Minecraft 服务器的在线人数和玩家列表。图片会显示玩家名称、头像和服务器名称。

## 功能

- 显示在线人数和玩家头像；
- 支持分页，每页最多显示 27 名玩家；
- 可将管理员排在列表前面；
- 支持 SkinsRestorer；
- 可更换图片底图，调整卡片透明度。

## 运行环境

- Minecraft 1.16.5 或更高版本的 Bukkit、Spigot 兼容服务端；
- Java 8 或更高版本；
- HuHoBotPenguin；
- SkinsRestorer v15+（可选）。

## 安装

1. 安装并配置 HuHoBotPenguin。
2. 将 `HuHoBot-OnlineList-1.22.0.jar` 放入服务器的 `plugins/` 目录。
3. 启动服务器，在 QQ 群发送 `/在线列表`。

查看指定页可发送 `/在线列表 2`。

## 自定义底图

将 PNG 图片命名为 `online-list.png`，放入：

```text
plugins/HuHoBotOnlineList/assets/custom/backgrounds/
```

重启服务器后生效。图片会保持原始比例并居中裁切。

卡片透明度可在 `plugins/HuHoBotOnlineList/config.yml` 中调整：

```yaml
render:
  custom-background:
    surface-opacity: 0.12
```

数值越小，底图越清晰。

## 常用设置

在 `plugins/HuHoBotOnlineList/config.yml` 中可以修改服务器名称、查询间隔和每页人数：

```yaml
server-name: "MinecraftServer"
cooldown-seconds: 3
pagination:
  page-size: 27
```

修改后重启服务器即可生效。

## 问题反馈

使用中遇到问题，可在QQ群联系开发者，并附上服务端版本和相关报错。
