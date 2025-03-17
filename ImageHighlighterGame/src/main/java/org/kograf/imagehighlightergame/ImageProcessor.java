package org.kograf.imagehighlightergame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class ImageProcessor {

    public static BufferedImage applyThresholdParallel(BufferedImage input, int threshold, Object unused) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        IntStream.range(0, h).parallel().forEach(y -> {
            for (int x = 0; x < w; x++) {
                int argb = input.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int brightness = (r + g + b) / 3;
                if (brightness > threshold) {
                    output.setRGB(x, y, 0xFFFFFFFF);
                } else {
                    output.setRGB(x, y, 0xFF000000);
                }
            }
        });

        return output;
    }

    public static BufferedImage morphologicalCloseParallel(BufferedImage input, int kernelSize, Object unused) {
        BufferedImage dilated = dilateParallel(input, kernelSize);
        return erodeParallel(dilated, kernelSize);
    }

    private static BufferedImage dilateParallel(BufferedImage input, int kernelSize) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int radius = kernelSize / 2;

        IntStream.range(0, h).parallel().forEach(y -> {
            for (int x = 0; x < w; x++) {
                boolean white = false;
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int nx = x + kx;
                        int ny = y + ky;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            int pix = input.getRGB(nx, ny);
                            if (pix == 0xFFFFFFFF) {
                                white = true;
                                break;
                            }
                        }
                    }
                    if (white) break;
                }
                if (white) {
                    output.setRGB(x, y, 0xFFFFFFFF);
                } else {
                    output.setRGB(x, y, 0xFF000000);
                }
            }
        });

        return output;
    }

    private static BufferedImage erodeParallel(BufferedImage input, int kernelSize) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int radius = kernelSize / 2;

        IntStream.range(0, h).parallel().forEach(y -> {
            for (int x = 0; x < w; x++) {
                boolean black = false;
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int nx = x + kx;
                        int ny = y + ky;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            int pix = input.getRGB(nx, ny);
                            if (pix == 0xFF000000) {
                                black = true;
                                break;
                            }
                        }
                    }
                    if (black) break;
                }
                if (black) {
                    output.setRGB(x, y, 0xFF000000);
                } else {
                    output.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        });

        return output;
    }

    public static BufferedImage colorizeThreshold(BufferedImage thresholded, BufferedImage original, Color highlightColor, Object unused) {
        int w = thresholded.getWidth();
        int h = thresholded.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int argbColor = (highlightColor.getAlpha() << 24)
                | (highlightColor.getRed() << 16)
                | (highlightColor.getGreen() << 8)
                | highlightColor.getBlue();

        IntStream.range(0, h).parallel().forEach(y -> {
            for (int x = 0; x < w; x++) {
                int pix = thresholded.getRGB(x, y);
                if (pix == 0xFFFFFFFF) {
                    output.setRGB(x, y, argbColor);
                } else {
                    output.setRGB(x, y, original.getRGB(x, y));
                }
            }
        });

        return output;
    }
}
