package cn.huohuas001.huhobot.onlinelist.skin;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import net.skinsrestorer.api.SkinsRestorerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SkinRestorerSkinResolverTest {
    @AfterEach
    void resetProvider() {
        SkinsRestorerProvider.set(null);
    }

    @Test
    void readsStoredSkinPropertyWithoutLookup() throws Exception {
        FakeStorage storage = new FakeStorage(Optional.of(new FakeProperty(encoded("stored"))), Optional.empty());
        SkinsRestorerProvider.set(new FakeApi(storage));
        SkinRestorerSkinResolver resolver = SkinRestorerSkinResolver.connectWithClassLoader(
            getClass().getClassLoader(),
            true,
            Logger.getLogger("test"),
            false
        );

        String url = resolver.resolve(player());

        assertEquals("https://textures.minecraft.net/texture/stored", url);
        assertEquals(0, storage.lookupCalls);
    }

    @Test
    void fallsBackToJoinLookupForOfflinePlayer() throws Exception {
        FakeStorage storage = new FakeStorage(Optional.empty(), Optional.of(new FakeProperty(encoded("lookup"))));
        SkinsRestorerProvider.set(new FakeApi(storage));
        SkinRestorerSkinResolver resolver = SkinRestorerSkinResolver.connectWithClassLoader(
            getClass().getClassLoader(),
            true,
            Logger.getLogger("test"),
            false
        );

        String url = resolver.resolve(player());

        assertEquals("https://textures.minecraft.net/texture/lookup", url);
        assertEquals(1, storage.lookupCalls);
    }

    private static PlayerSnapshot player() {
        return new PlayerSnapshot("OfflinePlayer", "12345678-1234-1234-1234-123456789abc", null);
    }

    private static String encoded(String hash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/"
            + hash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static final class FakeApi {
        private final FakeStorage storage;

        private FakeApi(FakeStorage storage) {
            this.storage = storage;
        }

        public FakeStorage getPlayerStorage() {
            return storage;
        }
    }

    public static final class FakeStorage {
        private final Optional<FakeProperty> stored;
        private final Optional<FakeProperty> lookup;
        private int lookupCalls;

        private FakeStorage(Optional<FakeProperty> stored, Optional<FakeProperty> lookup) {
            this.stored = stored;
            this.lookup = lookup;
        }

        public Optional<FakeProperty> getSkinOfPlayer(UUID uuid) {
            return stored;
        }

        public Optional<FakeProperty> getSkinForPlayer(UUID uuid, String name, boolean onlineMode) {
            lookupCalls++;
            return lookup;
        }
    }

    public static final class FakeProperty {
        private final String value;

        private FakeProperty(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
