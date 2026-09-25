package cn.huohuas001.huhobot.onlinelist.testing;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;
import cn.huohuas001.huhobot.onlinelist.render.OnlineListRenderer;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Generates a local roster with the real bundled Minecraft Steve face for layout and QQ tests. */
public final class FakePlayerFactory {
    private static final BufferedImage STEVE_AVATAR = OnlineListRenderer.createDefaultSteveHead();

    private FakePlayerFactory() {
    }

    public static ServerSnapshot create(String serverName, int maxPlayers, int count, Instant capturedAt) {
        return create(serverName, maxPlayers, count, capturedAt, "TestPlayer{index3}");
    }

    public static ServerSnapshot create(
        String serverName,
        int maxPlayers,
        int count,
        Instant capturedAt,
        String nameTemplate
    ) {
        return create(serverName, maxPlayers, count, capturedAt, nameTemplate, 0);
    }

    public static ServerSnapshot create(
        String serverName,
        int maxPlayers,
        int count,
        Instant capturedAt,
        String nameTemplate,
        int administratorCount
    ) {
        int safeCount = Math.max(0, Math.min(300, count));
        int safeAdministratorCount = Math.max(0, Math.min(safeCount, administratorCount));
        String safeTemplate = normalizeTemplate(nameTemplate);
        List<PlayerSnapshot> players = new ArrayList<PlayerSnapshot>(safeCount);
        for (int index = 1; index <= safeCount; index++) {
            String name = formatName(safeTemplate, index);
            String uuid = UUID.nameUUIDFromBytes(("HuHoBotOnlineList:" + index).getBytes(StandardCharsets.UTF_8)).toString();
            players.add(new PlayerSnapshot(name, uuid, null, STEVE_AVATAR, index <= safeAdministratorCount));
        }
        return new ServerSnapshot(
            serverName + " · 测试名单",
            Math.max(Math.max(0, maxPlayers), safeCount),
            players,
            capturedAt
        );
    }

    private static String normalizeTemplate(String template) {
        String value = template == null ? "" : template.trim();
        if (value.isEmpty()) value = "TestPlayer{index3}";
        return value.length() > 128 ? value.substring(0, 128) : value;
    }

    private static String formatName(String template, int index) {
        return template
            .replace("{index}", Integer.toString(index))
            .replace("{index2}", String.format("%02d", index))
            .replace("{index3}", String.format("%03d", index));
    }

}
