package cn.huohuas001.huhobot.onlinelist;

import cn.huohuas001.huhobot.onlinelist.api.ImageReplyApi;
import cn.huohuas001.huhobot.onlinelist.api.ImageReplyRequest;
import cn.huohuas001.huhobot.onlinelist.api.ImageReplyResult;
import cn.huohuas001.huhobot.onlinelist.bridge.BotCommandContext;
import cn.huohuas001.huhobot.onlinelist.bridge.BuiltInCommandBridge;
import cn.huohuas001.huhobot.onlinelist.bridge.HuHoBotBridge;
import cn.huohuas001.huhobot.onlinelist.image.ReflectiveImageReplyApi;
import cn.huohuas001.huhobot.onlinelist.model.OnlineListPages;
import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;
import cn.huohuas001.huhobot.onlinelist.render.OnlineListRenderer;
import cn.huohuas001.huhobot.onlinelist.skin.AvatarCache;
import cn.huohuas001.huhobot.onlinelist.skin.BukkitSkinResolver;
import cn.huohuas001.huhobot.onlinelist.testing.FakePlayerFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class OnlineListPlugin extends JavaPlugin implements CommandExecutor {
    private final BukkitSkinResolver skinResolver = new BukkitSkinResolver();
    private final Set<String> inFlightGroups = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Long> lastRequestAt = new ConcurrentHashMap<String, Long>();
    private ExecutorService renderExecutor;
    private ExecutorService skinExecutor;
    private HuHoBotBridge bridge;
    private BuiltInCommandBridge builtInBridge;
    private BukkitTask registrationRetryTask;
    private ImageReplyApi imageReplyApi;
    private OnlineListRenderer renderer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getCommand("huhobotonlinelist") != null) {
            getCommand("huhobotonlinelist").setExecutor(this);
        }

        String configuredCommand = getConfig().getString("bot-command", "在线列表");
        String commandKey = configuredCommand == null ? "" : configuredCommand.trim();
        if (commandKey.isEmpty()) {
            getLogger().severe("config.yml 的 bot-command 不能为空");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        renderExecutor = Executors.newSingleThreadExecutor(namedThreads("HuHoBotOnlineList-Render"));
        int downloadThreads = clamp(getConfig().getInt("skin.download-threads", 4), 1, 12);
        skinExecutor = Executors.newFixedThreadPool(downloadThreads, namedThreads("HuHoBotOnlineList-Skin"));

        try {
            builtInBridge = new BuiltInCommandBridge(this, this::acceptBotCommand);
            Plugin huHoBot = builtInBridge.getHuHoBotPlugin();
            if (huHoBot == null || !huHoBot.isEnabled()) {
                throw new IllegalStateException("未找到已启用的 HuHoBotPenguin");
            }
            AvatarCache avatars = new AvatarCache(
                getLogger(),
                skinExecutor,
                getConfig().getInt("skin.connect-timeout-ms", 3000),
                getConfig().getInt("skin.read-timeout-ms", 5000),
                getConfig().getInt("skin.cache-size", 200),
                getConfig().getBoolean("skin.enabled", true)
            );
            renderer = new OnlineListRenderer(
                avatars,
                getConfig().getInt("render.columns", 3),
                getConfig().getString("render.font-family", ""),
                getConfig().getString("render.footer-text", "POWERED BY HuHoBot")
            );
            imageReplyApi = new ReflectiveImageReplyApi(
                huHoBot,
                getLogger(),
                getConfig().getInt("image-api.max-bytes", 20 * 1024 * 1024)
            );
            Bukkit.getServicesManager().register(ImageReplyApi.class, imageReplyApi, this, ServicePriority.Normal);
            connectCommand(commandKey);
        } catch (Throwable error) {
            getLogger().log(Level.SEVERE, "HuHoBot 在线列表初始化失败", error);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("在线列表已就绪，主题：雾蓝玻璃，图片回复：JAR 内置字节 API");
    }

    @Override
    public void onDisable() {
        if (registrationRetryTask != null) registrationRetryTask.cancel();
        if (builtInBridge != null) builtInBridge.disconnect();
        if (bridge != null) bridge.disconnect();
        Bukkit.getServicesManager().unregisterAll(this);
        shutdown(renderExecutor);
        shutdown(skinExecutor);
        inFlightGroups.clear();
        lastRequestAt.clear();
    }

    private void connectCommand(String commandKey) throws ReflectiveOperationException {
        if ("在线列表".equals(commandKey)) {
            BuiltInCommandBridge.ConnectResult result = builtInBridge.tryConnect();
            if (result == BuiltInCommandBridge.ConnectResult.CONNECTED) {
                getLogger().info("已通过 HuHoBot 原生命令入口注册 /在线列表");
                return;
            }
            if (result == BuiltInCommandBridge.ConnectResult.NOT_READY) {
                registrationRetryTask = getServer().getScheduler().runTaskTimer(this, () -> {
                    BuiltInCommandBridge.ConnectResult retry = builtInBridge.tryConnect();
                    if (retry == BuiltInCommandBridge.ConnectResult.CONNECTED) {
                        getLogger().info("QQ 客户端启动完成，已注册 /在线列表");
                        registrationRetryTask.cancel();
                        registrationRetryTask = null;
                    } else if (retry == BuiltInCommandBridge.ConnectResult.UNSUPPORTED) {
                        registrationRetryTask.cancel();
                        registrationRetryTask = null;
                        try {
                            connectEventFallback(commandKey);
                        } catch (ReflectiveOperationException error) {
                            getLogger().log(Level.SEVERE, "HuHoBot 命令后备入口注册失败", error);
                            getServer().getPluginManager().disablePlugin(this);
                        }
                    }
                }, 20L, 20L);
                getLogger().info("等待 HuHoBot QQ 客户端启动，随后自动注册 /在线列表");
                return;
            }
        }
        connectEventFallback(commandKey);
    }

    private void connectEventFallback(String commandKey) throws ReflectiveOperationException {
        bridge = new HuHoBotBridge(
            this,
            commandKey,
            getConfig().getBoolean("push-command-menu", true),
            this::acceptBotCommand
        );
        bridge.connect();
        getLogger().warning("当前 HuHoBot 分支不支持原生命令注册，已启用 OnBotCommand 兼容入口");
    }

    private void acceptBotCommand(BotCommandContext context) {
        if (Bukkit.isPrimaryThread()) {
            handleBotCommand(context);
        } else {
            getServer().getScheduler().runTask(this, () -> handleBotCommand(context));
        }
    }

    private void handleBotCommand(BotCommandContext context) {
        int requestedPage = parseRequestedPage(context.getCommandArguments());
        if (requestedPage < 1) {
            context.replyText(message("messages.page-usage", "用法：/{command} [页码]，页码必须是正整数。")
                .replace("{command}", getConfig().getString("bot-command", "在线列表")));
            return;
        }

        ServerSnapshot complete = captureSnapshot();
        int pages = OnlineListPages.totalPages(complete.getOnlinePlayers(), pageSize());
        if (requestedPage > pages) {
            context.replyText(message("messages.page-out-of-range", "没有第 {page} 页，当前共 {pages} 页。")
                .replace("{page}", Integer.toString(requestedPage))
                .replace("{pages}", Integer.toString(pages)));
            return;
        }

        String group = context.getGroupOpenId();
        long now = System.currentTimeMillis();
        long cooldownMillis = Math.max(0, getConfig().getLong("cooldown-seconds", 3)) * 1000L;
        Long previous = lastRequestAt.put(group, now);
        if ((previous != null && now - previous < cooldownMillis) || !inFlightGroups.add(group)) {
            context.replyText(message("messages.busy", "在线列表正在生成，请稍候再试。"));
            return;
        }

        ServerSnapshot snapshot = OnlineListPages.page(complete, requestedPage, pageSize());
        // QQ 图片消息仍要求 content 字段；单个空格可保持消息合法，同时不显示图片尾随说明。
        String caption = " ";
        renderExecutor.execute(() -> {
            try {
                byte[] png = renderer.render(snapshot);
                ImageReplyResult result = imageReplyApi.reply(new ImageReplyRequest(
                    context.getSourceEvent(),
                    context.getGroupOpenId(),
                    context.getMessageId(),
                    context.getMessageSequence(),
                    caption,
                    "online-list.png",
                    png
                ));
                if (!result.isSuccess()) {
                    getLogger().log(
                        Level.WARNING,
                        "在线列表图片发送失败，transport=" + result.getTransport() + "：" + result.getMessage(),
                        result.getCause()
                    );
                    context.replyText(message("messages.failed", "在线列表生成或发送失败，请稍后再试。"));
                }
            } catch (Throwable error) {
                getLogger().log(Level.WARNING, "在线列表渲染失败", error);
                context.replyText(message("messages.failed", "在线列表生成或发送失败，请稍后再试。"));
            } finally {
                inFlightGroups.remove(group);
            }
        });
    }

    private ServerSnapshot captureSnapshot() {
        int fakePlayerCount = clamp(getConfig().getInt("test.fake-player-count", 0), 0, 300);
        if (fakePlayerCount > 0) {
            ServerSnapshot fake = FakePlayerFactory.create(
                serverName(),
                Bukkit.getMaxPlayers(),
                fakePlayerCount,
                Instant.now(),
                getConfig().getString("test.fake-player-name-template", "TestPlayer{index3}"),
                clamp(getConfig().getInt("test.fake-admin-count", 3), 0, fakePlayerCount)
            );
            return completeSnapshot(fake.getServerName(), fake.getMaxPlayers(), fake.getPlayers(), fake.getCapturedAt());
        }
        List<PlayerSnapshot> players = new ArrayList<PlayerSnapshot>();
        String adminPermission = getConfig().getString(
            "sorting.admin-permission",
            "huhobotonlinelist.priority"
        );
        String permission = adminPermission == null ? "" : adminPermission.trim();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean administrator = player.isOp() || (!permission.isEmpty() && player.hasPermission(permission));
            players.add(new PlayerSnapshot(
                player.getName(),
                player.getUniqueId().toString(),
                skinResolver.resolve(player),
                administrator
            ));
        }
        return completeSnapshot(serverName(), Bukkit.getMaxPlayers(), players, Instant.now());
    }

    private ServerSnapshot completeSnapshot(
        String name,
        int maxPlayers,
        List<PlayerSnapshot> players,
        Instant capturedAt
    ) {
        List<PlayerSnapshot> sorted = OnlineListPages.sorted(
            players,
            getConfig().getBoolean("sorting.admin-first", true)
        );
        return new ServerSnapshot(name, maxPlayers, sorted, capturedAt);
    }

    private String serverName() {
        String configured = getConfig().getString("server-name", "MinecraftServer");
        if (configured != null && !configured.trim().isEmpty()) return configured.trim();
        return "MinecraftServer";
    }

    private String message(String path, String fallback) {
        String value = getConfig().getString(path, fallback);
        return value == null ? fallback : value;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "bridge".equalsIgnoreCase(args[0])) return true;
        if (args.length > 0 && ("test".equalsIgnoreCase(args[0]) || "preview".equalsIgnoreCase(args[0]))) {
            return generateTestPreview(sender, args);
        }
        sender.sendMessage("HuHoBotOnlineList " + getDescription().getVersion() + " 已启用；QQ 命令 /"
            + getConfig().getString("bot-command", "在线列表")
            + " [页码]；本地测试：/" + label + " test <虚假玩家数> [页码] [名称模板]");
        return true;
    }

    private boolean generateTestPreview(CommandSender sender, String[] args) {
        if (!sender.hasPermission("huhobotonlinelist.admin")) {
            sender.sendMessage("你没有权限生成测试预览。");
            return true;
        }
        int count = 65;
        if (args.length > 1) {
            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage("虚假玩家数量必须是 0 到 300 之间的整数。");
                return true;
            }
        }
        if (count < 0 || count > 300) {
            sender.sendMessage("虚假玩家数量必须是 0 到 300 之间的整数。");
            return true;
        }

        int requestedPage = 1;
        int templateStart = 2;
        if (args.length > 2) {
            try {
                requestedPage = Integer.parseInt(args[2]);
                templateStart = 3;
            } catch (NumberFormatException ignored) {
                // 兼容旧用法：第三个参数不是整数时，直接把它视为名称模板。
            }
        }
        if (requestedPage < 1) {
            sender.sendMessage("页码必须是正整数。");
            return true;
        }

        final int requestedCount = count;
        final int previewPage = requestedPage;
        String configuredNameTemplate = getConfig().getString("test.fake-player-name-template", "TestPlayer{index3}");
        final String nameTemplate = args.length > templateStart
            ? joinArguments(args, templateStart)
            : (configuredNameTemplate == null ? "TestPlayer{index3}" : configuredNameTemplate);
        ServerSnapshot fake = FakePlayerFactory.create(
            serverName(),
            Math.max(Bukkit.getMaxPlayers(), requestedCount),
            requestedCount,
            Instant.now(),
            nameTemplate,
            clamp(getConfig().getInt("test.fake-admin-count", 3), 0, requestedCount)
        );
        ServerSnapshot complete = completeSnapshot(
            fake.getServerName(),
            fake.getMaxPlayers(),
            fake.getPlayers(),
            fake.getCapturedAt()
        );
        int pages = OnlineListPages.totalPages(complete.getOnlinePlayers(), pageSize());
        if (previewPage > pages) {
            sender.sendMessage("没有第 " + previewPage + " 页，当前共 " + pages + " 页。");
            return true;
        }
        ServerSnapshot snapshot = OnlineListPages.page(complete, previewPage, pageSize());
        sender.sendMessage("正在生成 " + requestedCount + " 名虚假玩家的第 " + previewPage + " / " + pages
            + " 页三列预览，名称模板：" + nameTemplate);
        renderExecutor.execute(() -> {
            try {
                byte[] png = renderer.render(snapshot);
                Path output = getDataFolder().toPath().resolve(
                    "test-preview-" + requestedCount + "-page-" + previewPage + ".png"
                );
                Files.createDirectories(output.getParent());
                Files.write(output, png);
                sendOnMain(sender, "测试预览已生成：" + output.toAbsolutePath());
            } catch (Throwable error) {
                getLogger().log(Level.WARNING, "生成虚假玩家测试预览失败", error);
                sendOnMain(sender, "测试预览生成失败，请查看控制台日志。");
            }
        });
        return true;
    }

    private void sendOnMain(CommandSender sender, String message) {
        getServer().getScheduler().runTask(this, () -> sender.sendMessage(message));
    }

    private static String joinArguments(String[] args, int start) {
        StringBuilder result = new StringBuilder();
        for (int index = start; index < args.length; index++) {
            if (result.length() > 0) result.append(' ');
            result.append(args[index]);
        }
        return result.toString();
    }

    private int pageSize() {
        return clamp(getConfig().getInt("pagination.page-size", 27), 1, 300);
    }

    private static int parseRequestedPage(String arguments) {
        String value = arguments == null ? "" : arguments.trim();
        if (value.isEmpty()) return 1;
        if (value.matches(".*\\s+.*")) return -1;
        try {
            int page = Integer.parseInt(value);
            return page > 0 ? page : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ThreadFactory namedThreads(String prefix) {
        AtomicInteger index = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static void shutdown(ExecutorService executor) {
        if (executor != null) executor.shutdownNow();
    }
}
