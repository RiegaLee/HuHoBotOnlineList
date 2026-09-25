package cn.huohuas001.huhobot.onlinelist.skin;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AvatarCacheTest {
    @Test
    void acceptsStandardAndHighResolutionSkinSizes() {
        assertTrue(AvatarCache.isSupportedSkinSize(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB)));
        assertTrue(AvatarCache.isSupportedSkinSize(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)));
        assertTrue(AvatarCache.isSupportedSkinSize(new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)));
        assertFalse(AvatarCache.isSupportedSkinSize(new BufferedImage(48, 16, BufferedImage.TYPE_INT_ARGB)));
    }

    @Test
    void scalesHighResolutionHeadAndAppliesHatLayer() {
        BufferedImage skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = skin.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(16, 16, 16, 16);
            graphics.setColor(new Color(0, 0, 255, 255));
            graphics.fillRect(80, 16, 16, 16);
        } finally {
            graphics.dispose();
        }

        BufferedImage head = AvatarCache.cropHead(skin);

        assertTrue(AvatarCache.hasVisiblePixels(head));
        assertEquals(Color.BLUE.getRGB(), head.getRGB(64, 64));
    }

    @Test
    void detectsFullyTransparentHead() {
        assertFalse(AvatarCache.hasVisiblePixels(new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)));
    }

    @Test
    void rejectedDownloadTaskFallsBackWithoutBreakingTheWholeList() {
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("queue full");
        };
        AvatarCache cache = new AvatarCache(
            Logger.getLogger("test"),
            rejectingExecutor,
            500,
            500,
            16,
            true
        );
        PlayerSnapshot player = new PlayerSnapshot(
            "QueueRejected",
            "00000000-0000-0000-0000-000000000001",
            "https://textures.minecraft.net/texture/unreachable"
        );

        Map<String, BufferedImage> result = cache.loadAll(Collections.singletonList(player));

        assertTrue(result.isEmpty());
    }
}
