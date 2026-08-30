package cn.huohuas001.huhobot.onlinelist.api;

/**
 * 附属插件自带的图片字节回复 API。
 *
 * <p>该服务会注册到 Bukkit ServicesManager。其他附属可以通过
 * {@code Bukkit.getServicesManager().load(ImageReplyApi.class)} 获取实例，
 * 不需要修改 HuHoBot 主插件。</p>
 */
public interface ImageReplyApi {
    ImageReplyResult reply(ImageReplyRequest request);
}
