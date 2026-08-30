package cn.huohuas001.bot;

/** Test fixture matching the private Kotlin lateinit field used by HuHoBot QClient. */
public final class QClient {
    private static Object starter;

    private QClient() {
    }

    public static void setStarter(Object value) {
        starter = value;
    }
}
