package cn.huohuas001.huhobot.onlinelist.skin;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BukkitSkinResolverTest {
    @Test
    void readsModernBukkitProfile() throws Exception {
        String resolved = new BukkitSkinResolver().resolve(
            new ModernPlayer(new URL("http://textures.minecraft.net/texture/modern"))
        );

        assertEquals("https://textures.minecraft.net/texture/modern", resolved);
    }

    @Test
    void readsPaper1211RecordStyleGameProfile() {
        String resolved = new BukkitSkinResolver().resolve(new Paper1211Player(encoded("record")));

        assertEquals("https://textures.minecraft.net/texture/record", resolved);
    }

    @Test
    void keepsCompatibilityWithGetterStyleGameProfile() {
        String resolved = new BukkitSkinResolver().resolve(new LegacyPlayer(encoded("legacy")));

        assertEquals("https://textures.minecraft.net/texture/legacy", resolved);
    }

    private static String encoded(String hash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/"
            + hash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static final class ModernPlayer {
        private final URL skin;

        private ModernPlayer(URL skin) {
            this.skin = skin;
        }

        public String getName() {
            return "Modern";
        }

        public Object getPlayerProfile() {
            return new Object() {
                public Object getTextures() {
                    return new Object() {
                        public URL getSkin() {
                            return skin;
                        }
                    };
                }
            };
        }
    }

    public static final class Paper1211Player {
        private final String encoded;

        private Paper1211Player(String encoded) {
            this.encoded = encoded;
        }

        public String getName() {
            return "Paper1211";
        }

        public Object getPlayerProfile() {
            return new EmptyProfile();
        }

        public Object getProfile() {
            return new Object() {
                public FakeProperties properties() {
                    return new FakeProperties(Collections.singletonList(new RecordProperty(encoded)));
                }
            };
        }
    }

    public static final class LegacyPlayer {
        private final String encoded;

        private LegacyPlayer(String encoded) {
            this.encoded = encoded;
        }

        public String getName() {
            return "Legacy";
        }

        public Object getPlayerProfile() {
            return new EmptyProfile();
        }

        public Object getProfile() {
            return new Object() {
                public FakeProperties getProperties() {
                    return new FakeProperties(Collections.singletonList(new GetterProperty(encoded)));
                }
            };
        }
    }

    public static final class EmptyProfile {
        public Object getTextures() {
            return new Object() {
                public URL getSkin() {
                    return null;
                }
            };
        }
    }

    public static final class FakeProperties {
        private final Collection<?> textures;

        private FakeProperties(Collection<?> textures) {
            this.textures = textures;
        }

        public Collection<?> get(String key) {
            return "textures".equals(key) ? textures : Collections.emptyList();
        }
    }

    public static final class RecordProperty {
        private final String value;

        private RecordProperty(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static final class GetterProperty {
        private final String value;

        private GetterProperty(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
