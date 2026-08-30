package cn.huohuas001.huhobot.onlinelist.bridge;

import cn.huohuas001.bot.events.commands.BaseCommand;
import cn.huohuas001.bot.events.commands.Commands;

import java.util.function.Consumer;

/**
 * HuHoBot 原生命令处理器。使用原生入口可避免自定义命令桥额外回复“已发送执行请求”。
 */
public final class BuiltInOnlineListCommand extends BaseCommand {
    private final Consumer<Object> handler;

    public BuiltInOnlineListCommand(Consumer<Object> handler) {
        this.handler = handler;
    }

    @Commands(command = "在线列表", describe = "生成服务器在线玩家列表，可选页码")
    public void onlineList(Object groupMessageEvent) {
        handler.accept(groupMessageEvent);
    }
}
