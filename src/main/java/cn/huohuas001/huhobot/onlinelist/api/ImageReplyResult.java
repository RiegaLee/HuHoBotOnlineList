package cn.huohuas001.huhobot.onlinelist.api;

/** 图片回复结果；失败时保留适配器和简短原因，便于定位不同 HuHoBot 分支。 */
public final class ImageReplyResult {
    private final boolean success;
    private final String transport;
    private final String message;
    private final Throwable cause;

    private ImageReplyResult(boolean success, String transport, String message, Throwable cause) {
        this.success = success;
        this.transport = transport;
        this.message = message;
        this.cause = cause;
    }

    public static ImageReplyResult success(String transport) {
        return new ImageReplyResult(true, transport, "ok", null);
    }

    public static ImageReplyResult failure(String transport, String message, Throwable cause) {
        return new ImageReplyResult(false, transport, message == null ? "unknown error" : message, cause);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransport() {
        return transport;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}
