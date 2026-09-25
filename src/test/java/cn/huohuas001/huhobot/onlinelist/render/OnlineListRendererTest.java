package cn.huohuas001.huhobot.onlinelist.render;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.model.OnlineListPages;
import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;
import cn.huohuas001.huhobot.onlinelist.skin.AvatarCache;
import cn.huohuas001.huhobot.onlinelist.testing.FakePlayerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Font;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineListRendererTest {
    @TempDir
    Path tempDirectory;

    @Test
    void emptyListUsesTheNewBuiltInMaterialTheme() throws Exception {
        OnlineListRenderer renderer = renderer();
        byte[] png = renderer.render(snapshot(0));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        Path output = Paths.get("build", "preview", "default-empty-preview.png");
        Files.createDirectories(output.getParent());
        Files.write(output, png);

        assertNotNull(image);
        assertEquals(1200, image.getWidth());
        assertEquals(900, image.getHeight());
        assertTrue(Files.size(output) > 50_000);
    }

    @Test
    void playerListGrowsExactlyWithTheTransparentRoster() throws Exception {
        OnlineListRenderer renderer = renderer();
        for (int players : new int[]{1, 6, 7, 20, 48}) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(renderer.render(snapshot(players))));
            assertEquals(1200, image.getWidth());
            int rows = (players + 2) / 3;
            int expectedHeight = Math.max(900, 220 + 104 + rows * 96 + 20 + 180);
            assertEquals(expectedHeight, image.getHeight());
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
        assertEquals(908, lastPageImage.getHeight());
    }

    @Test
    void bundledSteveFallbackIsOpaqueAndRecognizable() {
        BufferedImage steve = OnlineListRenderer.createDefaultSteveHead();

        assertEquals(8, steve.getWidth());
        assertEquals(8, steve.getHeight());
        assertEquals(0xFF523D89, steve.getRGB(2, 4));
        assertEquals(0xFFFFFFFF, steve.getRGB(1, 4));
        assertEquals(0xFF332411, steve.getRGB(0, 0));
    }

    @Test
    void bundledTitleFontContainsTheLockedFourGlyphs() {
        Font font = OnlineListRenderer.loadOnlineTitleFont();

        assertEquals(-1, font.canDisplayUpTo("在线列表"));
        assertTrue(font.getFamily().contains("HuHoBot Online Title"));
    }

    @Test
    void rendersSteveFallbackPreviewForPlayerWithoutSkin() throws Exception {
        List<PlayerSnapshot> players = new ArrayList<PlayerSnapshot>();
        players.add(new PlayerSnapshot(
            "NoSkinPlayer",
            "00000000-0000-0000-0000-000000000001",
            null
        ));
        ServerSnapshot snapshot = new ServerSnapshot(
            "Steve Fallback Test",
            20,
            players,
            Instant.parse("2026-09-04T04:00:00Z")
        );

        byte[] png = renderer().render(snapshot);
        Path output = Paths.get("build", "preview", "steve-fallback-preview.png");
        Files.createDirectories(output.getParent());
        Files.write(output, png);

        assertTrue(Files.size(output) > 50_000);
    }

    @Test
    void customBackgroundKeepsDynamicCanvasAndWallpaperVisible() throws Exception {
        BufferedImage wallpaper = new BufferedImage(400, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = wallpaper.createGraphics();
        try {
            graphics.setPaint(new GradientPaint(
                0, 0, new Color(29, 45, 77),
                wallpaper.getWidth(), wallpaper.getHeight(), new Color(188, 103, 81)
            ));
            graphics.fillRect(0, 0, wallpaper.getWidth(), wallpaper.getHeight());
            graphics.setColor(new Color(100, 190, 210, 120));
            graphics.fillOval(170, 180, 330, 430);
            graphics.setColor(new Color(244, 190, 106, 115));
            graphics.fillOval(-90, 500, 360, 330);
        } finally {
            graphics.dispose();
        }
        Path file = tempDirectory.resolve("online-list.png");
        ImageIO.write(wallpaper, "png", file.toFile());

        OnlineListRenderer renderer = new OnlineListRenderer(
            avatars(), 3, "", "POWERED BY HuHoBot", "steve", file, "cover"
        );
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(renderer.render(snapshot(9))));

        Path preview = Paths.get("build", "preview", "custom-background-preview.png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(image, "png", preview.toFile());

        assertEquals(1200, image.getWidth());
        assertEquals(900, image.getHeight());
        assertNotEquals(image.getRGB(160, 700), image.getRGB(1040, 700));
        assertEquals(0, new Color(image.getRGB(0, 0), true).getAlpha());
    }

    @Test
    void customPlayerCardKeepsAllFourAvatarCornersVisible() throws Exception {
        BufferedImage wallpaper = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D wallpaperGraphics = wallpaper.createGraphics();
        try {
            wallpaperGraphics.setColor(new Color(25, 31, 43));
            wallpaperGraphics.fillRect(0, 0, wallpaper.getWidth(), wallpaper.getHeight());
        } finally {
            wallpaperGraphics.dispose();
        }
        Path file = tempDirectory.resolve("avatar-corners.png");
        ImageIO.write(wallpaper, "png", file.toFile());

        BufferedImage avatar = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D avatarGraphics = avatar.createGraphics();
        try {
            avatarGraphics.setColor(Color.RED);
            avatarGraphics.fillRect(0, 0, avatar.getWidth(), avatar.getHeight());
        } finally {
            avatarGraphics.dispose();
        }
        List<PlayerSnapshot> players = new ArrayList<PlayerSnapshot>();
        players.add(new PlayerSnapshot(
            "CompleteAvatar",
            "00000000-0000-0000-0000-000000000001",
            null,
            avatar
        ));
        ServerSnapshot snapshot = new ServerSnapshot(
            "Avatar Test",
            100,
            players,
            Instant.parse("2026-09-22T01:00:00Z")
        );
        OnlineListRenderer renderer = new OnlineListRenderer(
            avatars(), 3, "", "POWERED BY HuHoBot", "steve", file, "cover"
        );

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(renderer.render(snapshot)));

        assertEquals(Color.RED.getRGB(), image.getRGB(100, 339));
        assertEquals(Color.RED.getRGB(), image.getRGB(151, 339));
        assertEquals(Color.RED.getRGB(), image.getRGB(100, 390));
        assertEquals(Color.RED.getRGB(), image.getRGB(151, 390));
    }

    @Test
    void brightWallpaperUsesLightTranslucentCardsInsteadOfBlackBlocks() throws Exception {
        BufferedImage wallpaper = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = wallpaper.createGraphics();
        try {
            graphics.setPaint(new GradientPaint(
                0, 0, new Color(255, 255, 255),
                wallpaper.getWidth(), wallpaper.getHeight(), new Color(218, 235, 250)
            ));
            graphics.fillRect(0, 0, wallpaper.getWidth(), wallpaper.getHeight());
        } finally {
            graphics.dispose();
        }
        Path file = tempDirectory.resolve("bright-wallpaper.png");
        ImageIO.write(wallpaper, "png", file.toFile());
        OnlineListRenderer renderer = new OnlineListRenderer(
            avatars(),
            3,
            "",
            "POWERED BY HuHoBot",
            "steve",
            file,
            "cover",
            0.12,
            true,
            0.38
        );

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(renderer.render(snapshot(9))));
        Path preview = Paths.get("build", "preview", "bright-wallpaper-preview.png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(image, "png", preview.toFile());

        Color playerCard = new Color(image.getRGB(300, 390), true);
        assertTrue(playerCard.getRed() > 100);
        assertTrue(playerCard.getGreen() > 100);
        assertTrue(playerCard.getBlue() > 100);
        assertTrue(Files.size(preview) > 50_000);
    }

    @Test
    void rendersExternalWallpaperThroughProductionPipelineWhenConfigured() throws Exception {
        String configured = System.getenv("ONLINE_LIST_PREVIEW_BACKGROUND");
        Assumptions.assumeTrue(configured != null && !configured.trim().isEmpty());
        Path wallpaper = Paths.get(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isRegularFile(wallpaper));

        OnlineListRenderer renderer = new OnlineListRenderer(
            avatars(), 3, "", "POWERED BY HuHoBot", "steve", wallpaper, "cover"
        );
        byte[] png = renderer.render(snapshot(9));
        Path output = Paths.get("build", "preview", "actual-wallpaper-preview.png");
        Files.createDirectories(output.getParent());
        Files.write(output, png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(1200, image.getWidth());
        assertEquals(900, image.getHeight());
        assertTrue(Files.size(output) > 100_000);
    }

    @Test
    void rejectsUnsupportedCustomBackgroundFit() {
        BufferedImage wallpaper = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        assertThrows(
            IllegalArgumentException.class,
            () -> OnlineListBackground.of(wallpaper, "contain")
        );
    }

    private static OnlineListRenderer renderer() throws Exception {
        return new OnlineListRenderer(avatars(), 3, "", "POWERED BY HuHoBot");
    }

    private static AvatarCache avatars() {
        AvatarCache avatars = new AvatarCache(
            Logger.getLogger("test"),
            Runnable::run,
            500,
            500,
            16,
            false
        );
        return avatars;
    }

    private static ServerSnapshot snapshot(int count) {
        List<PlayerSnapshot> players = new ArrayList<PlayerSnapshot>();
        for (int i = 1; i <= count; i++) {
            players.add(new PlayerSnapshot(
                String.format("Player_%02d", i),
                "00000000-0000-0000-0000-" + String.format("%012d", i),
                null
            ));
        }
        return new ServerSnapshot("HuHoBot 测试服务器", 100, players, Instant.parse("2026-08-30T03:30:00Z"));
    }

}
