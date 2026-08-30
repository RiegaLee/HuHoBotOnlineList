package cn.huohuas001.huhobot.onlinelist.model;

import java.awt.image.BufferedImage;
import java.util.Objects;

/** 从 Bukkit 主线程取得的不可变玩家快照。 */
public final class PlayerSnapshot {
    private final String name;
    private final String uuid;
    private final String skinUrl;
    private final BufferedImage avatarOverride;
    private final boolean administrator;

    public PlayerSnapshot(String name, String uuid, String skinUrl) {
        this(name, uuid, skinUrl, null, false);
    }

    public PlayerSnapshot(String name, String uuid, String skinUrl, BufferedImage avatarOverride) {
        this(name, uuid, skinUrl, avatarOverride, false);
    }

    public PlayerSnapshot(String name, String uuid, String skinUrl, boolean administrator) {
        this(name, uuid, skinUrl, null, administrator);
    }

    public PlayerSnapshot(
        String name,
        String uuid,
        String skinUrl,
        BufferedImage avatarOverride,
        boolean administrator
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.skinUrl = skinUrl;
        this.avatarOverride = avatarOverride;
        this.administrator = administrator;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSkinUrl() {
        return skinUrl;
    }

    public BufferedImage getAvatarOverride() {
        return avatarOverride;
    }

    public boolean isAdministrator() {
        return administrator;
    }
}
