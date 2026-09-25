# 头像与兼容架构

## 当前目标环境

`1.22.0` 只编译 Bukkit/Spigot API，不包含单独的 Paper 适配层：

- 单个 Bukkit/Spigot 系服务端；
- Java 8 字节码；
- 离线模式与正版模式；
- 可选 SkinsRestorer v15+；
- HuHoBot 主线 Addon API。

Paper 可作为 Spigot 兼容实现运行，但插件不编译或调用 Paper API、paperweight、NMS。

## 实际头像链路

```text
主线程快照阶段：
1. Bukkit PlayerProfile.getTextures().getSkin()
        ↓ URL 缺失
2. 底层 GameProfile textures 属性

皮肤线程池阶段：
3. 下载并裁剪快照中的皮肤 URL
        ↓ URL 缺失或下载/裁剪失败
4. SkinsRestorer PlayerStorage 已绑定皮肤
        ↓ 缺失且 allow-lookup=true
5. SkinsRestorer 登录时将应用的皮肤
        ↓ URL 缺失或下载/裁剪失败
6. 此 UUID 本次运行期间最后一次成功头像
        ↓ 不存在
7. UUID 磁盘头像缓存
        ↓ 不存在
8. JAR 内置 Minecraft Steve 正脸
```

Bukkit Profile 与底层 GameProfile 属于同一次主线程快照中的“公共入口优先、底层入口补缺”。SkinsRestorer 查询、HTTP 下载和裁剪均在皮肤线程池执行。

## Authlib 访问器兼容

`BukkitSkinResolver` 优先使用公共 Profile 接口。底层 GameProfile 后备同时识别：

```text
旧式：getProperties() / getValue()
新版：properties() / value()
```

此处仅使用运行时反射兼容不同服务端内部 Authlib 形态，不引入 Paper 编译依赖。textures 属性是 Base64 JSON；`SkinTextureUrl` 只提取 `textures.SKIN.url`，忽略 CAPE，并把 `http://textures.minecraft.net/` 规范化为 HTTPS。

## SkinsRestorer 适配

适配器面向 SkinsRestorer v15+：

```text
SkinsRestorerProvider.get()
  -> getPlayerStorage()
  -> getSkinOfPlayer(UUID)
  -> getSkinForPlayer(UUID, name, false)  # allow-lookup=true 时
```

实现使用反射，因此 SkinsRestorer 不是强制依赖，也不会被打包进 Addon JAR。可能访问存储或网络的 `getSkinForPlayer` 只允许在皮肤线程池执行。

## 缓存

`AvatarCache` 维护两个受 `skin.cache-size` 限制的内存 LRU：

- 皮肤 URL → 已裁剪头像；
- 玩家 UUID → 本次运行期间最后一次成功头像。

启用 `skin.persistent-cache` 后，最后一次成功的 128×128 头像还会按 UUID 哈希文件名保存到：

```text
plugins/HuHoBotOnlineList/cache/avatars/
```

服务器重启、Profile 暂时为空或下载队列拒绝新任务时，插件会尝试磁盘缓存。缓存文件名不直接使用玩家输入，避免路径穿越。

## 下载与裁剪

- 接受 64×32、64×64 及相同比例的高分辨率皮肤。
- 同时绘制 8×8 正脸与帽子层，最近邻缩放为 128×128 头像。
- 单张下载最大 4 MiB。
- 默认连接超时 3000 ms、读取超时 5000 ms。
- 裁剪结果全透明会被视为失败。
- 玩家卡片保留完整方形头像像素，不做圆角裁切，也不叠加在线状态圆点。

并发下载数由 `skin.download-threads` 控制。图片生成线程会等待当前页头像任务完成，但 Bukkit 主线程不会被阻塞。

## Steve 兜底

Steve 正脸使用代码内置的真实 8×8 像素数据，以最近邻算法缩放。它不请求网络，也不依赖资源包或外部图片服务。

```yaml
skin:
  fallback-avatar: "steve"
```

兼容选项 `initial` 会改用彩色首字母；其他无法识别的值按 Steve 处理。

## 诊断方式

```yaml
skin:
  debug: true
```

| 日志关键词 | 判断 |
|---|---|
| `Bukkit PlayerProfile 取得皮肤` | 公共 Profile 正常 |
| `底层 GameProfile 取得皮肤` | 公共入口为空，底层 textures 可用 |
| `SkinsRestorer 已从 stored 取得皮肤` | 玩家已有绑定皮肤 |
| `SkinsRestorer 已从 join lookup 取得皮肤` | 通过登录皮肤查询补全 |
| `皮肤下载/裁剪失败` | 检查 HTTP、尺寸或透明内容 |
| `沿用该玩家最后一次成功头像` | 命中内存缓存 |
| `沿用磁盘中的最后一次成功头像` | 命中 UUID 磁盘缓存 |
| `使用内置 Steve 头像` | 所有真实皮肤及缓存入口均失败 |

诊断结束后建议恢复 `debug: false`。

## 尚未覆盖

- SkinsRestorer v14 及更早版本；
- Floodgate/Geyser 基岩版皮肤 API；
- 下载失败负缓存；
- 所有 Bukkit/Spigot 系小版本的真实服务器测试矩阵。
