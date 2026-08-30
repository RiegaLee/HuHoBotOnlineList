package cn.huohuas001.huhobot.onlinelist.render;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.model.OnlineListPages;
import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;
import cn.huohuas001.huhobot.onlinelist.skin.AvatarCache;
import cn.huohuas001.huhobot.onlinelist.testing.FakePlayerFactory;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineListRendererTest {
    @Test
    void emptyListUsesOneBodyTile() throws Exception {
        OnlineListRenderer renderer = renderer();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(renderer.render(snapshot(0))));

        assertNotNull(image);
        assertEquals(1200, image.getWidth());
        assertEquals(1280, image.getHeight());
    }

    @Test
    void playerListGrowsOnlyByWholeBodyTiles() throws Exception {
        OnlineListRenderer renderer = renderer();
        for (int players : new int[]{1, 6, 7, 20, 48}) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(renderer.render(snapshot(players))));
            assertEquals(1200, image.getWidth());
            assertEquals(0, (image.getHeight() - OnlineListRenderer.HEADER_HEIGHT - OnlineListRenderer.FOOTER_HEIGHT)
                % OnlineListRenderer.BODY_TILE_HEIGHT);
            assertTrue(image.getHeight() >= 1280);
        }
    }

    @Test
    void createsVisualPreviewArtifact() throws Exception {
        OnlineListRenderer renderer = renderer();
        ServerSnapshot complete = FakePlayerFactory.create(
            "MinecraftServer",
            100,
            65,
            Instant.parse("2026-08-30T03:30:00Z"),
            "VeryLongMinecraftPlayerName_{index3}",
            3
        );
        byte[] png = renderer.render(OnlineListPages.page(complete, 1, 27));
        Path output = Paths.get("build", "preview", "glass-online-list-preview.png");
        Files.createDirectories(output.getParent());
        Files.write(output, png);
        assertTrue(Files.size(output) > 100_000);

        byte[] lastPage = renderer.render(OnlineListPages.page(complete, 3, 27));
        Path lastPageOutput = Paths.get("build", "preview", "glass-online-list-page-3-preview.png");
        Files.write(lastPageOutput, lastPage);
        BufferedImage lastPageImage = ImageIO.read(new ByteArrayInputStream(lastPage));
        assertEquals(1920, lastPageImage.getHeight());
    }

    private static OnlineListRenderer renderer() throws Exception {
        AvatarCache avatars = new AvatarCache(
            Logger.getLogger("test"),
            Runnable::run,
            500,
            500,
            16,
            false
        );
        return new OnlineListRenderer(avatars, 3, "", "POWERED BY HuHoBot");
    }

    private static ServerSnapshot snapshot(int count) {
        List<PlayerSnapshot> players = new ArrayList<PlayerSnapshot>();
        for (int i = 1; i <= count; i++) {
            players.add(new PlayerSnapshot(
                String.format("Player_%02d", i),
                "00000000-0000-0000-0000-" + String.format("%012d", i),
                null,
                avatar(i)
            ));
        }
        return new ServerSnapshot("HuHoBot 测试服务器", 100, players, Instant.parse("2026-08-30T03:30:00Z"));
    }

    private static BufferedImage avatar(int seed) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.getHSBColor((seed * 37 % 360) / 360f, 0.38f, 0.88f));
            graphics.fillRect(0, 0, 64, 64);
            graphics.setColor(new Color(255, 255, 255, 170));
            graphics.fillRect(12, 14, 14, 14);
            graphics.fillRect(38, 14, 14, 14);
            graphics.fillRect(20, 42, 24, 6);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
