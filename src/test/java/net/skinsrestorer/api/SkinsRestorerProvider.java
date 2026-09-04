package net.skinsrestorer.api;

/** Test fixture matching the SkinsRestorer v15+ static provider. */
public final class SkinsRestorerProvider {
    private static Object api;

    private SkinsRestorerProvider() {
    }

    public static Object get() {
        if (api == null) throw new IllegalStateException("API is not set");
        return api;
    }

    public static void set(Object value) {
        api = value;
    }
}
