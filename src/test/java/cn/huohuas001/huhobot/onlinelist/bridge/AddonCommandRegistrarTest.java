package cn.huohuas001.huhobot.onlinelist.bridge;

import cn.huohuas001.bot.addon.Addon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AddonCommandRegistrarTest {
    @Test
    void registersNativeCommandWithAddonMetadata() throws Exception {
        FakeAddonQClient qClient = new FakeAddonQClient();
        Object command = new Object();

        boolean registered = AddonCommandRegistrar.tryRegister(
            getClass().getClassLoader(),
            qClient,
            command,
            "HuHoBotOnlineList",
            "1.0.0",
            "在线列表图片",
            "RiegaLee, Shabby-666"
        );

        assertTrue(registered);
        assertEquals("HuHoBotOnlineList", qClient.addon.getName());
        assertEquals("1.0.0", qClient.addon.getVersion());
        assertEquals("在线列表图片", qClient.addon.getDescription());
        assertEquals("RiegaLee, Shabby-666", qClient.addon.getAuthor());
        assertSame(command, qClient.command);
    }

    @Test
    void leavesLegacyQClientForTheExistingFallback() throws Exception {
        boolean registered = AddonCommandRegistrar.tryRegister(
            getClass().getClassLoader(),
            new LegacyQClient(),
            new Object(),
            "HuHoBotOnlineList",
            "1.0.0",
            "在线列表图片",
            "RiegaLee"
        );

        assertFalse(registered);
    }

    private static final class FakeAddonQClient {
        private Addon addon;
        private Object command;

        public void registerCommand(Addon addon, Object command) {
            this.addon = addon;
            this.command = command;
        }
    }

    private static final class LegacyQClient {
        public void registerCommand(Object command) {
        }
    }
}
