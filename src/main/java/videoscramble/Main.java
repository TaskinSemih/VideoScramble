package videoscramble;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import videoscramble.video.VideoProcessor;

/**
 * @file    Main.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Point d'entrée de l'application VideoScramble.
 *          Lance l'IHM JavaFX si aucun argument n'est fourni.
 *          Si des arguments sont fournis, exécute le traitement en ligne de commande.
 *
 * Usage CLI :
 *   mvn javafx:run -Djavafx.args="encrypt  <input> <output> <r> <s>"
 *   mvn javafx:run -Djavafx.args="decrypt  <input> <output> <r> <s>"
 *
 * Exemples :
 *   encrypt  video.mp4  video_scrambled.mp4  40  12
 *   decrypt  video_scrambled.mp4  video_out.mp4  40  12
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

    /**
     * Point d'entrée principal.
     * Si 5 arguments sont fournis (mode, input, output, r, s), traitement en CLI.
     * Sinon, ouverture de l'interface graphique JavaFX.
     *
     * @param args Arguments optionnels : mode input output r s
     */
    public static void main(String[] args) {
        // Charger les bibliothèques natives OpenCV embarquées par openpnp
        OpenCV.loadLocally();

        if (args.length == 5) {
            runCli(args);
        } else if (args.length != 0) {
            printUsage();
        } else {
            // Aucun argument → lancement de l'IHM graphique
            launch(args);
        }
    }

    /**
     * Exécute le traitement vidéo en mode ligne de commande.
     *
     * @param args Tableau d'arguments [mode, input, output, r, s]
     */
    private static void runCli(String[] args) {
        String mode   = args[0].toLowerCase();
        String input  = args[1];
        String output = args[2];

        if (!mode.equals("encrypt") && !mode.equals("decrypt")) {
            System.err.println("[ERREUR] Mode inconnu : '" + mode + "'. Valeurs attendues : encrypt | decrypt");
            printUsage();
            return;
        }

        int r, s;
        try {
            r = Integer.parseInt(args[3]);
            s = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("[ERREUR] R et S doivent être des entiers.");
            printUsage();
            return;
        }

        if (r < 0 || r > 255 || s < 0 || s > 127) {
            System.err.println("[ERREUR] R doit être dans [0-255] et S dans [0-127].");
            return;
        }

        boolean encrypt = mode.equals("encrypt");
        System.out.printf("[VideoScramble] Mode=%s | Entrée=%s | Sortie=%s | R=%d | S=%d%n",
                mode, input, output, r, s);

        VideoProcessor processor = new VideoProcessor();
        try {
            processor.processVideo(input, output, encrypt, r, s, false);
            System.out.println("[VideoScramble] Traitement terminé → " + output);
        } catch (Exception e) {
            System.err.println("[ERREUR] " + e.getMessage());
        }
    }

    /**
     * Affiche l'aide sur la sortie standard.
     */
    private static void printUsage() {
        System.out.println("Usage : VideoScramble <mode> <input> <output> <r> <s>");
        System.out.println("  mode   : encrypt | decrypt");
        System.out.println("  input  : chemin de la vidéo source");
        System.out.println("  output : chemin de la vidéo de sortie");
        System.out.println("  r      : décalage offset [0-255]");
        System.out.println("  s      : pas step [0-127]");
        System.out.println();
        System.out.println("Exemples :");
        System.out.println("  encrypt video.mp4 video_scrambled.mp4 40 12");
        System.out.println("  decrypt video_scrambled.mp4 video_out.mp4 40 12");
        System.out.println();
        System.out.println("Sans argument → lance l'interface graphique.");
    }
}
