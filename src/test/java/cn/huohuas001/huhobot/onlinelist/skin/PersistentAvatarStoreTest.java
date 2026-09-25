package cn.huohuas001.huhobot.onlinelist.skin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class PersistentAvatarStoreTest {
    @TempDir
    Path tempDirectory;

    @Test
    void savesAndLoadsAvatarBySafeHashedPlayerId() throws Exception {
        Path directory = tempDirectory.resolve("avatars");
        PersistentAvatarStore store = new PersistentAvatarStore(directory, Logger.getLogger("test"));
        BufferedImage avatar = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        avatar.setRGB(0, 0, Color.MAGENTA.getRGB());
        avatar.setRGB(127, 127, Color.CYAN.getRGB());

        store.save("../../unsafe-player-id", avatar);
        BufferedImage loaded = store.load("../../unsafe-player-id");

        assertNotNull(loaded);
        assertEquals(Color.MAGENTA.getRGB(), loaded.getRGB(0, 0));
        assertEquals(Color.CYAN.getRGB(), loaded.getRGB(127, 127));
        assertEquals(1L, Files.list(directory).count());
        assertNull(store.load("different-player"));
    }
}
