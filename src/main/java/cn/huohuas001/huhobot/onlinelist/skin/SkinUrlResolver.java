package cn.huohuas001.huhobot.onlinelist.skin;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;

/** 在 Bukkit 当前 Profile 没有可用皮肤时提供后备皮肤地址。 */
public interface SkinUrlResolver {
    String resolve(PlayerSnapshot player);
}
