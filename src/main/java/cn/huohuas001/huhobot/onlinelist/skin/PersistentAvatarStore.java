package cn.huohuas001.huhobot.onlinelist.skin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/** Stores the last successfully rendered head per player without trusting player-controlled paths. */
final class PersistentAvatarStore {
    private static final int EXPECTED_SIZE = 128;

    private final Path directory;
    private final Logger logger;

    PersistentAvatarStore(Path directory, Logger logger) {
        this.directory = directory == null ? null : directory.toAbsolutePath().normalize();
        this.logger = logger;
    }

    synchronized BufferedImage load(String playerId) {
        if (directory == null) return null;
        Path file = fileFor(playerId);
        if (!Files.isRegularFile(file)) return null;
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null || image.getWidth() != EXPECTED_SIZE || image.getHeight() != EXPECTED_SIZE) {
                return null;
            }
            return image;
        } catch (IOException error) {
            logger.fine("无法读取玩家头像缓存 " + file.getFileName() + "：" + error.getMessage());
            return null;
        }
    }

    synchronized void save(String playerId, BufferedImage avatar) {
        if (directory == null || avatar == null) return;
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            Path target = fileFor(playerId);
            temporary = Files.createTempFile(directory, "avatar-", ".tmp");
            if (!ImageIO.write(avatar, "png", temporary.toFile())) {
                throw new IOException("PNG writer is unavailable");
            }
            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            logger.fine("无法保存玩家头像缓存：" + error.getMessage());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup only.
                }
            }
        }
    }

    private Path fileFor(String playerId) {
        return directory.resolve(sha256(playerId) + ".png");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte valueByte : digest) result.append(String.format("%02x", valueByte & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
