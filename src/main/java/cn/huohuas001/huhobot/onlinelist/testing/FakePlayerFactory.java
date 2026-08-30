package cn.huohuas001.huhobot.onlinelist.testing;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 生成完全本地的虚假玩家与像素头像，用于版式和 QQ 图片发送测试。 */
public final class FakePlayerFactory {
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
            players.add(new PlayerSnapshot(name, uuid, null, avatar(index), index <= safeAdministratorCount));
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

    private static BufferedImage avatar(int seed) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            float hue = (seed * 47 % 360) / 360f;
            Color base = Color.getHSBColor(hue, 0.34f, 0.88f);
            Color shade = Color.getHSBColor(hue, 0.42f, 0.67f);
            graphics.setColor(base);
            graphics.fillRect(0, 0, 64, 64);
            graphics.setColor(shade);
            graphics.fillRect(0, 0, 64, 9);
            graphics.fillRect(0, 9, 8, 55);
            graphics.setColor(new Color(242, 253, 255));
            graphics.fillRect(14, 20, 12, 12);
            graphics.fillRect(38, 20, 12, 12);
            graphics.fillRect(20, 44, 24, 6);
            graphics.setColor(new Color(44, 91, 116));
            graphics.fillRect(18, 24, 5, 6);
            graphics.fillRect(42, 24, 5, 6);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
