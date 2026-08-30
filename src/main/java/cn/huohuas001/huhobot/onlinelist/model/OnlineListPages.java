package cn.huohuas001.huhobot.onlinelist.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 在线玩家的稳定排序与分页规则。 */
public final class OnlineListPages {
    private OnlineListPages() {
    }

    public static Comparator<PlayerSnapshot> comparator(boolean administratorsFirst) {
        Comparator<PlayerSnapshot> names = Comparator
            .comparing(PlayerSnapshot::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(PlayerSnapshot::getName)
            .thenComparing(PlayerSnapshot::getUuid);
        if (!administratorsFirst) return names;
        return Comparator
            .comparingInt((PlayerSnapshot player) -> player.isAdministrator() ? 0 : 1)
            .thenComparing(names);
    }

    public static List<PlayerSnapshot> sorted(List<PlayerSnapshot> players, boolean administratorsFirst) {
        List<PlayerSnapshot> result = new ArrayList<PlayerSnapshot>(players);
        result.sort(comparator(administratorsFirst));
        return result;
    }

    public static int totalPages(int playerCount, int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        return Math.max(1, (Math.max(0, playerCount) + safePageSize - 1) / safePageSize);
    }

    public static ServerSnapshot page(ServerSnapshot complete, int requestedPage, int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        int totalOnline = complete.getPlayers().size();
        int pages = totalPages(totalOnline, safePageSize);
        if (requestedPage < 1 || requestedPage > pages) {
            throw new IllegalArgumentException("page " + requestedPage + " is outside 1.." + pages);
        }
        int from = Math.min((requestedPage - 1) * safePageSize, totalOnline);
        int to = Math.min(from + safePageSize, totalOnline);
        return new ServerSnapshot(
            complete.getServerName(),
            complete.getMaxPlayers(),
            totalOnline,
            complete.getPlayers().subList(from, to),
            complete.getCapturedAt(),
            requestedPage,
            pages
        );
    }
}
