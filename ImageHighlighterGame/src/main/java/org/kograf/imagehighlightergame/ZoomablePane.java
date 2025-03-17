package org.kograf.imagehighlightergame;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import java.awt.image.BufferedImage;

public class ZoomablePane extends StackPane {

    private final ImageView imageView;
    private final Scale scaleTransform;
    private final DoubleProperty scaleProperty;

    public ZoomablePane() {
        // Создаем ImageView и сохраняем пропорции изображения
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        scaleTransform = new Scale(1, 1, 0, 0);
        imageView.getTransforms().add(scaleTransform);

        imageView.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            scaleTransform.setPivotX(newBounds.getWidth() / 2);
            scaleTransform.setPivotY(newBounds.getHeight() / 2);
        });

        // Свойство масштаба, с ограничениями (0.1..4.0)
        scaleProperty = new SimpleDoubleProperty(1.0);
        scaleProperty.addListener((obs, oldVal, newVal) -> {
            double s = clampScale(newVal.doubleValue());
            scaleTransform.setX(s);
            scaleTransform.setY(s);
        });

        // Автоматическое центрирование благодаря StackPane
        setAlignment(imageView, Pos.CENTER);
        getChildren().add(imageView);

        // Обработка масштабирования с помощью Ctrl+Scroll
        this.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
                double newScale = scaleProperty.get() * factor;
                newScale = clampScale(newScale);
                scaleProperty.set(newScale);
                e.consume();
            }
        });
    }

    /**
     * Возвращает свойство масштаба.
     */
    public DoubleProperty scaleProperty() {
        return scaleProperty;
    }

    /**
     * Устанавливает новое изображение, преобразуя BufferedImage в JavaFX Image.
     * Масштаб сбрасывается к 1.0.
     */
    public void setImage(BufferedImage bf) {
        if (bf == null) {
            imageView.setImage(null);
            scaleProperty.set(1.0);
            return;
        }
        Image fxImage = ImageUtils.convertToFxImage(bf);
        imageView.setImage(fxImage);
        scaleProperty.set(1.0);
    }

    /**
     * Ограничиваем масштаб в пределах [0.1 .. 4.0].
     */
    private double clampScale(double s) {
        if (s < 0.1) s = 0.1;
        if (s > 4.0) s = 4.0;
        return s;
    }
}
