package cn.huohuas001.huhobot.onlinelist.testing;

import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakePlayerFactoryTest {
    @Test
    void createsLocalPlayersAndClampsTheMaximum() {
        ServerSnapshot snapshot = FakePlayerFactory.create("Test", 20, 400, Instant.EPOCH);

        assertEquals(300, snapshot.getOnlinePlayers());
        assertEquals(300, snapshot.getMaxPlayers());
        assertTrue(snapshot.getServerName().contains("测试名单"));
        assertEquals("TestPlayer001", snapshot.getPlayers().get(0).getName());
        assertNotNull(snapshot.getPlayers().get(0).getAvatarOverride());
    }

    @Test
    void supportsLongCustomNameTemplates() {
        ServerSnapshot snapshot = FakePlayerFactory.create(
            "Test",
            20,
            2,
            Instant.EPOCH,
            "VeryLongMinecraftPlayerName_{index3}"
        );

        assertEquals("VeryLongMinecraftPlayerName_001", snapshot.getPlayers().get(0).getName());
        assertEquals("VeryLongMinecraftPlayerName_002", snapshot.getPlayers().get(1).getName());
    }

    @Test
    void canMarkFakeAdministratorsForPriorityTests() {
        ServerSnapshot snapshot = FakePlayerFactory.create(
            "Test",
            20,
            5,
            Instant.EPOCH,
            "Player{index2}",
            2
        );

        assertTrue(snapshot.getPlayers().get(0).isAdministrator());
        assertTrue(snapshot.getPlayers().get(1).isAdministrator());
        assertEquals(false, snapshot.getPlayers().get(2).isAdministrator());
    }
}
