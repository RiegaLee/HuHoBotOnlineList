package cn.huohuas001.huhobot.onlinelist.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineListPagesTest {
    @Test
    void putsAdministratorsFirstAndSortsEachGroupByName() {
        List<PlayerSnapshot> sorted = OnlineListPages.sorted(Arrays.asList(
            player("Zoe", false),
            player("charlie", true),
            player("Alice", false),
            player("bob", true)
        ), true);

        assertEquals("bob", sorted.get(0).getName());
        assertEquals("charlie", sorted.get(1).getName());
        assertEquals("Alice", sorted.get(2).getName());
        assertEquals("Zoe", sorted.get(3).getName());
        assertTrue(sorted.get(0).isAdministrator());
    }

    @Test
    void slicesTwentySevenPlayersPerPageAndPreservesTotalOnline() {
        PlayerSnapshot[] players = new PlayerSnapshot[65];
        for (int index = 0; index < players.length; index++) {
            players[index] = player(String.format("Player%03d", index + 1), false);
        }
        ServerSnapshot complete = new ServerSnapshot("Test", 100, Arrays.asList(players), Instant.EPOCH);

        ServerSnapshot page = OnlineListPages.page(complete, 3, 27);

        assertEquals(65, page.getOnlinePlayers());
        assertEquals(11, page.getPlayers().size());
        assertEquals(3, page.getCurrentPage());
        assertEquals(3, page.getTotalPages());
        assertEquals("Player055", page.getPlayers().get(0).getName());
    }

    @Test
    void emptyListStillHasOnePageAndRejectsOutOfRangePages() {
        ServerSnapshot complete = new ServerSnapshot(
            "Test",
            100,
            java.util.Collections.<PlayerSnapshot>emptyList(),
            Instant.EPOCH
        );

        assertEquals(1, OnlineListPages.totalPages(0, 27));
        assertEquals(0, OnlineListPages.page(complete, 1, 27).getPlayers().size());
        assertThrows(IllegalArgumentException.class, () -> OnlineListPages.page(complete, 2, 27));
    }

    private static PlayerSnapshot player(String name, boolean administrator) {
        return new PlayerSnapshot(name, "uuid-" + name, null, administrator);
    }
}
