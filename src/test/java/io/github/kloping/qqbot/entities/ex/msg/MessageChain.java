package io.github.kloping.qqbot.entities.ex.msg;

/** Test fixture for HuHoBot's QQ SDK byte-image MessageChain. */
public final class MessageChain {
    private String text;
    private byte[] image;

    public MessageChain text(String value) {
        text = value;
        return this;
    }

    public MessageChain image(byte[] value) {
        image = value;
        return this;
    }

    public String getText() {
        return text;
    }

    public byte[] getImage() {
        return image;
    }
}
