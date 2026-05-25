package videoscramble.util;

import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;

/**
 * @file    Utils.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Méthodes utilitaires.
 */
public class Utils {

    /**
     * Convertit une matrice OpenCV (Mat) en Image JavaFX.
     * Utilise l'encodage BMP en mémoire (très rapide) pour éviter les problèmes d'espace colorimétrique.
     *
     * @param frame La matrice OpenCV à convertir
     * @return      L'image JavaFX correspondante
     */
    public static Image mat2Image(Mat frame) {
        if (frame == null || frame.empty()) return null;
        MatOfByte buffer = new MatOfByte();
        // L'encodage BMP est rapide et évite les pertes/compressions CPU intensives
        Imgcodecs.imencode(".bmp", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}
