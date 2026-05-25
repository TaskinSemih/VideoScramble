package videoscramble;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;

/**
 * @file    Main.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Point d'entrée de l'application. Initialise OpenCV et lance l'IHM JavaFX.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/videoscramble/ui/main.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/videoscramble/ui/style.css").toExternalForm());

        primaryStage.setTitle("VideoScramble - Chiffrement Vidéo");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Charger les bibliothèques natives OpenCV embarquées par openpnp
        OpenCV.loadLocally();
        
        launch(args);
    }
}
