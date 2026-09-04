package cn.huohuas001.huhobot.onlinelist.render;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;
import cn.huohuas001.huhobot.onlinelist.skin.AvatarCache;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 使用 1200px 雾蓝玻璃三段底图渲染可向下无限扩展的在线列表。 */
public final class OnlineListRenderer {
    public static final int WIDTH = 1200;
    public static final int HEADER_HEIGHT = 384;
    public static final int BODY_TILE_HEIGHT = 640;
    public static final int FOOTER_HEIGHT = 256;

    private static final int GRID_TOP = 520;

    private static final Color TEXT_PRIMARY = new Color(25, 69, 96);
    private static final Color TEXT_SECONDARY = new Color(33, 88, 116, 190);
    private static final Color CARD_FILL = new Color(232, 248, 255, 128);
    private static final Color CARD_BORDER = new Color(220, 252, 255, 188);
    private static final Color CARD_SHADOW = new Color(18, 65, 98, 35);
    private static final Color CYAN = new Color(57, 218, 236);
    private static final Color ADMIN_TEXT = new Color(15, 139, 166, 220);
    private static final int[][] DEFAULT_STEVE_FACE = {
        {0xFF332411, 0xFF332411, 0xFF3F2A15, 0xFF3F2A15, 0xFF3F2A15, 0xFF3F2A15, 0xFF332411, 0xFF2B1E0D},
        {0xFF241808, 0xFF332411, 0xFF332411, 0xFF3F2A15, 0xFF3F2A15, 0xFF332411, 0xFF3F2A15, 0xFF332411},
        {0xFF2B1E0D, 0xFF9B6349, 0xFFB3795E, 0xFFB7836B, 0xFFB3795E, 0xFFAA7259, 0xFF9B6349, 0xFF342512},
        {0xFF9B6349, 0xFFAA7259, 0xFFB3795E, 0xFFB3795E, 0xFFAA7259, 0xFFAA7259, 0xFFAA7259, 0xFF9B6349},
        {0xFFAA7259, 0xFFFFFFFF, 0xFF523D89, 0xFFAA7259, 0xFF9B6349, 0xFF523D89, 0xFFFFFFFF, 0xFFAA7259},
        {0xFF9B6349, 0xFFAA7259, 0xFFAA7259, 0xFF6A4030, 0xFF6A4030, 0xFFAA7259, 0xFFAA7259, 0xFF9B6349},
        {0xFF90593F, 0xFF8F5E3E, 0xFF492510, 0xFF774235, 0xFF774235, 0xFF421D0A, 0xFF8F5E3E, 0xFF815339},
        {0xFF94603E, 0xFF815339, 0xFF421D0A, 0xFF492510, 0xFF421D0A, 0xFF492510, 0xFF815339, 0xFF8F5E3E}
    };
    private static final BufferedImage DEFAULT_STEVE_HEAD = createDefaultSteveHead();

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.of("Asia/Shanghai"));

    private final BufferedImage header;
    private final BufferedImage bodyTile;
    private final BufferedImage footer;
    private final AvatarCache avatarCache;
    private final int columns;
    private final int sideMargin;
    private final int cardGapX;
    private final int cardGapY;
    private final int cardHeight;
    private final int avatarSize;
    private final int contentBottomSafe;
    private final String fontFamily;
    private final String footerText;
    private final boolean useInitialFallback;

    public OnlineListRenderer(AvatarCache avatarCache, int columns, String configuredFont, String footerText) throws IOException {
        this(avatarCache, columns, configuredFont, footerText, "steve");
    }

    public OnlineListRenderer(
        AvatarCache avatarCache,
        int columns,
        String configuredFont,
        String footerText,
        String fallbackAvatar
    ) throws IOException {
        this(
            readResource("/online/huhobot-glass-header-1200x384.png"),
            readResource("/online/huhobot-glass-body-tile-1200x640.png"),
            readResource("/online/huhobot-glass-footer-1200x256.png"),
            avatarCache,
            columns,
            configuredFont,
            footerText,
            fallbackAvatar
        );
    }

    OnlineListRenderer(
        BufferedImage header,
        BufferedImage bodyTile,
        BufferedImage footer,
        AvatarCache avatarCache,
        int columns,
        String configuredFont,
        String footerText
    ) {
        this(header, bodyTile, footer, avatarCache, columns, configuredFont, footerText, "steve");
    }

    OnlineListRenderer(
        BufferedImage header,
        BufferedImage bodyTile,
        BufferedImage footer,
        AvatarCache avatarCache,
        int columns,
        String configuredFont,
        String footerText,
        String fallbackAvatar
    ) {
        validateSlice(header, WIDTH, HEADER_HEIGHT, "header");
        validateSlice(bodyTile, WIDTH, BODY_TILE_HEIGHT, "body tile");
        validateSlice(footer, WIDTH, FOOTER_HEIGHT, "footer");
        this.header = header;
        this.bodyTile = bodyTile;
        this.footer = footer;
        this.avatarCache = avatarCache;
        this.columns = Math.max(1, Math.min(3, columns));
        boolean compact = this.columns == 3;
        this.sideMargin = compact ? 92 : 104;
        this.cardGapX = compact ? 18 : 28;
        this.cardGapY = compact ? 16 : 22;
        this.cardHeight = compact ? 96 : 118;
        this.avatarSize = compact ? 60 : 76;
        this.contentBottomSafe = compact ? 60 : 86;
        this.fontFamily = chooseFont(configuredFont);
        this.footerText = footerText == null ? "" : footerText.trim();
        this.useInitialFallback = "initial".equalsIgnoreCase(fallbackAvatar == null ? "" : fallbackAvatar.trim());
    }

    public byte[] render(ServerSnapshot snapshot) throws IOException {
        Map<String, BufferedImage> avatars = avatarCache.loadAll(snapshot.getPlayers());
        BufferedImage image = renderImage(snapshot, avatars);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) throw new IOException("PNG writer is unavailable");
            return output.toByteArray();
        }
    }

    BufferedImage renderImage(ServerSnapshot snapshot, Map<String, BufferedImage> avatars) {
        int displayedPlayers = snapshot.getPlayers().size();
        int rows = displayedPlayers == 0 ? 0 : (displayedPlayers + columns - 1) / columns;
        int gridHeight = rows == 0 ? 0 : rows * cardHeight + (rows - 1) * cardGapY;
        int contentBottom = rows == 0 ? GRID_TOP + 210 : GRID_TOP + gridHeight;
        int bodyTiles = Math.max(1, divideRoundUp(contentBottom + contentBottomSafe - HEADER_HEIGHT, BODY_TILE_HEIGHT));
        if (snapshot.getTotalPages() > 1) bodyTiles = Math.max(2, bodyTiles);
        int footerTop = HEADER_HEIGHT + bodyTiles * BODY_TILE_HEIGHT;
        int height = footerTop + FOOTER_HEIGHT;

        BufferedImage result = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            configure(graphics);
            drawBackground(graphics, bodyTiles, footerTop);
            drawHeader(graphics, snapshot);
            if (rows == 0) {
                drawEmptyState(graphics);
            } else {
                drawPlayers(graphics, snapshot, avatars);
            }
            drawFooter(graphics, footerTop, snapshot);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private void drawBackground(Graphics2D graphics, int bodyTiles, int footerTop) {
        graphics.drawImage(header, 0, 0, null);
        for (int i = 0; i < bodyTiles; i++) {
            graphics.drawImage(bodyTile, 0, HEADER_HEIGHT + i * BODY_TILE_HEIGHT, null);
        }
        graphics.drawImage(footer, 0, footerTop, null);
    }

    private void drawHeader(Graphics2D graphics, ServerSnapshot snapshot) {
        graphics.setFont(font(Font.BOLD, 58));
        graphics.setColor(TEXT_PRIMARY);
        String title = "在线列表";
        graphics.drawString(title, (WIDTH - graphics.getFontMetrics().stringWidth(title)) / 2, 280);

        graphics.setFont(font(Font.BOLD, 32));
        graphics.setColor(TEXT_PRIMARY);
        graphics.drawString(snapshot.getServerName(), sideMargin, 446);

        String count = snapshot.getOnlinePlayers() + " / " + snapshot.getMaxPlayers();
        graphics.setFont(font(Font.BOLD, 30));
        drawRight(graphics, count, WIDTH - sideMargin, 446, TEXT_PRIMARY);

        graphics.setFont(font(Font.PLAIN, 22));
        graphics.setColor(TEXT_SECONDARY);
        graphics.drawString("当前在线玩家", sideMargin, 486);
        drawRight(graphics, "更新 " + TIME.format(snapshot.getCapturedAt()), WIDTH - sideMargin, 486, TEXT_SECONDARY);
    }

    private void drawPlayers(
        Graphics2D graphics,
        ServerSnapshot snapshot,
        Map<String, BufferedImage> avatars
    ) {
        int usableWidth = WIDTH - sideMargin * 2;
        int cardWidth = (usableWidth - (columns - 1) * cardGapX) / columns;
        for (int index = 0; index < snapshot.getPlayers().size(); index++) {
            PlayerSnapshot player = snapshot.getPlayers().get(index);
            int column = index % columns;
            int row = index / columns;
            int x = sideMargin + column * (cardWidth + cardGapX);
            int y = GRID_TOP + row * (cardHeight + cardGapY);
            drawPlayerCard(graphics, x, y, cardWidth, player, avatars.get(player.getUuid()));
        }
    }

    private void drawPlayerCard(
        Graphics2D graphics,
        int x,
        int y,
        int width,
        PlayerSnapshot player,
        BufferedImage avatar
    ) {
        int cardArc = columns == 3 ? 24 : 32;
        RoundRectangle2D card = new RoundRectangle2D.Float(x, y, width, cardHeight, cardArc, cardArc);
        graphics.setColor(CARD_SHADOW);
        graphics.fill(new RoundRectangle2D.Float(x + 2, y + (columns == 3 ? 4 : 6), width, cardHeight, cardArc, cardArc));
        graphics.setColor(CARD_FILL);
        graphics.fill(card);
        graphics.setStroke(new BasicStroke(1.7f));
        graphics.setColor(CARD_BORDER);
        graphics.draw(card);

        int avatarX = x + (columns == 3 ? 16 : 21);
        int avatarY = y + (cardHeight - avatarSize) / 2;
        int avatarArc = columns == 3 ? 16 : 20;
        RoundRectangle2D avatarShape = new RoundRectangle2D.Float(avatarX, avatarY, avatarSize, avatarSize, avatarArc, avatarArc);
        Shape previousClip = graphics.getClip();
        graphics.clip(avatarShape);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (avatar == null) {
            drawFallbackAvatar(graphics, avatarX, avatarY, player);
        } else {
            graphics.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize, null);
        }
        graphics.setClip(previousClip);
        graphics.setColor(new Color(219, 250, 255, 210));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(avatarShape);

        graphics.setColor(CYAN);
        int dotSize = columns == 3 ? 10 : 13;
        graphics.fillOval(avatarX + avatarSize - dotSize, avatarY + avatarSize - dotSize, dotSize, dotSize);

        int textX = avatarX + avatarSize + (columns == 3 ? 13 : 19);
        graphics.setFont(font(Font.BOLD, columns == 3 ? 22 : 28));
        graphics.setColor(TEXT_PRIMARY);
        String name = truncate(graphics, player.getName(), x + width - textX - 18);
        graphics.drawString(name, textX, y + (columns == 3 ? 50 : 69));

        graphics.setFont(font(Font.PLAIN, columns == 3 ? 15 : 18));
        graphics.setColor(player.isAdministrator() ? ADMIN_TEXT : TEXT_SECONDARY);
        graphics.drawString(player.isAdministrator() ? "ADMIN" : "ONLINE", textX, y + (columns == 3 ? 73 : 94));
    }

    private void drawFallbackAvatar(Graphics2D graphics, int x, int y, PlayerSnapshot player) {
        if (!useInitialFallback) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(DEFAULT_STEVE_HEAD, x, y, x + avatarSize, y + avatarSize, 0, 0, 8, 8, null);
            return;
        }
        int hue = Math.abs(player.getUuid().hashCode()) % 360;
        Color base = Color.getHSBColor(hue / 360f, 0.22f, 0.86f);
        graphics.setColor(base);
        graphics.fillRect(x, y, avatarSize, avatarSize);
        graphics.setColor(new Color(255, 255, 255, 210));
        graphics.setFont(font(Font.BOLD, columns == 3 ? 27 : 34));
        String initial = player.getName().isEmpty() ? "?" : player.getName().substring(0, 1).toUpperCase();
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(initial, x + (avatarSize - metrics.stringWidth(initial)) / 2, y + (columns == 3 ? 40 : 50));
    }

    static BufferedImage createDefaultSteveHead() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < DEFAULT_STEVE_FACE.length; y++) {
            for (int x = 0; x < DEFAULT_STEVE_FACE[y].length; x++) {
                image.setRGB(x, y, DEFAULT_STEVE_FACE[y][x]);
            }
        }
        return image;
    }

    private void drawEmptyState(Graphics2D graphics) {
        int x = sideMargin;
        int y = GRID_TOP + 25;
        int width = WIDTH - sideMargin * 2;
        int height = 160;
        RoundRectangle2D card = new RoundRectangle2D.Float(x, y, width, height, 36, 36);
        graphics.setColor(CARD_FILL);
        graphics.fill(card);
        graphics.setColor(CARD_BORDER);
        graphics.setStroke(new BasicStroke(1.7f));
        graphics.draw(card);

        graphics.setFont(font(Font.BOLD, 30));
        String title = "服务器里静悄悄的";
        drawCentered(graphics, title, y + 70, TEXT_PRIMARY);
        graphics.setFont(font(Font.PLAIN, 22));
        drawCentered(graphics, "当前没有玩家在线", y + 112, TEXT_SECONDARY);
    }

    private void drawFooter(Graphics2D graphics, int footerTop, ServerSnapshot snapshot) {
        if (!footerText.isEmpty()) {
            graphics.setFont(font(Font.PLAIN, 18));
            drawCentered(graphics, footerText, footerTop + 73, new Color(226, 249, 255, 180));
        }

        // 用同一玻璃胶囊内的无装饰纹理覆盖原有四个方块，再写入动态页码。
        graphics.drawImage(
            footer,
            525,
            footerTop + 151,
            675,
            footerTop + 202,
            500,
            151,
            501,
            202,
            null
        );
        graphics.setFont(font(Font.BOLD, 23));
        drawCentered(
            graphics,
            "第 " + snapshot.getCurrentPage() + " / " + snapshot.getTotalPages() + " 页",
            footerTop + 187,
            TEXT_PRIMARY
        );
    }

    private Font font(int style, int size) {
        return new Font(fontFamily, style, size);
    }

    private static String truncate(Graphics2D graphics, String text, int maxWidth) {
        FontMetrics metrics = graphics.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 1 && metrics.stringWidth(text.substring(0, end) + "…") > maxWidth) end--;
        return text.substring(0, end) + "…";
    }

    private static void drawRight(Graphics2D graphics, String text, int right, int baseline, Color color) {
        graphics.setColor(color);
        graphics.drawString(text, right - graphics.getFontMetrics().stringWidth(text), baseline);
    }

    private static void drawCentered(Graphics2D graphics, String text, int baseline, Color color) {
        graphics.setColor(color);
        graphics.drawString(text, (WIDTH - graphics.getFontMetrics().stringWidth(text)) / 2, baseline);
    }

    private static int divideRoundUp(int value, int divisor) {
        return Math.max(0, (value + divisor - 1) / divisor);
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static String chooseFont(String configured) {
        Set<String> available = new HashSet<String>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        if (configured != null && !configured.trim().isEmpty() && available.contains(configured.trim())) {
            return configured.trim();
        }
        String[] candidates = {"Microsoft YaHei", "Noto Sans CJK SC", "Source Han Sans SC", "WenQuanYi Micro Hei", "SansSerif"};
        for (String candidate : candidates) if (available.contains(candidate)) return candidate;
        return Font.SANS_SERIF;
    }

    private static BufferedImage readResource(String path) throws IOException {
        try (InputStream input = OnlineListRenderer.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing image resource: " + path);
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IOException("Unreadable image resource: " + path);
            return image;
        }
    }

    private static void validateSlice(BufferedImage image, int width, int height, String name) {
        if (image.getWidth() != width || image.getHeight() != height) {
            throw new IllegalArgumentException(
                name + " must be " + width + "x" + height + ", got " + image.getWidth() + "x" + image.getHeight()
            );
        }
    }
}
