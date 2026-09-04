package cn.huohuas001.huhobot.onlinelist.skin;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.util.Reflect;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/** 通过反射读取 SkinsRestorer v15+ 已存储或将要应用的皮肤。 */
public final class SkinRestorerSkinResolver implements SkinUrlResolver {
    private static final String PROVIDER_CLASS = "net.skinsrestorer.api.SkinsRestorerProvider";

    private final Object playerStorage;
    private final boolean allowLookup;
    private final Logger logger;
    private final boolean debug;

    private SkinRestorerSkinResolver(
        Object playerStorage,
        boolean allowLookup,
        Logger logger,
        boolean debug
    ) {
        this.playerStorage = playerStorage;
        this.allowLookup = allowLookup;
        this.logger = logger;
        this.debug = debug;
    }

    public static SkinRestorerSkinResolver connect(
        Plugin plugin,
        boolean allowLookup,
        Logger logger,
        boolean debug
    ) throws ReflectiveOperationException {
        return connectWithClassLoader(plugin.getClass().getClassLoader(), allowLookup, logger, debug);
    }

    static SkinRestorerSkinResolver connectWithClassLoader(
        ClassLoader loader,
        boolean allowLookup,
        Logger logger,
        boolean debug
    ) throws ReflectiveOperationException {
        Class<?> provider = Class.forName(PROVIDER_CLASS, true, loader);
        Method get = provider.getMethod("get");
        Object api = get.invoke(null);
        Object storage = Reflect.invoke(api, "getPlayerStorage");
        return new SkinRestorerSkinResolver(storage, allowLookup, logger, debug);
    }

    @Override
    public String resolve(PlayerSnapshot player) {
        try {
            UUID uuid = UUID.fromString(player.getUuid());
            Object property = unwrap(Reflect.invoke(playerStorage, "getSkinOfPlayer", uuid));
            String source = "stored";
            if (property == null && allowLookup) {
                property = lookup(uuid, player.getName());
                source = "join lookup";
            }
            if (property == null) {
                trace(player, "没有已绑定的皮肤");
                return null;
            }
            Object encoded = Reflect.read(property, "value");
            String url = SkinTextureUrl.fromEncodedProperty(encoded == null ? null : encoded.toString());
            trace(player, url == null ? source + " 属性不含 SKIN URL" : "已从 " + source + " 取得皮肤");
            return url;
        } catch (Throwable error) {
            if (debug) {
                logger.info("[头像诊断] SkinsRestorer 读取 " + player.getName() + " 失败：" + concise(error));
            } else {
                logger.fine("SkinsRestorer skin lookup failed for " + player.getName() + ": " + concise(error));
            }
            return null;
        }
    }

    private Object lookup(UUID uuid, String name) throws ReflectiveOperationException {
        Method threeArguments = Reflect.findCompatibleMethod(
            playerStorage.getClass(),
            "getSkinForPlayer",
            uuid,
            name,
            false
        );
        Object result = threeArguments != null
            ? Reflect.invoke(playerStorage, "getSkinForPlayer", uuid, name, false)
            : Reflect.invoke(playerStorage, "getSkinForPlayer", uuid, name);
        return unwrap(result);
    }

    private static Object unwrap(Object value) {
        if (value instanceof Optional) return ((Optional<?>) value).orElse(null);
        return value;
    }

    private void trace(PlayerSnapshot player, String message) {
        if (debug) logger.info("[头像诊断] " + player.getName() + "：SkinsRestorer " + message);
    }

    private static String concise(Throwable error) {
        Throwable cursor = error;
        while (cursor.getCause() != null && cursor.getCause() != cursor) cursor = cursor.getCause();
        return cursor.getClass().getSimpleName() + (cursor.getMessage() == null ? "" : ": " + cursor.getMessage());
    }
}
