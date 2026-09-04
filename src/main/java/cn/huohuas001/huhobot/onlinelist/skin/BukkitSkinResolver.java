package cn.huohuas001.huhobot.onlinelist.skin;

import cn.huohuas001.huhobot.onlinelist.util.Reflect;

import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;

/** 从 Bukkit/Paper PlayerProfile 或 CraftPlayer GameProfile 中读取当前皮肤材质地址。 */
public final class BukkitSkinResolver {
    private final Logger logger;
    private final boolean debug;

    public BukkitSkinResolver() {
        this(null, false);
    }

    public BukkitSkinResolver(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }

    public String resolve(Object player) {
        String modern = modernProfile(player);
        if (modern != null) {
            trace(player, "Bukkit PlayerProfile", true);
            return SkinTextureUrl.normalize(modern);
        }
        String legacy = legacyProfile(player);
        trace(player, "底层 GameProfile", legacy != null);
        return SkinTextureUrl.normalize(legacy);
    }

    private String modernProfile(Object player) {
        try {
            Object profile = Reflect.invoke(player, "getPlayerProfile");
            Object textures = Reflect.invoke(profile, "getTextures");
            Object skin = Reflect.invoke(textures, "getSkin");
            if (skin instanceof URL) return skin.toString();
            if (skin != null) return skin.toString();
        } catch (Throwable error) {
            traceFailure(player, "Bukkit PlayerProfile", error);
        }
        return null;
    }

    private String legacyProfile(Object player) {
        try {
            Object profile = Reflect.invoke(player, "getProfile");
            // 旧 Authlib 使用 getProperties()/getValue()；Paper 1.21.11 使用 properties()/value()。
            Object properties = Reflect.read(profile, "properties");
            Object values = Reflect.invoke(properties, "get", "textures");
            if (!(values instanceof Collection)) return null;
            for (Object property : (Collection<?>) values) {
                Object encoded = Reflect.read(property, "value");
                if (encoded == null) continue;
                String url = SkinTextureUrl.fromEncodedProperty(encoded.toString());
                if (url != null) return url;
            }
        } catch (Throwable error) {
            traceFailure(player, "底层 GameProfile", error);
        }
        return null;
    }

    private void trace(Object player, String source, boolean found) {
        if (!debug || logger == null) return;
        logger.info("[头像诊断] " + playerName(player) + "：" + source + (found ? " 取得皮肤" : " 没有皮肤"));
    }

    private void traceFailure(Object player, String source, Throwable error) {
        if (!debug || logger == null) return;
        logger.info("[头像诊断] " + playerName(player) + "：" + source + " 读取失败：" + concise(error));
    }

    private static String playerName(Object player) {
        try {
            Object name = Reflect.invoke(player, "getName");
            return name == null ? "<unknown>" : name.toString();
        } catch (Throwable ignored) {
            return "<unknown>";
        }
    }

    private static String concise(Throwable error) {
        Throwable cursor = error;
        while (cursor.getCause() != null && cursor.getCause() != cursor) cursor = cursor.getCause();
        return cursor.getClass().getSimpleName() + (cursor.getMessage() == null ? "" : ": " + cursor.getMessage());
    }
}
