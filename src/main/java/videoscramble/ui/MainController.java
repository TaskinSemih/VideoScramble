package videoscramble.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;
import videoscramble.util.Utils;
import videoscramble.video.VideoProcessor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @file    MainController.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Contrôleur de l'interface JavaFX pour traiter les vidéos.
 */
public class MainController {

    @FXML private ImageView originalVideoView;
    @FXML private ImageView processedVideoView;
    @FXML private TextField keyRField;
    @FXML private TextField keySField;
    @FXML private TextField inputPathField;
    @FXML private ComboBox<String> modeComboBox;
    @FXML private ComboBox<String> criterionComboBox;
    @FXML private javafx.scene.control.CheckBox embedKeyCheckBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private VideoProcessor processor;

    @FXML
    public void initialize() {
        processor = new VideoProcessor();
        processor.setFrameCallback((original, processed, progress, currentR, currentS) -> {
            Image origImg = Utils.mat2Image(original);
            Image procImg = Utils.mat2Image(processed);
            
            Platform.runLater(() -> {
                originalVideoView.setImage(origImg);
                processedVideoView.setImage(procImg);
                progressBar.setProgress(progress);
                
                // Si la clé embarquée a changé (lors de l'extraction), on met à jour l'IHM
                if (embedKeyCheckBox.isSelected() && modeComboBox.getSelectionModel().getSelectedIndex() == 1) {
                    keyRField.setText(String.valueOf(currentR));
                    keySField.setText(String.valueOf(currentS));
                }
            });
            
            original.release();
            processed.release();
            
            // Ralentir un peu le thread pour permettre à l'IHM de se rafraîchir
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        modeComboBox.getItems().addAll("Chiffrement (Encrypt)", "Déchiffrement (Decrypt)");
        modeComboBox.getSelectionModel().select(0);
        
        criterionComboBox.getItems().addAll("Euclide", "Pearson");
        criterionComboBox.getSelectionModel().select(0);
    }

    @FXML
    public void onBrowseInput() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une vidéo d'entrée");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers vidéo", "*.mp4", "*.avi", "*.mkv", "*.m4v", "*.mov", "*.wmv", "*.flv"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            inputPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    public void onBrowseKey() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier de clé");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers texte", "*.txt"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                String content = Files.readString(Paths.get(file.getAbsolutePath())).trim();
                String[] parts = content.split(",");
                if (parts.length == 2) {
                    keyRField.setText(parts[0].trim());
                    keySField.setText(parts[1].trim());
                } else {
                    statusLabel.setText("Format de fichier clé invalide (attendu: r,s)");
                }
            } catch (Exception e) {
                statusLabel.setText("Erreur lecture clé: " + e.getMessage());
            }
        }
    }

    @FXML
    public void onStart() {
        String inputPath = inputPathField.getText();
        if (inputPath == null || inputPath.isEmpty() || !new File(inputPath).exists()) {
            statusLabel.setText("Veuillez sélectionner un fichier d'entrée valide.");
            return;
        }

        int r, s;
        try {
            r = Integer.parseInt(keyRField.getText());
            s = Integer.parseInt(keySField.getText());
            if (r < 0 || r > 255 || s < 0 || s > 127) {
                statusLabel.setText("Clé invalide. R [0-255], S [0-127]");
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Veuillez entrer des valeurs numériques pour R et S.");
            return;
        }

        boolean encrypt = modeComboBox.getSelectionModel().getSelectedIndex() == 0;
        boolean embedKey = embedKeyCheckBox.isSelected();
        String outputPath = getOutputPath(inputPath, encrypt, embedKey);

        progressBar.setProgress(0);
        statusLabel.setText("Traitement en cours...");

        new Thread(() -> {
            try {
                processor.processVideo(inputPath, outputPath, encrypt, r, s, embedKey);
                Platform.runLater(() -> statusLabel.setText("Terminé ! Vidéo sauvegardée : " + outputPath));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onBruteForce() {
        String inputPath = inputPathField.getText();
        if (inputPath == null || inputPath.isEmpty() || !new File(inputPath).exists()) {
            statusLabel.setText("Veuillez sélectionner un fichier d'entrée valide pour le cassage.");
            return;
        }

        boolean usePearson = criterionComboBox.getSelectionModel().getSelectedIndex() == 1;
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Force Brute en cours (test de 32768 clés)...");

        new Thread(() -> {
            try {
                Mat frame = processor.extractRepresentativeFrame(inputPath);
                if (frame == null || frame.empty()) {
                    Platform.runLater(() -> statusLabel.setText("Erreur: Impossible d'extraire une image de la vidéo."));
                    return;
                }

                videoscramble.core.BruteForcer forcer = new videoscramble.core.BruteForcer();
                
                long startTime = System.currentTimeMillis();
                videoscramble.core.BruteForcer.BruteForceResult bestKey = forcer.findKey(frame, usePearson);
                long elapsed = System.currentTimeMillis() - startTime;

                // Décoder la frame avec la meilleure clé pour la montrer à l'utilisateur
                videoscramble.core.Scrambler scrambler = new videoscramble.core.Scrambler();
                Mat decrypted = scrambler.decrypt(frame, bestKey.r, bestKey.s);
                Image origImg = Utils.mat2Image(frame);
                Image procImg = Utils.mat2Image(decrypted);

                Platform.runLater(() -> {
                    keyRField.setText(String.valueOf(bestKey.r));
                    keySField.setText(String.valueOf(bestKey.s));
                    
                    originalVideoView.setImage(origImg);
                    processedVideoView.setImage(procImg);
                    progressBar.setProgress(1.0);
                    
                    statusLabel.setText(String.format("Clé trouvée en %d ms ! R=%d, S=%d (Score: %.2f)", 
                            elapsed, bestKey.r, bestKey.s, bestKey.score));
                });
                
                frame.release();
                decrypted.release();
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur Force Brute : " + e.getMessage());
                    progressBar.setProgress(0);
                });
            }
        }).start();
    }

    private String getOutputPath(String inputPath, boolean encrypt, boolean embedKey) {
        int dotIndex = inputPath.lastIndexOf('.');
        String ext = dotIndex > 0 ? inputPath.substring(dotIndex) : ".mp4";
        String base = dotIndex > 0 ? inputPath.substring(0, dotIndex) : inputPath;
        return base + (encrypt ? "_scrambled" : "_unscrambled") + ext;
    }
}
