package org.kograf.imagehighlightergame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.kograf.imagehighlightergame.AppController;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Image Highlighter");
        Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
        primaryStage.getIcons().add(icon);

        AppController controller = new AppController();
        Scene scene = new Scene(controller.getRootPane(), 1040, 600);

        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());

        primaryStage.setMinWidth(1040);
        primaryStage.setMinHeight(600);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
