package cn.huohuas001.huhobot.onlinelist.bridge;

import cn.huohuas001.huhobot.onlinelist.util.Reflect;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * 后备入口：监听 HuHoBot OnBotCommand 事件，并通过新规范 API 注册扩展命令。
 */
public final class HuHoBotBridge {
    private static final String[] EVENT_CLASSES = {
        "cn.huohuas001.huhobotPenguin.spigot.events.OnBotCommand"
    };

    private final JavaPlugin owner;
    private final String commandKey;
    private final boolean pushMenu;
    private final Consumer<BotCommandContext> handler;
    private Plugin huHoBot;
    private Listener listener;
    private boolean commandRegistered;
    private boolean newApiUsed;

    public HuHoBotBridge(JavaPlugin owner, String commandKey, boolean pushMenu, Consumer<BotCommandContext> handler) {
        this.owner = owner;
        this.commandKey = commandKey;
        this.pushMenu = pushMenu;
        this.handler = handler;
    }

    public void connect() throws ReflectiveOperationException {
        PluginManager manager = owner.getServer().getPluginManager();
        huHoBot = locatePlugin(manager);
        if (huHoBot == null || !huHoBot.isEnabled())
            throw new IllegalStateException("未找到已启用的 HuHoBotPenguin");

        // 注册事件监听
        Class<? extends Event> eventClass = findEventClass(huHoBot.getClass().getClassLoader());
        listener = new Listener() { };
        EventExecutor executor = (ignored, event) -> onEvent(event);
        manager.registerEvent(eventClass, listener, EventPriority.NORMAL, executor, owner, false);

        // 尝试新规范：registerAddon + registerBotCommand(addonName, ...)
        if (tryNewApi()) {
            commandRegistered = true;
            newApiUsed = true;
            return;
        }

        // 回退旧规范：仅 registerBotCommand(key, ...)
        if (tryOldApi()) {
            commandRegistered = true;
            newApiUsed = false;
            return;
        }

        throw new IllegalStateException("HuHoBot 拒绝注册命令：" + commandKey);
    }

    public void disconnect() {
        if (huHoBot != null && commandRegistered) {
            try {
                if (newApiUsed) {
                    Reflect.invoke(huHoBot, "unregisterBotCommand", owner.getName());
                }
            } catch (Throwable error) {
                owner.getLogger().warning("注销 HuHoBot 命令失败：" + error.getMessage());
            }
        }
        commandRegistered = false;
    }

    public Plugin getHuHoBotPlugin() { return huHoBot; }

    private boolean tryNewApi() {
        try {
            String addonName = owner.getName();
            String version = owner.getDescription().getVersion();
            String description = owner.getDescription().getDescription();
            if (description == null) description = "";
            String author = owner.getDescription().getAuthors().isEmpty()
                ? "" : owner.getDescription().getAuthors().get(0);

            // registerAddon(name, version, description, author)
            Reflect.invoke(huHoBot, "registerAddon", addonName, version, description, author);

            // registerBotCommand(addonName, key, command, permission, pushMenu)
            Object result = Reflect.invoke(huHoBot, "registerBotCommand",
                addonName, commandKey, "查看服务器在线列表", 0, pushMenu);
            return result instanceof Boolean && (Boolean) result;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean tryOldApi() {
        try {
            Object result = Reflect.invoke(huHoBot, "registerBotCommand",
                commandKey, "查看服务器在线列表", 0, pushMenu);
            return result instanceof Boolean && (Boolean) result;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    private void onEvent(Event event) {
        try {
            BotCommandContext context = BotCommandContext.from(event);
            if (!commandKey.equals(context.getCommandKey())) return;
            context.cancel();
            handler.accept(context);
        } catch (Throwable error) {
            owner.getLogger().severe("处理 HuHoBot 命令事件失败：" + error.getMessage());
        }
    }

    private static Plugin locatePlugin(PluginManager manager) {
        Plugin named = manager.getPlugin("HuHoBotPenguin");
        if (named != null) return named;
        for (Plugin plugin : manager.getPlugins()) {
            if (plugin.getClass().getName().contains("HuHoBot")) return plugin;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Event> findEventClass(ClassLoader loader) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String name : EVENT_CLASSES) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                if (Event.class.isAssignableFrom(type)) return (Class<? extends Event>) type;
            } catch (ClassNotFoundException error) { last = error; }
        }
        throw last == null ? new ClassNotFoundException("HuHoBot OnBotCommand") : last;
    }
}
