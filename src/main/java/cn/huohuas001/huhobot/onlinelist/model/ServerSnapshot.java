package cn.huohuas001.huhobot.onlinelist.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 一次渲染使用的服务器在线状态快照。 */
public final class ServerSnapshot {
    private final String serverName;
    private final int maxPlayers;
    private final int totalOnlinePlayers;
    private final List<PlayerSnapshot> players;
    private final Instant capturedAt;
    private final int currentPage;
    private final int totalPages;

    public ServerSnapshot(String serverName, int maxPlayers, List<PlayerSnapshot> players, Instant capturedAt) {
        this(serverName, maxPlayers, players.size(), players, capturedAt, 1, 1);
    }

    public ServerSnapshot(
        String serverName,
        int maxPlayers,
        int totalOnlinePlayers,
        List<PlayerSnapshot> players,
        Instant capturedAt,
        int currentPage,
        int totalPages
    ) {
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.maxPlayers = Math.max(0, maxPlayers);
        this.players = Collections.unmodifiableList(new ArrayList<PlayerSnapshot>(players));
        this.totalOnlinePlayers = Math.max(this.players.size(), totalOnlinePlayers);
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
        this.totalPages = Math.max(1, totalPages);
        this.currentPage = Math.max(1, Math.min(currentPage, this.totalPages));
    }

    public String getServerName() {
        return serverName;
    }

    public int getOnlinePlayers() {
        return totalOnlinePlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public List<PlayerSnapshot> getPlayers() {
        return players;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
