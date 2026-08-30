package cn.huohuas001.huhobot.onlinelist.bridge;

import cn.huohuas001.huhobot.onlinelist.util.Reflect;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

/**
 * 通过监听 OnBotCommand 事件接收命令，并使用新规范 API（registerAddon + registerBotCommand）注册。
 * 不再使用 BaseCommand 子类，避免命令同时出现在内置和扩展分类中。
 */
public final class BuiltInCommandBridge {
    public enum ConnectResult { CONNECTED, NOT_READY, UNSUPPORTED }

    private final JavaPlugin owner;
    private final Consumer<BotCommandContext> handler;
    private final Plugin huHoBot;
    private Listener listener;
    private boolean commandRegistered;

    public BuiltInCommandBridge(JavaPlugin owner, Consumer<BotCommandContext> handler) {
        this.owner = owner;
        this.handler = handler;
        this.huHoBot = locatePlugin(owner.getServer().getPluginManager());
    }

    public ConnectResult tryConnect() {
        if (commandRegistered) return ConnectResult.CONNECTED;
        if (huHoBot == null || !huHoBot.isEnabled()) return ConnectResult.UNSUPPORTED;

        try {
            // 注册事件监听
            Class<? extends Event> eventClass = findEventClass(huHoBot.getClass().getClassLoader());
            listener = new Listener() { };
            EventExecutor executor = (ignored, event) -> onEvent(event);
            owner.getServer().getPluginManager().registerEvent(
                eventClass, listener, EventPriority.NORMAL, executor, owner, false);

            // 注册扩展 + 命令
            String addonName = owner.getName();
            String version = owner.getDescription().getVersion();
            String description = owner.getDescription().getDescription();
            if (description == null) description = "";
            String author = owner.getDescription().getAuthors().isEmpty()
                ? "" : owner.getDescription().getAuthors().get(0);

            Reflect.invoke(huHoBot, "registerAddon", addonName, version, description, author);

            String commandKey = owner.getConfig().getString("bot-command", "在线列表");
            if (commandKey == null || commandKey.trim().isEmpty()) commandKey = "在线列表";
            boolean pushMenu = owner.getConfig().getBoolean("push-command-menu", true);

            Object result = Reflect.invoke(huHoBot, "registerBotCommand",
                addonName, commandKey, "查看服务器在线列表", 0, pushMenu);

            if (result instanceof Boolean && (Boolean) result) {
                commandRegistered = true;
                return ConnectResult.CONNECTED;
            }
            return ConnectResult.UNSUPPORTED;
        } catch (Throwable error) {
            if (isNotReady(error)) return ConnectResult.NOT_READY;
            owner.getLogger().fine("HuHoBot 命令注册失败：" + concise(error));
            return ConnectResult.UNSUPPORTED;
        }
    }

    public void disconnect() {
        // 注销事件监听（无论 commandRegistered 状态）
        if (listener != null) {
            try {
                for (org.bukkit.event.HandlerList list : org.bukkit.event.HandlerList.getHandlerLists()) {
                    list.unregister(listener);
                }
            } catch (Throwable ignored) {
            }
            listener = null;
        }
        // 注销命令
        if (commandRegistered) {
            try {
                String commandKey = owner.getConfig().getString("bot-command", "在线列表");
                if (commandKey != null) {
                    Reflect.invoke(huHoBot, "unregisterBotCommand", commandKey);
                }
            } catch (Throwable ignored) {
            }
            commandRegistered = false;
        }
    }

    public Plugin getHuHoBotPlugin() { return huHoBot; }

    private void onEvent(Event event) {
        try {
            BotCommandContext context = BotCommandContext.from(event);
            String commandKey = owner.getConfig().getString("bot-command", "在线列表");
            if (commandKey == null) commandKey = "在线列表";
            if (!commandKey.equals(context.getCommandKey())) return;
            context.cancel();
            handler.accept(context);
        } catch (Throwable error) {
            owner.getLogger().warning("处理 HuHoBot 命令事件失败：" + error.getMessage());
        }
    }

    private static boolean isNotReady(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && (message.contains("not been launched") || message.contains("not initialized")))
                return true;
            cursor = cursor.getCause();
        }
        return false;
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
        String[] eventClasses = {
            "cn.huohuas001.huhobotPenguin.spigot.events.OnBotCommand"
        };
        ClassNotFoundException last = null;
        for (String name : eventClasses) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                if (Event.class.isAssignableFrom(type)) return (Class<? extends Event>) type;
            } catch (ClassNotFoundException error) { last = error; }
        }
        throw last == null ? new ClassNotFoundException("HuHoBot OnBotCommand") : last;
    }

    private static String concise(Throwable error) {
        Throwable cursor = error;
        while (cursor.getCause() != null && cursor.getCause() != cursor) cursor = cursor.getCause();
        return cursor.getClass().getSimpleName() + (cursor.getMessage() == null ? "" : ": " + cursor.getMessage());
    }
}
