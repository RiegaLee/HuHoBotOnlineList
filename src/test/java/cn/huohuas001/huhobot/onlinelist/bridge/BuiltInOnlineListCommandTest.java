package cn.huohuas001.huhobot.onlinelist.bridge;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BuiltInOnlineListCommandTest {
    @Test
    void exposesTheCurrentHuHoBotAddonCommandAbi() {
        AtomicReference<Object> receivedEvent = new AtomicReference<Object>();
        AtomicReference<String> receivedArguments = new AtomicReference<String>();
        BuiltInOnlineListCommand command = new BuiltInOnlineListCommand((event, arguments) -> {
            receivedEvent.set(event);
            receivedArguments.set(arguments);
        });

        Object event = new Object();
        command.onlineList(event, " 2 ");

        assertEquals(1, command.registeredCommands().size());
        assertEquals("在线列表", command.registeredCommands().get(0).getCommand());
        assertSame(event, receivedEvent.get());
        assertEquals(" 2 ", receivedArguments.get());
    }
}
