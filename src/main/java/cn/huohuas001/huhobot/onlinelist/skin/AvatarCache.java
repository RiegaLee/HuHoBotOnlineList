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
    private final SkinUrlResolver fallbackSkinResolver;
    private final boolean debug;
    private final Map<String, BufferedImage> cacheByUrl;
    private final Map<String, BufferedImage> lastSuccessfulByPlayer;

    public AvatarCache(
        Logger logger,
        Executor downloadExecutor,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxEntries,
        boolean enabled
    ) {
        this(logger, downloadExecutor, connectTimeoutMs, readTimeoutMs, maxEntries, enabled, null, false);
    }

    public AvatarCache(
        Logger logger,
        Executor downloadExecutor,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxEntries,
        boolean enabled,
        SkinUrlResolver fallbackSkinResolver,
        boolean debug
    ) {
        this.logger = logger;
        this.downloadExecutor = downloadExecutor;
        this.connectTimeoutMs = Math.max(250, connectTimeoutMs);
        this.readTimeoutMs = Math.max(250, readTimeoutMs);
        this.enabled = enabled;
        this.fallbackSkinResolver = fallbackSkinResolver;
        this.debug = debug;
        final int capacity = Math.max(8, maxEntries);
        this.cacheByUrl = lruCache(capacity);
        this.lastSuccessfulByPlayer = lruCache(capacity);
    }

    private static Map<String, BufferedImage> lruCache(final int capacity) {
        return Collections.synchronizedMap(new LinkedHashMap<String, BufferedImage>(16, 0.75f, true) {
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
            } else if (enabled) {
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
        String currentUrl = SkinTextureUrl.normalize(player.getSkinUrl());
        BufferedImage current = loadUrl(player, currentUrl);
        if (current != null) {
            remember(player, current);
            return current;
        }

        String restoredUrl = resolveFallbackUrl(player);
        if (restoredUrl != null && !restoredUrl.equals(currentUrl)) {
            BufferedImage restored = loadUrl(player, restoredUrl);
            if (restored != null) {
                remember(player, restored);
                return restored;
            }
        }

        BufferedImage previous = lastSuccessfulByPlayer.get(player.getUuid());
        if (previous != null) {
            trace(player, "本次皮肤不可用，沿用该玩家最后一次成功头像");
            return previous;
        }
        trace(player, "没有可用皮肤，使用内置 Steve 头像");
        return null;
    }

    private BufferedImage loadUrl(PlayerSnapshot player, String url) {
        if (url == null) return null;
        BufferedImage cached = cacheByUrl.get(url);
        if (cached != null) {
            trace(player, "命中皮肤图片缓存");
            return cached;
        }
        try {
            BufferedImage skin = download(url);
            if (!isSupportedSkinSize(skin)) {
                throw new IllegalStateException("unsupported skin dimensions");
            }
            BufferedImage icon = cropHead(skin);
            if (!hasVisiblePixels(icon)) throw new IllegalStateException("skin head is fully transparent");
            cacheByUrl.put(url, icon);
            trace(player, "皮肤下载并裁剪成功");
            return icon;
        } catch (Exception error) {
            if (debug) {
                logger.info("[头像诊断] " + player.getName() + "：皮肤下载/裁剪失败：" + error.getMessage());
            } else {
                logger.fine("玩家 " + player.getName() + " 的皮肤下载失败，使用后备头像：" + error.getMessage());
            }
            return null;
        }
    }

    private String resolveFallbackUrl(PlayerSnapshot player) {
        if (fallbackSkinResolver == null) return null;
        try {
            return SkinTextureUrl.normalize(fallbackSkinResolver.resolve(player));
        } catch (Throwable error) {
            if (debug) {
                logger.info("[头像诊断] " + player.getName() + "：后备皮肤解析失败：" + error.getMessage());
            }
            return null;
        }
    }

    private void remember(PlayerSnapshot player, BufferedImage avatar) {
        lastSuccessfulByPlayer.put(player.getUuid(), avatar);
    }

    private void trace(PlayerSnapshot player, String message) {
        if (debug) logger.info("[头像诊断] " + player.getName() + "：" + message);
    }

    private BufferedImage download(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(true);
        connection.setRequestProperty("User-Agent", "HuHoBotOnlineList/1.0");
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
        int scale = skin.getWidth() / 64;
        BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(skin, 0, 0, ICON_SIZE, ICON_SIZE, 8 * scale, 8 * scale, 16 * scale, 16 * scale, null);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.drawImage(skin, 0, 0, ICON_SIZE, ICON_SIZE, 40 * scale, 8 * scale, 48 * scale, 16 * scale, null);
        } finally {
            graphics.dispose();
        }
        return icon;
    }

    static boolean isSupportedSkinSize(BufferedImage skin) {
        if (skin == null || skin.getWidth() < 64 || skin.getWidth() % 64 != 0) return false;
        int scale = skin.getWidth() / 64;
        return skin.getHeight() == 32 * scale || skin.getHeight() == 64 * scale;
    }

    static boolean hasVisiblePixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }
}
