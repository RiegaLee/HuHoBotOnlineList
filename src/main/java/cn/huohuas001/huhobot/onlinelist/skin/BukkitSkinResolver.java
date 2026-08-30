package cn.huohuas001.huhobot.onlinelist.skin;

import cn.huohuas001.huhobot.onlinelist.util.Reflect;
import org.bukkit.entity.Player;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从 Bukkit/Paper PlayerProfile 或 CraftPlayer GameProfile 中读取当前皮肤材质地址。 */
public final class BukkitSkinResolver {
    private static final Pattern TEXTURE_URL = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    public String resolve(Player player) {
        String modern = modernProfile(player);
        if (modern != null) return normalize(modern);
        String legacy = legacyProfile(player);
        return legacy == null ? null : normalize(legacy);
    }

    private String modernProfile(Player player) {
        try {
            Object profile = Reflect.invoke(player, "getPlayerProfile");
            Object textures = Reflect.invoke(profile, "getTextures");
            Object skin = Reflect.invoke(textures, "getSkin");
            if (skin instanceof URL) return skin.toString();
            if (skin != null) return skin.toString();
        } catch (Throwable ignored) {
            // API did not exist before modern PlayerProfile support.
        }
        return null;
    }

    private String legacyProfile(Player player) {
        try {
            Object profile = Reflect.invoke(player, "getProfile");
            Object properties = Reflect.invoke(profile, "getProperties");
            Object values = Reflect.invoke(properties, "get", "textures");
            if (!(values instanceof Collection)) return null;
            for (Object property : (Collection<?>) values) {
                Object encoded = Reflect.invoke(property, "getValue");
                if (encoded == null) continue;
                String json = new String(Base64.getDecoder().decode(encoded.toString()), StandardCharsets.UTF_8);
                Matcher matcher = TEXTURE_URL.matcher(json);
                if (matcher.find()) return matcher.group(1).replace("\\/", "/");
            }
        } catch (Throwable ignored) {
            // Offline-mode/default skins legitimately have no textures property.
        }
        return null;
    }

    private static String normalize(String url) {
        if (url.startsWith("http://textures.minecraft.net/")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }
}
