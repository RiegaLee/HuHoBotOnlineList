package cn.huohuas001.huhobot.onlinelist.skin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从 Mojang/SkinRestorer 的 textures 属性中提取皮肤地址。 */
final class SkinTextureUrl {
    private static final Pattern SKIN_URL = Pattern.compile(
        "\\\"SKIN\\\"\\s*:\\s*\\{.*?\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"",
        Pattern.DOTALL
    );

    private SkinTextureUrl() {
    }

    static String fromEncodedProperty(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) return null;
        String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        Matcher matcher = SKIN_URL.matcher(json);
        return matcher.find() ? normalize(matcher.group(1).replace("\\/", "/")) : null;
    }

    static String normalize(String url) {
        if (url == null) return null;
        String value = url.trim();
        if (value.isEmpty()) return null;
        if (value.startsWith("http://textures.minecraft.net/")) {
            return "https://" + value.substring("http://".length());
        }
        return value;
    }
}
