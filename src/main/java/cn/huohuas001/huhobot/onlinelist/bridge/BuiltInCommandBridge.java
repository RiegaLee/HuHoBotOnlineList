package cn.huohuas001.huhobot.onlinelist.bridge;

import cn.huohuas001.huhobot.onlinelist.util.Reflect;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;

/** 将附属 JAR 中的 BaseCommand 处理器注册到当前 HuHoBot QClient。 */
public final class BuiltInCommandBridge {
    public enum ConnectResult {
        CONNECTED,
        NOT_READY,
        UNSUPPORTED
    }

    private final JavaPlugin owner;
    private final Consumer<BotCommandContext> handler;
    private final Plugin huHoBot;
    private Object commandInstance;
    private Class<?> qClientClass;

    public BuiltInCommandBridge(JavaPlugin owner, Consumer<BotCommandContext> handler) {
        this.owner = owner;
        this.handler = handler;
        this.huHoBot = locatePlugin(owner.getServer().getPluginManager());
    }

    public ConnectResult tryConnect() {
        if (commandInstance != null) return ConnectResult.CONNECTED;
        if (huHoBot == null || !huHoBot.isEnabled()) return ConnectResult.UNSUPPORTED;
        try {
            ClassLoader loader = huHoBot.getClass().getClassLoader();
            qClientClass = Class.forName("cn.huohuas001.bot.QClient", false, loader);
            Object qClient = Reflect.kotlinObject(qClientClass);
            Object candidate = new BuiltInOnlineListCommand(this::onRawEvent);
            Reflect.invoke(qClient, "registerCommand", candidate);
            commandInstance = candidate;
            return ConnectResult.CONNECTED;
        } catch (Throwable error) {
            if (isNotReady(error)) return ConnectResult.NOT_READY;
            owner.getLogger().fine("HuHoBot 原生命令入口不可用：" + concise(error));
            return ConnectResult.UNSUPPORTED;
        }
    }

    public void disconnect() {
        if (qClientClass == null || commandInstance == null) return;
        try {
            Object handlerObject = readKotlinField(qClientClass, "groupMessageHandler");
            Object commands = Reflect.read(handlerObject, "commands");
            if (commands instanceof List) ((List<?>) commands).remove(commandInstance);
            Object qClient = Reflect.kotlinObject(qClientClass);
            Reflect.invoke(qClient, "syncGroupPanels");
        } catch (Throwable error) {
            owner.getLogger().warning("注销 HuHoBot 原生命令失败：" + concise(error));
        } finally {
            commandInstance = null;
        }
    }

    public Plugin getHuHoBotPlugin() {
        return huHoBot;
    }

    public String readServerName() {
        if (huHoBot == null) return null;
        try {
            Object value = Reflect.invoke(huHoBot, "getServerName");
            String text = value == null ? "" : value.toString().trim();
            return text.isEmpty() ? null : text;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void onRawEvent(Object event) {
        try {
            handler.accept(BotCommandContext.fromRawEvent(event));
        } catch (Throwable error) {
            owner.getLogger().warning("读取 HuHoBot 原生群消息失败：" + concise(error));
        }
    }

    private static Object readKotlinField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = Reflect.findField(type, name);
        if (field == null) throw new NoSuchFieldException(type.getName() + "." + name);
        field.setAccessible(true);
        return field.get(Modifier.isStatic(field.getModifiers()) ? null : Reflect.kotlinObject(type));
    }

    private static boolean isNotReady(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && (message.contains("not been launched") || message.contains("not initialized"))) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static Plugin locatePlugin(PluginManager manager) {
        Plugin named = manager.getPlugin("HuHoBotPenguin");
        if (named != null) return named;
        for (Plugin plugin : manager.getPlugins()) {
            if (plugin.getClass().getName().contains("HuHoBot")) return plugin;
            Method method = Reflect.findCompatibleMethod(plugin.getClass(), "registerBotCommand", "a", "b");
            if (method != null) return plugin;
        }
        return null;
    }

    private static String concise(Throwable error) {
        Throwable cursor = error;
        while (cursor.getCause() != null && cursor.getCause() != cursor) cursor = cursor.getCause();
        return cursor.getClass().getSimpleName() + (cursor.getMessage() == null ? "" : ": " + cursor.getMessage());
    }
}
