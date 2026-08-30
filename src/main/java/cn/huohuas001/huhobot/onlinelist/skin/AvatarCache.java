package cn.huohuas001.huhobot.onlinelist.skin;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 有界内存缓存；并发下载皮肤后裁出 Minecraft 头部与帽子层。 */
public final class AvatarCache {
    private static final int ICON_SIZE = 128;
    private static final int MAX_DOWNLOAD_BYTES = 4 * 1024 * 1024;

    private final Logger logger;
    private final Executor downloadExecutor;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final boolean enabled;
    private final Map<String, BufferedImage> cache;

    public AvatarCache(
        Logger logger,
        Executor downloadExecutor,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxEntries,
        boolean enabled
    ) {
        this.logger = logger;
        this.downloadExecutor = downloadExecutor;
        this.connectTimeoutMs = Math.max(250, connectTimeoutMs);
        this.readTimeoutMs = Math.max(250, readTimeoutMs);
        this.enabled = enabled;
        final int capacity = Math.max(8, maxEntries);
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, BufferedImage>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
                return size() > capacity;
            }
        });
    }

    public Map<String, BufferedImage> loadAll(List<PlayerSnapshot> players) {
        Map<String, BufferedImage> result = new LinkedHashMap<String, BufferedImage>();
        List<CompletableFuture<Map.Entry<String, BufferedImage>>> futures = new ArrayList<CompletableFuture<Map.Entry<String, BufferedImage>>>();
        for (PlayerSnapshot player : players) {
            if (player.getAvatarOverride() != null) {
                result.put(player.getUuid(), player.getAvatarOverride());
            } else if (enabled && player.getSkinUrl() != null && !player.getSkinUrl().trim().isEmpty()) {
                futures.add(CompletableFuture.supplyAsync(() ->
                    new java.util.AbstractMap.SimpleImmutableEntry<String, BufferedImage>(player.getUuid(), load(player)),
                    downloadExecutor
                ));
            }
        }
        for (CompletableFuture<Map.Entry<String, BufferedImage>> future : futures) {
            try {
                Map.Entry<String, BufferedImage> entry = future.join();
                if (entry.getValue() != null) result.put(entry.getKey(), entry.getValue());
            } catch (RuntimeException error) {
                logger.log(Level.FINE, "Skin download task failed", error);
            }
        }
        return result;
    }

    private BufferedImage load(PlayerSnapshot player) {
        String url = player.getSkinUrl();
        BufferedImage cached = cache.get(url);
        if (cached != null) return cached;
        try {
            BufferedImage skin = download(url);
            if (skin == null || skin.getWidth() < 48 || skin.getHeight() < 16) return null;
            BufferedImage icon = cropHead(skin);
            cache.put(url, icon);
            return icon;
        } catch (Exception error) {
            logger.fine("玩家 " + player.getName() + " 的皮肤下载失败，使用占位头像：" + error.getMessage());
            return null;
        }
    }

    private BufferedImage download(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(true);
        connection.setRequestProperty("User-Agent", "HuHoBotOnlineList/0.1");
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
            int length = connection.getContentLength();
            if (length > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("skin image is too large");
            byte[] bytes;
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    total += read;
                    if (total > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("skin image is too large");
                    out.write(buffer, 0, read);
                }
                bytes = out.toByteArray();
            }
            return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } finally {
            connection.disconnect();
        }
    }

    static BufferedImage cropHead(BufferedImage skin) {
        BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(skin, 0, 0, ICON_SIZE, ICON_SIZE, 8, 8, 16, 16, null);
            if (skin.getHeight() >= 64) {
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.drawImage(skin, 0, 0, ICON_SIZE, ICON_SIZE, 40, 8, 48, 16, null);
            }
        } finally {
            graphics.dispose();
        }
        return icon;
    }
}
