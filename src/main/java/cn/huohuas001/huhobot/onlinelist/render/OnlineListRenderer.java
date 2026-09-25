package cn.huohuas001.huhobot.onlinelist.render;

import cn.huohuas001.huhobot.onlinelist.model.PlayerSnapshot;
import cn.huohuas001.huhobot.onlinelist.model.ServerSnapshot;
import cn.huohuas001.huhobot.onlinelist.skin.AvatarCache;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Renders the single transparent-material online-list UI over a replaceable wallpaper. */
public final class OnlineListRenderer {
    public static final int WIDTH = 1200;
    private static final int CUSTOM_SIDE_MARGIN = 64;
    private static final int CUSTOM_PANEL_TOP = 220;
    private static final int CUSTOM_PANEL_HEADER = 104;
    private static final int CUSTOM_PANEL_BOTTOM = 20;
    private static final int CUSTOM_CARD_GAP = 14;
    private static final double DEFAULT_CUSTOM_SURFACE_OPACITY = 0.12;

    private static final Color CUSTOM_TEXT_PRIMARY = new Color(250, 252, 255, 242);
    private static final Color CUSTOM_TEXT_SECONDARY = new Color(235, 241, 248, 182);
    private static final Color CUSTOM_ACCENT = new Color(198, 245, 250, 238);
    /** Pixel-for-pixel front face from Minecraft's classic 64x64 Steve skin (8..15, 8..15). */
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
    private static final Font ONLINE_TITLE_FONT = loadOnlineTitleFont();

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.of("Asia/Shanghai"));

    private final OnlineListBackground customBackground;
    private final AvatarCache avatarCache;
    private final int columns;
    private final int cardHeight;
    private final int avatarSize;
    private final String customFontFamily;
    private final String footerText;
    private final boolean useInitialFallback;
    private final double customSurfaceOpacity;

    public OnlineListRenderer(AvatarCache avatarCache, int columns, String configuredFont, String footerText) throws IOException {
        this(avatarCache, columns, configuredFont, footerText, "steve");
    }

    public OnlineListRenderer(
        AvatarCache avatarCache,
        int columns,
        String configuredFont,
        String footerText,
        String fallbackAvatar
    ) {
        this(
            avatarCache,
            columns,
            configuredFont,
            footerText,
            fallbackAvatar,
            OnlineListBackground.defaultBackground(),
            DEFAULT_CUSTOM_SURFACE_OPACITY
        );
    }

    public OnlineListRenderer(
        AvatarCache avatarCache,
        int columns,
        String configuredFont,
        String footerText,
        String fallbackAvatar,
        Path customBackgroundPath,
        String customBackgroundFit
    ) throws IOException {
        this(
            avatarCache,
            columns,
            configuredFont,
            footerText,
            fallbackAvatar,
            OnlineListBackground.load(customBackgroundPath, customBackgroundFit),
            DEFAULT_CUSTOM_SURFACE_OPACITY
        );
    }

    public OnlineListRenderer(
        AvatarCache avatarCache,
        int columns,
        String configuredFont,
        String footerText,
        String fallbackAvatar,
        Path customBackgroundPath,
        String customBackgroundFit,
        double customSurfaceOpacity,
        boolean adaptiveBackdropDimming,
        double maximumBackdropDimming
    ) throws IOException {
        this(
            avatarCache,
            columns,
            configuredFont,
            footerText,
            fallbackAvatar,
            OnlineListBackground.load(
                customBackgroundPath,
                customBackgroundFit,
                adaptiveBackdropDimming,
                maximumBackdropDimming
            ),
            customSurfaceOpacity
        );
    }

    private OnlineListRenderer(
        AvatarCache avatarCache,
        int columns,
        String configuredFont,
        String footerText,
        String fallbackAvatar,
        OnlineListBackground customBackground,
        double customSurfaceOpacity
    ) {
        this.customBackground = customBackground;
        this.avatarCache = avatarCache;
        this.columns = Math.max(1, Math.min(3, columns));
        boolean compact = this.columns == 3;
        this.cardHeight = compact ? 96 : 118;
        this.avatarSize = compact ? 60 : 76;
        this.customFontFamily = chooseCustomFont(configuredFont);
        this.footerText = footerText == null ? "" : footerText.trim();
        this.useInitialFallback = "initial".equalsIgnoreCase(fallbackAvatar == null ? "" : fallbackAvatar.trim());
        if (!Double.isFinite(customSurfaceOpacity) || customSurfaceOpacity < 0 || customSurfaceOpacity > 1) {
            throw new IllegalArgumentException("render.custom-background.surface-opacity must be between 0 and 1");
        }
        this.customSurfaceOpacity = customSurfaceOpacity;
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
        int rosterContentHeight = rows == 0 ? 150 : rows * cardHeight;
        int contentBottom = CUSTOM_PANEL_TOP + CUSTOM_PANEL_HEADER + rosterContentHeight + CUSTOM_PANEL_BOTTOM;
        int height = Math.max(900, contentBottom + 180);
        int footerTop = height - 140;

        BufferedImage result = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            configure(graphics);
            customBackground.paint(graphics, WIDTH, height, footerTop);
            drawCustomHeader(graphics, snapshot);
            drawCustomRoster(graphics, snapshot, avatars);
            drawCustomFooter(graphics, footerTop, snapshot);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private void drawCustomHeader(Graphics2D graphics, ServerSnapshot snapshot) {
        int titleX = 420;
        int titleY = 52;
        int titleWidth = 360;
        int titleHeight = 112;
        RoundRectangle2D titlePanel = new RoundRectangle2D.Float(
            titleX, titleY, titleWidth, titleHeight, 56, 56
        );
        drawCustomSurface(graphics, titlePanel);
        graphics.setFont(ONLINE_TITLE_FONT.deriveFont(43f));
        String title = "在线列表";
        drawTextWithShadow(
            graphics,
            title,
            (WIDTH - graphics.getFontMetrics().stringWidth(title)) / 2,
            124,
            CUSTOM_TEXT_PRIMARY
        );
    }

    private void drawCustomRoster(
        Graphics2D graphics,
        ServerSnapshot snapshot,
        Map<String, BufferedImage> avatars
    ) {
        int rows = (snapshot.getPlayers().size() + columns - 1) / columns;
        int panelX = CUSTOM_SIDE_MARGIN;
        int panelY = CUSTOM_PANEL_TOP;
        int panelWidth = WIDTH - CUSTOM_SIDE_MARGIN * 2;
        int rosterContentHeight = rows == 0 ? 150 : rows * cardHeight;
        int panelHeight = CUSTOM_PANEL_HEADER + rosterContentHeight + CUSTOM_PANEL_BOTTOM;
        RoundRectangle2D panel = new RoundRectangle2D.Float(panelX, panelY, panelWidth, panelHeight, 44, 44);

        drawCustomSurface(graphics, panel);

        int headerX = panelX + 26;
        graphics.setFont(customFont(Font.BOLD, 29));
        drawTextWithShadow(graphics, snapshot.getServerName(), headerX, panelY + 48, CUSTOM_TEXT_PRIMARY);
        graphics.setFont(customFont(Font.PLAIN, 17));
        drawTextWithShadow(
            graphics,
            "更新 " + TIME.format(snapshot.getCapturedAt()),
            headerX,
            panelY + 79,
            CUSTOM_TEXT_SECONDARY
        );

        String count = snapshot.getOnlinePlayers() + "/" + snapshot.getMaxPlayers();
        graphics.setFont(customFont(Font.BOLD, 19));
        int countWidth = graphics.getFontMetrics().stringWidth(count) + 36;
        int countX = panelX + panelWidth - countWidth - 24;
        drawCustomSurface(
            graphics,
            new RoundRectangle2D.Float(countX, panelY + 26, countWidth, 50, 25, 25)
        );
        drawCenteredInRectWithShadow(
            graphics,
            count,
            countX,
            countWidth,
            panelY + 58,
            CUSTOM_TEXT_PRIMARY
        );

        int innerX = panelX + 18;
        int innerWidth = panelWidth - 36;
        int cardWidth = (innerWidth - (columns - 1) * CUSTOM_CARD_GAP) / columns;
        int cardsTop = panelY + CUSTOM_PANEL_HEADER;

        if (snapshot.getPlayers().isEmpty()) {
            RoundRectangle2D emptyCard = new RoundRectangle2D.Float(
                innerX,
                cardsTop,
                innerWidth,
                132,
                28,
                28
            );
            drawCustomSurface(graphics, emptyCard);
            graphics.setFont(customFont(Font.BOLD, 27));
            drawCenteredWithShadow(graphics, "服务器里静悄悄的", cardsTop + 57, CUSTOM_TEXT_PRIMARY);
            graphics.setFont(customFont(Font.PLAIN, 18));
            drawCenteredWithShadow(graphics, "当前没有玩家在线", cardsTop + 94, CUSTOM_TEXT_SECONDARY);
            return;
        }

        for (int index = 0; index < snapshot.getPlayers().size(); index++) {
            PlayerSnapshot player = snapshot.getPlayers().get(index);
            int column = index % columns;
            int row = index / columns;
            int x = innerX + column * (cardWidth + CUSTOM_CARD_GAP);
            int y = cardsTop + row * cardHeight;
            drawCustomPlayerCard(graphics, x, y, cardWidth, player, avatars.get(player.getUuid()));
        }
    }

    private void drawCustomPlayerCard(
        Graphics2D graphics,
        int x,
        int y,
        int width,
        PlayerSnapshot player,
        BufferedImage avatar
    ) {
        int height = cardHeight - CUSTOM_CARD_GAP;
        int arc = 26;
        drawCustomSurface(graphics, new RoundRectangle2D.Float(x, y, width, height, arc, arc));

        int avatarX = x + 14;
        int avatarY = y + (height - avatarSize) / 2;
        drawPlayerAvatar(graphics, avatarX, avatarY, player, avatar);

        int textX = avatarX + avatarSize + 14;
        graphics.setFont(customFont(Font.BOLD, 20));
        String name = truncate(graphics, player.getName(), x + width - textX - 18);
        drawTextWithShadow(graphics, name, textX, y + 43, CUSTOM_TEXT_PRIMARY);
        if (player.isAdministrator()) {
            graphics.setFont(customFont(Font.PLAIN, 13));
            drawTextWithShadow(graphics, "管理员", textX, y + 64, CUSTOM_ACCENT);
        }
    }

    private void drawPlayerAvatar(
        Graphics2D graphics,
        int x,
        int y,
        PlayerSnapshot player,
        BufferedImage avatar
    ) {
        int inset = columns == 3 ? 4 : 5;
        int imageSize = avatarSize - inset * 2;
        int imageX = x + inset;
        int imageY = y + inset;
        int frameArc = columns == 3 ? 14 : 18;
        RoundRectangle2D frame = new RoundRectangle2D.Float(x, y, avatarSize, avatarSize, frameArc, frameArc);

        graphics.setColor(new Color(255, 255, 255, 20));
        graphics.fill(frame);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (avatar == null) {
            drawFallbackAvatar(graphics, imageX, imageY, imageSize, player);
        } else {
            // Minecraft heads are pixel art. Keep every corner pixel instead of clipping the face into a circle.
            graphics.drawImage(avatar, imageX, imageY, imageSize, imageSize, null);
        }

        graphics.setStroke(new BasicStroke(1.2f));
        graphics.setColor(new Color(255, 255, 255, 92));
        graphics.draw(frame);

    }

    private void drawFallbackAvatar(Graphics2D graphics, int x, int y, int size, PlayerSnapshot player) {
        if (!useInitialFallback) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(DEFAULT_STEVE_HEAD, x, y, x + size, y + size, 0, 0, 8, 8, null);
            return;
        }
        int hue = Math.abs(player.getUuid().hashCode()) % 360;
        Color base = Color.getHSBColor(hue / 360f, 0.22f, 0.86f);
        graphics.setColor(base);
        graphics.fillRect(x, y, size, size);
        graphics.setColor(new Color(255, 255, 255, 210));
        graphics.setFont(customFont(Font.BOLD, columns == 3 ? 27 : 34));
        String initial = player.getName().isEmpty() ? "?" : player.getName().substring(0, 1).toUpperCase();
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(
            initial,
            x + (size - metrics.stringWidth(initial)) / 2,
            y + (size - metrics.getHeight()) / 2 + metrics.getAscent()
        );
    }

    /** Returns a fresh copy of Minecraft's classic Steve face for offline previews and fallback rendering. */
    public static BufferedImage createDefaultSteveHead() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < DEFAULT_STEVE_FACE.length; y++) {
            for (int x = 0; x < DEFAULT_STEVE_FACE[y].length; x++) {
                image.setRGB(x, y, DEFAULT_STEVE_FACE[y][x]);
            }
        }
        return image;
    }

    private void drawCustomFooter(Graphics2D graphics, int footerTop, ServerSnapshot snapshot) {
        if (!footerText.isEmpty()) {
            graphics.setFont(customFont(Font.PLAIN, 15));
            drawCenteredWithShadow(graphics, footerText, footerTop + 27, new Color(255, 255, 255, 180));
        }
        int pillWidth = 176;
        int pillHeight = 48;
        int pillX = (WIDTH - pillWidth) / 2;
        int pillY = footerTop + 51;
        drawCustomSurface(
            graphics,
            new RoundRectangle2D.Float(pillX, pillY, pillWidth, pillHeight, 24, 24)
        );
        graphics.setFont(customFont(Font.PLAIN, 18));
        drawCenteredWithShadow(
            graphics,
            snapshot.getCurrentPage() + " / " + snapshot.getTotalPages(),
            pillY + 31,
            CUSTOM_TEXT_PRIMARY
        );
    }

    private Font customFont(int style, int size) {
        return new Font(customFontFamily, style, size);
    }

    private void drawCustomSurface(Graphics2D graphics, java.awt.Shape shape) {
        Rectangle bounds = shape.getBounds();
        int topAlpha = (int) Math.round(255 * Math.min(1, customSurfaceOpacity * 1.15));
        int bottomAlpha = (int) Math.round(255 * Math.min(1, customSurfaceOpacity * 0.65));
        graphics.setPaint(new GradientPaint(
            0,
            bounds.y,
            new Color(244, 247, 249, topAlpha),
            0,
            bounds.y + bounds.height,
            new Color(214, 221, 226, bottomAlpha)
        ));
        graphics.fill(shape);
        if (customSurfaceOpacity > 0) {
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.setColor(new Color(255, 255, 255, 58));
            graphics.draw(shape);
        }
    }

    private static void drawTextWithShadow(
        Graphics2D graphics,
        String text,
        int x,
        int baseline,
        Color color
    ) {
        graphics.setColor(new Color(0, 0, 0, 92));
        graphics.drawString(text, x + 1, baseline + 2);
        graphics.setColor(color);
        graphics.drawString(text, x, baseline);
    }

    private static void drawCenteredWithShadow(
        Graphics2D graphics,
        String text,
        int baseline,
        Color color
    ) {
        drawTextWithShadow(
            graphics,
            text,
            (WIDTH - graphics.getFontMetrics().stringWidth(text)) / 2,
            baseline,
            color
        );
    }

    private static void drawCenteredInRectWithShadow(
        Graphics2D graphics,
        String text,
        int x,
        int width,
        int baseline,
        Color color
    ) {
        drawTextWithShadow(
            graphics,
            text,
            x + (width - graphics.getFontMetrics().stringWidth(text)) / 2,
            baseline,
            color
        );
    }

    static Font loadOnlineTitleFont() {
        try (InputStream input = OnlineListRenderer.class.getResourceAsStream(
            "/fonts/HuHoBotOnlineTitle-Semibold.ttf"
        )) {
            if (input == null) throw new IOException("Missing embedded online-list title font");
            Font font = Font.createFont(Font.TRUETYPE_FONT, input);
            if (font.canDisplayUpTo("在线列表") != -1) {
                throw new IOException("Embedded online-list title font is missing required glyphs");
            }
            return font;
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException("Unable to load embedded online-list title font", exception);
        }
    }

    private static String truncate(Graphics2D graphics, String text, int maxWidth) {
        FontMetrics metrics = graphics.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 1 && metrics.stringWidth(text.substring(0, end) + "…") > maxWidth) end--;
        return text.substring(0, end) + "…";
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static String chooseCustomFont(String configured) {
        Set<String> available = new HashSet<String>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        if (configured != null && !configured.trim().isEmpty() && available.contains(configured.trim())) {
            return configured.trim();
        }
        String[] candidates = {
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "Noto Sans SC",
            "Noto Sans CJK SC",
            "Source Han Sans SC",
            "SansSerif"
        };
        for (String candidate : candidates) if (available.contains(candidate)) return candidate;
        return Font.SANS_SERIF;
    }

}
