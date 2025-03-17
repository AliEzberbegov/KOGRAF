package org.kograf.imagehighlightergame;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

public class ImageUtils {

    public static Image convertToFxImage(BufferedImage bf) {
        if (bf == null) return null;
        return SwingFXUtils.toFXImage(bf, null);
    }

    public static BufferedImage convertToBufferedImage(Image img) {
        if (img == null) return null;
        return SwingFXUtils.fromFXImage(img, null);
    }
}
