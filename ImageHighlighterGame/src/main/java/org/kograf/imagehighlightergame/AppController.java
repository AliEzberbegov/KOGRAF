package org.kograf.imagehighlightergame;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AppController {

    private final BorderPane rootPane;

    private final TabPane tabPane;
    private final Tab tabOriginal;
    private final Tab tabProcessed;

    // StackPane для вкладки "Исходное"
    private final StackPane originalStack;

    // Placeholder (пунктирная рамка, иконка, текст)
    private final VBox dragDropPlaceholder;

    // ZoomablePane для исходного
    private ZoomablePane originalPane;

    // ZoomablePane для обработанного
    private final ZoomablePane processedPane;

    // Кнопки
    private final Button loadButton, processButton, saveButton;

    // Элементы управления
    private final Slider thresholdSlider;
    private final CheckBox morphCheckBox;
    private final Spinner<Integer> kernelSpinner;
    private final ColorPicker colorPicker;
    private final Label timeLabel;

    // Общий Spinner для масштаба (обе вкладки)
    private final Spinner<Integer> zoomSpinner;

    // Изображения
    private BufferedImage originalImage;
    private BufferedImage finalImage;

    public AppController() {
        rootPane = new BorderPane();

        tabPane = new TabPane();
        tabOriginal = new Tab("Исходное");
        tabProcessed = new Tab("Обработанное");
        tabOriginal.setClosable(false);
        tabProcessed.setClosable(false);

        // StackPane для вкладки "Исходное"
        originalStack = new StackPane();
        originalStack.setAlignment(Pos.CENTER);

        // Placeholder
        dragDropPlaceholder = createPlaceholder();
        originalStack.getChildren().add(dragDropPlaceholder);

        // Вешаем Drag & Drop на originalStack
        initDragAndDropOnStack(originalStack);

        // Вкладка "Исходное"
        tabOriginal.setContent(originalStack);

        // Вкладка "Обработанное"
        processedPane = new ZoomablePane();
        tabProcessed.setContent(processedPane);

        tabPane.getTabs().addAll(tabOriginal, tabProcessed);

        // Кнопки
        loadButton = new Button("Загрузить");
        loadButton.setOnAction(e -> chooseFileAndLoad());

        processButton = new Button("Обработать");
        processButton.setOnAction(e -> startProcessing());

        saveButton = new Button("Сохранить");
        saveButton.setDisable(true);
        saveButton.setOnAction(e -> saveImage());

        thresholdSlider = new Slider(0, 255, 128);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setMajorTickUnit(50);

        morphCheckBox = new CheckBox("Морфология");
        morphCheckBox.setSelected(false);

        kernelSpinner = new Spinner<>(1, 21, 1, 2);
        kernelSpinner.setEditable(true);

        colorPicker = new ColorPicker(Color.YELLOW);

        timeLabel = new Label("Время: -- ms");

        // Общий Spinner для масштаба (10..400%)
        zoomSpinner = new Spinner<>(10, 400, 100, 10);
        zoomSpinner.setEditable(true);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            // При переключении вкладки синхронизируем zoomSpinner со scaleProperty активной вкладки
            if (newTab == tabOriginal && originalPane != null) {
                double scale = originalPane.scaleProperty().get();
                zoomSpinner.getValueFactory().setValue((int)(scale * 100));
            } else if (newTab == tabProcessed) {
                double scale = processedPane.scaleProperty().get();
                zoomSpinner.getValueFactory().setValue((int)(scale * 100));
            }
        });

        processedPane.scaleProperty().addListener((obs, oldVal, newVal) -> {
            // Если вкладка "Обработанное" активна, синхронизируем Spinner
            if (tabPane.getSelectionModel().getSelectedItem() == tabProcessed) {
                zoomSpinner.getValueFactory().setValue((int)(newVal.doubleValue() * 100));
            }
        });

        zoomSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            double scale = newVal / 100.0;
            if (scale < 0.1) scale = 0.1;
            if (scale > 4.0) scale = 4.0;

            if (tabPane.getSelectionModel().getSelectedItem() == tabOriginal && originalPane != null) {
                // Меняем масштаб originalPane
                originalPane.scaleProperty().set(scale);
            } else if (tabPane.getSelectionModel().getSelectedItem() == tabProcessed) {
                processedPane.scaleProperty().set(scale);
            }
        });

        HBox buttonBar = new HBox(12, loadButton, processButton, saveButton);
        buttonBar.setPadding(new Insets(10));

        FlowPane controlsPane = new FlowPane(12, 12);
        controlsPane.setPadding(new Insets(0, 10, 10, 10));
        controlsPane.getChildren().addAll(
                new Label("Порог:"), thresholdSlider,
                morphCheckBox,
                new Label("Ядро:"), kernelSpinner,
                new Label("Цвет:"), colorPicker,
                new Label("Масштаб(%):"), zoomSpinner,
                timeLabel
        );

        VBox topContainer = new VBox(6, buttonBar, controlsPane);

        rootPane.setTop(topContainer);
        rootPane.setCenter(tabPane);
    }

    /**
     * Создаём VBox (placeholder) с пунктирной рамкой, иконкой, текстом.
     */
    private VBox createPlaceholder() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setPrefSize(700, 250);
        box.setMaxSize(700, 250);
        box.setAlignment(Pos.CENTER);

        box.setStyle(
                "-fx-border-color: #999;" +
                        "-fx-border-style: dashed;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-color: #ffffff;"
        );

        Label iconLabel = new Label("\uD83D\uDCC1");
        iconLabel.setStyle("-fx-font-size: 32; -fx-text-fill: #666;");

        Label textLabel = new Label("Перетащите изображение сюда");
        textLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #444; -fx-alignment: center;");

        box.getChildren().addAll(iconLabel, textLabel);
        return box;
    }

    /**
     * Навешиваем события Drag & Drop на StackPane.
     */
    private void initDragAndDropOnStack(StackPane stack) {
        stack.setOnDragOver(event -> {
            var db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                // Если placeholder виден, меняем его стиль
                if (dragDropPlaceholder.isVisible()) {
                    dragDropPlaceholder.setStyle(
                            "-fx-border-color: #80CBC4;" +
                                    "-fx-border-style: dashed;" +
                                    "-fx-border-width: 2;" +
                                    "-fx-border-radius: 8;" +
                                    "-fx-background-color: #E0F2F1;" +
                                    "-fx-alignment: center;"
                    );
                } else {
                    stack.setStyle("-fx-border-color: #80CBC4; -fx-border-style: dashed; -fx-border-width: 0;");
                }
            }
            event.consume();
        });

        stack.setOnDragExited(event -> {
            dragDropPlaceholder.setStyle(
                    "-fx-border-color: #999;" +
                            "-fx-border-style: dashed;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-color: #ffffff;" +
                            "-fx-alignment: center;"
            );
            stack.setStyle("");
            event.consume();
        });

        stack.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                success = true;
                loadOriginalImage(db.getFiles().get(0));
            }
            event.setDropCompleted(success);
            event.consume();

            dragDropPlaceholder.setStyle(
                    "-fx-border-color: #999;" +
                            "-fx-border-style: dashed;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-color: #ffffff;" +
                            "-fx-alignment: center;"
            );
            stack.setStyle("");
        });
    }

    /**
     * Кнопка "Загрузить" (FileChooser).
     */
    private void chooseFileAndLoad() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        var file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            loadOriginalImage(file);
        }
    }

    /**
     * Загружаем (или перезагружаем) исходное изображение.
     */
    private void loadOriginalImage(File file) {
        try {
            var bi = ImageIO.read(file);
            if (bi != null) {
                originalImage = bi;
                finalImage = null;
                timeLabel.setText("Время: -- ms");
                saveButton.setDisable(true);

                // Если не создан originalPane — создаём
                if (originalPane == null) {
                    originalPane = new ZoomablePane();
                    originalPane.scaleProperty().addListener((obs, oldVal, newVal) -> {
                        // Если вкладка "Исходное" активна, обновляем spinner
                        if (tabPane.getSelectionModel().getSelectedItem() == tabOriginal) {
                            zoomSpinner.getValueFactory().setValue((int)(newVal.doubleValue() * 100));
                        }
                    });
                }
                // Устанавливаем новое изображение
                originalPane.setImage(originalImage);

                // Прячем placeholder
                dragDropPlaceholder.setVisible(false);

                // В StackPane: убираем всё, добавляем originalPane + placeholder (скрыт)
                originalStack.getChildren().clear();
                originalStack.getChildren().addAll(originalPane, dragDropPlaceholder);

                // При переключении на вкладку "Исходное" — синхронизируем spinner
                if (tabPane.getSelectionModel().getSelectedItem() == tabOriginal) {
                    double scale = originalPane.scaleProperty().get();
                    zoomSpinner.getValueFactory().setValue((int)(scale * 100));
                }

                // Переключаемся на вкладку "Исходное"
                tabPane.getSelectionModel().select(tabOriginal);

            } else {
                System.out.println("Не удалось прочитать файл: " + file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BorderPane getRootPane() {
        return rootPane;
    }

    /**
     * Обработка (порог, морфология, подсветка).
     */
    private void startProcessing() {
        if (originalImage == null) return;

        processButton.setDisable(true);
        loadButton.setDisable(true);

        var task = new Task<Void>() {
            private long startTimeNs;

            @Override
            protected Void call() throws Exception {
                startTimeNs = System.nanoTime();

                double thresholdVal = thresholdSlider.getValue();
                boolean doMorph = morphCheckBox.isSelected();
                int kernelSize = kernelSpinner.getValue();
                Color fxColor = colorPicker.getValue();

                var thresholded = ImageProcessor.applyThresholdParallel(originalImage, (int) thresholdVal, null);
                if (doMorph) {
                    thresholded = ImageProcessor.morphologicalCloseParallel(thresholded, kernelSize, null);
                }

                var awtColor = new java.awt.Color(
                        (float) fxColor.getRed(),
                        (float) fxColor.getGreen(),
                        (float) fxColor.getBlue(),
                        (float) fxColor.getOpacity()
                );
                finalImage = ImageProcessor.colorizeThreshold(thresholded, originalImage, awtColor, null);

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                long elapsed = System.nanoTime() - startTimeNs;
                double ms = elapsed / 1_000_000.0;
                timeLabel.setText(String.format("Время: %.2f ms", ms));

                // "Обработанное" -> processedPane
                processedPane.setImage(finalImage);
                saveButton.setDisable(false);

                tabPane.getSelectionModel().select(tabProcessed);

                processButton.setDisable(false);
                loadButton.setDisable(false);
            }

            @Override
            protected void failed() {
                super.failed();
                timeLabel.setText("Ошибка при обработке!");
                processButton.setDisable(false);
                loadButton.setDisable(false);
            }
        };

        new Thread(task).start();
    }

    private void saveImage() {
        if (finalImage == null) return;
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить изображение");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg")
        );
        var file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            try {
                String ext = getExtension(file);
                if (ext == null) {
                    ext = "png";
                }
                ImageIO.write(finalImage, ext, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot + 1).toLowerCase();
        }
        return null;
    }
}