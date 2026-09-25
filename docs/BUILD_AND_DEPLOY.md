# 构建与部署

## 构建

使用工程自带的 Gradle Wrapper。建议用 JDK 21 运行 Gradle；插件编译为 Java 8 字节码，服务端接口基线为 Spigot API 1.16.5。

构建需要当前 HuHoBot 主线的 Bot JAR。默认路径：

```text
../PenguinClient-Main/common/Bot/build/libs/common-Bot-1.5.0.jar
```

如果 JAR 位于其他位置，使用 `-PhuhobotQqSdkJar=...` 或环境变量 `HUHOBOT_QQ_SDK_JAR` 指定。

Windows：

```powershell
.\gradlew.bat clean build
```

Linux 或 macOS：

```sh
./gradlew clean build
```

构建会运行测试，并检查最终 JAR 不含 HuHoBot、QQ SDK、Bukkit、Paper 或 NMS 的依赖类。产物位于：

```text
build/libs/HuHoBot-OnlineList-1.22.0.jar
```

## 部署

安装并配置 HuHoBotPenguin，将 JAR 放入服务端 `plugins/` 目录，启动服务器后在 QQ 群发送 `/在线列表`。自定义底图和配置方法见[服主说明](PLUGIN_CENTER_README.md)。
