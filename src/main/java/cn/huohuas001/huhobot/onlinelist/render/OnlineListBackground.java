package cn.huohuas001.huhobot.onlinelist.render;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/** Validates and paints a user-owned wallpaper below the stable OnlineList UI layer. */
final class OnlineListBackground {
    private static final long MAX_FILE_BYTES = 16L * 1024L * 1024L;
    private static final long MAX_PIXELS = 32L * 1024L * 1024L;
    private static final double TARGET_BACKDROP_LUMINANCE = 0.56;
    private static final double MINIMUM_ADAPTIVE_DIMMING = 0.06;

    private final BufferedImage image;
    private final String fit;
    private final boolean adaptiveBackdropDimming;
    private final double maximumBackdropDimming;
    private int cachedWidth;
    private int cachedHeight;
    private BufferedImage cachedWallpaper;
    private int cachedDimmingAlpha;

    private OnlineListBackground(
        BufferedImage image,
        String fit,
        boolean adaptiveBackdropDimming,
        double maximumBackdropDimming
    ) {
        this.image = Objects.requireNonNull(image, "image");
        this.fit = normalizeFit(fit);
        if (!Double.isFinite(maximumBackdropDimming)
            || maximumBackdropDimming < 0
            || maximumBackdropDimming > 1) {
            throw new IllegalArgumentException("render.custom-background.maximum-backdrop-dimming must be between 0 and 1");
        }
        this.adaptiveBackdropDimming = adaptiveBackdropDimming;
        this.maximumBackdropDimming = maximumBackdropDimming;
    }

    static OnlineListBackground load(Path path, String fit) throws IOException {
        return load(path, fit, true, 0.38);
    }

    static OnlineListBackground load(
        Path path,
        String fit,
        boolean adaptiveBackdropDimming,
        double maximumBackdropDimming
    ) throws IOException {
        Path normalized = Objects.requireNonNull(path, "custom background")
            .toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("Missing custom background: " + normalized);
        }
        if (Files.size(normalized) > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Custom background exceeds 16 MiB: " + normalized);
        }
        BufferedImage image = ImageIO.read(normalized.toFile());
        if (image == null) {
            throw new IllegalArgumentException("Unreadable custom background: " + normalized);
        }
        long pixels = (long) image.getWidth() * (long) image.getHeight();
        if (image.getWidth() < 1 || image.getHeight() < 1 || pixels > MAX_PIXELS) {
            throw new IllegalArgumentException("Custom background has unsafe dimensions: " + normalized);
        }
        return new OnlineListBackground(image, fit, adaptiveBackdropDimming, maximumBackdropDimming);
    }

    static OnlineListBackground of(BufferedImage image, String fit) {
        return new OnlineListBackground(image, fit, true, 0.38);
    }

    static OnlineListBackground defaultBackground() {
        BufferedImage image = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setPaint(new GradientPaint(
                0,
                0,
                new Color(22, 31, 47),
                1200,
                900,
                new Color(48, 68, 91)
            ));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setPaint(new GradientPaint(
                0,
                220,
                new Color(93, 123, 154, 100),
                0,
                760,
                new Color(20, 27, 40, 12)
            ));
            graphics.fillOval(180, 110, 840, 720);
        } finally {
            graphics.dispose();
        }
        return new OnlineListBackground(image, "cover", true, 0.38);
    }

    void paint(Graphics2D graphics, int width, int height, int footerTop) {
        ensureRendered(width, height);
        Shape previousClip = graphics.getClip();
        graphics.clip(new RoundRectangle2D.Float(6, 6, width - 12, height - 12, 48, 48));
        Object previousInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
        graphics.drawImage(cachedWallpaper, 0, 0, null);
        if (cachedDimmingAlpha > 0) {
            graphics.setColor(new Color(20, 22, 26, cachedDimmingAlpha));
            graphics.fillRect(0, 0, width, height);
        }
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            previousInterpolation == null
                ? RenderingHints.VALUE_INTERPOLATION_BICUBIC
                : previousInterpolation
        );

        graphics.setClip(previousClip);
    }

    private synchronized void ensureRendered(int width, int height) {
        if (cachedWallpaper != null && cachedWidth == width && cachedHeight == height) return;
        BufferedImage wallpaper = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D wallpaperGraphics = wallpaper.createGraphics();
        try {
            wallpaperGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if ("stretch".equals(fit)) {
                wallpaperGraphics.drawImage(image, 0, 0, width, height, null);
            } else {
                drawCover(wallpaperGraphics, width, height);
            }
        } finally {
            wallpaperGraphics.dispose();
        }

        cachedWidth = width;
        cachedHeight = height;
        cachedWallpaper = wallpaper;
        cachedDimmingAlpha = adaptiveBackdropDimming
            ? (int) Math.round(255 * adaptiveBackdropDimming(wallpaper, maximumBackdropDimming))
            : 0;
    }

    private static double adaptiveBackdropDimming(BufferedImage image, double maximum) {
        if (maximum <= 0) return 0;
        int capacity = ((image.getWidth() + 7) / 8) * ((image.getHeight() + 7) / 8);
        double[] samples = new double[capacity];
        int count = 0;
        for (int y = 4; y < image.getHeight(); y += 8) {
            for (int x = 4; x < image.getWidth(); x += 8) {
                int rgb = image.getRGB(x, y);
                samples[count++] = (0.2126 * ((rgb >>> 16) & 255)
                    + 0.7152 * ((rgb >>> 8) & 255)
                    + 0.0722 * (rgb & 255)) / 255.0;
            }
        }
        Arrays.sort(samples, 0, count);
        double brightTail = samples[Math.min(count - 1, (int) Math.floor((count - 1) * 0.85))];
        double tintLuminance = (0.2126 * 20 + 0.7152 * 22 + 0.0722 * 26) / 255.0;
        double required = brightTail <= TARGET_BACKDROP_LUMINANCE
            ? MINIMUM_ADAPTIVE_DIMMING
            : (brightTail - TARGET_BACKDROP_LUMINANCE) / (brightTail - tintLuminance);
        return Math.min(maximum, Math.max(Math.min(MINIMUM_ADAPTIVE_DIMMING, maximum), required));
    }

    private void drawCover(Graphics2D graphics, int width, int height) {
        double scale = Math.max(
            (double) width / (double) image.getWidth(),
            (double) height / (double) image.getHeight()
        );
        int scaledWidth = Math.max(1, (int) Math.ceil(image.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.ceil(image.getHeight() * scale));
        graphics.drawImage(
            image,
            (width - scaledWidth) / 2,
            (height - scaledHeight) / 2,
            scaledWidth,
            scaledHeight,
            null
        );
    }

    private static String normalizeFit(String value) {
        String fit = value == null ? "cover" : value.trim().toLowerCase(Locale.ROOT);
        if (!"cover".equals(fit) && !"stretch".equals(fit)) {
            throw new IllegalArgumentException("render.custom-background.fit must be cover or stretch");
        }
        return fit;
    }
}
