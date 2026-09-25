package cn.huohuas001.huhobot.onlinelist.bridge;

import cn.huohuas001.huhobot.onlinelist.util.Reflect;

/** 从 HuHoBot 原生群消息对象提取出的稳定命令上下文。 */
public final class BotCommandContext {
    private final Object sourceEvent;
    private final String commandArguments;
    private final String groupOpenId;
    private final String messageId;
    private final int messageSequence;

    private BotCommandContext(
        Object sourceEvent,
        String commandArguments,
        String groupOpenId,
        String messageId,
        int messageSequence
    ) {
        this.sourceEvent = sourceEvent;
        this.commandArguments = commandArguments;
        this.groupOpenId = groupOpenId;
        this.messageId = messageId;
        this.messageSequence = messageSequence;
    }

    public static BotCommandContext fromRawEvent(Object event) throws ReflectiveOperationException {
        Object rawMessage = Reflect.read(event, "rawMessage");
        return fromRawEvent(event, extractNativeArguments(rawMessage), rawMessage);
    }

    public static BotCommandContext fromRawEvent(Object event, String commandArguments) throws ReflectiveOperationException {
        Object rawMessage = Reflect.read(event, "rawMessage");
        return fromRawEvent(event, commandArguments, rawMessage);
    }

    private static BotCommandContext fromRawEvent(
        Object event,
        String commandArguments,
        Object rawMessage
    ) throws ReflectiveOperationException {
        String groupOpenId = optionalText(event, "groupOpenId");
        if (groupOpenId.isEmpty()) groupOpenId = optionalText(event, "groupId");
        int sequence = 0;
        try {
            sequence = number(Reflect.read(event, "msgSeq"));
        } catch (ReflectiveOperationException ignored) {
            // MessageChain can obtain its own sequence; only the low-level fallback uses this value.
        }
        return new BotCommandContext(
            event,
            commandArguments == null ? "" : commandArguments.trim(),
            groupOpenId,
            text(Reflect.read(rawMessage, "id")),
            sequence
        );
    }

    public boolean replyText(String text) {
        try {
            Object value = Reflect.invoke(sourceEvent, "replyText", text);
            return !(value instanceof Boolean) || (Boolean) value;
        } catch (ReflectiveOperationException first) {
            try {
                Object value = Reflect.invoke(sourceEvent, "reply", text);
                return !(value instanceof Boolean) || (Boolean) value;
            } catch (ReflectiveOperationException second) {
                try {
                    Object value = Reflect.invoke(sourceEvent, "sendMessage", text);
                    return value != null;
                } catch (ReflectiveOperationException third) {
                    return false;
                }
            }
        }
    }

    public Object getSourceEvent() {
        return sourceEvent;
    }

    public String getCommandArguments() {
        return commandArguments;
    }

    public String getGroupOpenId() {
        return groupOpenId;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getMessageSequence() {
        return messageSequence;
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static String optionalText(Object target, String name) {
        try {
            return text(Reflect.read(target, name));
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static String extractNativeArguments(Object rawMessage) {
        String content = optionalText(rawMessage, "content")
            .replaceAll("<@!?[^>]+>", "")
            .trim();
        if (content.startsWith("/")) content = content.substring(1).trim();
        String command = "在线列表";
        if (!content.startsWith(command)) return "";
        return content.substring(command.length()).trim();
    }
}
