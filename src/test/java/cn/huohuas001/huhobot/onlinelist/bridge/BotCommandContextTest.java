package cn.huohuas001.huhobot.onlinelist.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotCommandContextTest {
    @Test
    void extractsPageFromNativeRawMessageWithoutRequiringTwoParameterCommands() throws Exception {
        RawEvent event = new RawEvent("/在线列表 3", "group-1", "message-1");

        BotCommandContext context = BotCommandContext.fromRawEvent(event);

        assertEquals("3", context.getCommandArguments());
        assertEquals("group-1", context.getGroupOpenId());
        assertEquals("message-1", context.getMessageId());
    }

    @Test
    void removesBotMentionBeforeExtractingPage() throws Exception {
        RawEvent event = new RawEvent("<@!bot-id> /在线列表 12", "group-1", "message-1");

        assertEquals("12", BotCommandContext.fromRawEvent(event).getCommandArguments());
    }

    private static final class RawEvent {
        private final RawMessage rawMessage;
        private final String groupOpenId;
        private final int msgSeq = 0;

        private RawEvent(String content, String groupOpenId, String messageId) {
            this.rawMessage = new RawMessage(content, messageId);
            this.groupOpenId = groupOpenId;
        }
    }

    private static final class RawMessage {
        private final String content;
        private final String id;

        private RawMessage(String content, String id) {
            this.content = content;
            this.id = id;
        }
    }
}
