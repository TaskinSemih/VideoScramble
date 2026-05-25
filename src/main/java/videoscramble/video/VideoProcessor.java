package videoscramble.video;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import videoscramble.core.Scrambler;

/**
 * @file    VideoProcessor.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Gestion de la lecture, traitement et écriture de la vidéo avec OpenCV.
 */
public class VideoProcessor {

    private final Scrambler scrambler;
    
    private final videoscramble.core.Steganography steganography;
    
    // Callback pour remonter chaque frame traitée à l'IHM
    private FrameCallback frameCallback;

    public interface FrameCallback {
        void onFrameProcessed(Mat original, Mat processed, double progress, int currentR, int currentS);
    }

    public VideoProcessor() {
        this.scrambler = new Scrambler();
        this.steganography = new videoscramble.core.Steganography();
    }

    public void setFrameCallback(FrameCallback callback) {
        this.frameCallback = callback;
    }

    /**
     * Traite une vidéo complète (chiffrement ou déchiffrement).
     *
     * @param inputPath  Chemin de la vidéo d'entrée
     * @param outputPath Chemin de la vidéo de sortie
     * @param encrypt    true pour chiffrer, false pour déchiffrer
     * @param r          Clé offset
     * @param s          Clé step
     * @param embedKey   Si true, embarque/extrait la clé dans la frame
     * @throws Exception Si erreur d'ouverture
     */
    public void processVideo(String inputPath, String outputPath, boolean encrypt, int r, int s, boolean embedKey) throws Exception {
        VideoCapture capture = new VideoCapture(inputPath);
        if (!capture.isOpened()) {
            throw new Exception("Impossible d'ouvrir la vidéo d'entrée : " + inputPath);
        }

        int width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        
        int codec = VideoWriter.fourcc('m', 'p', '4', 'v'); // Codec standard

        VideoWriter writer = new VideoWriter(outputPath, codec, fps, new Size(width, height), true);

        if (!writer.isOpened()) {
            capture.release();
            throw new Exception("Impossible de créer la vidéo de sortie : " + outputPath);
        }

        Mat frame = new Mat();
        int frameCount = 0;
        
        int currentR = r;
        int currentS = s;

        // Si on déchiffre avec clé embarquée, on fait un vote majoritaire temporel sur les 15 premières frames !
        if (embedKey && !encrypt) {
            int[] keyCounts = new int[32768];
            int bestKey = 0;
            int maxCount = 0;
            
            Mat tempFrame = new Mat();
            for (int i = 0; i < 15; i++) {
                if (!capture.read(tempFrame) || tempFrame.empty()) break;
                videoscramble.core.Steganography.ExtractedKey ext = steganography.extractKey(tempFrame);
                int k = (ext.r << 7) | ext.s;
                keyCounts[k]++;
                if (keyCounts[k] > maxCount) {
                    maxCount = keyCounts[k];
                    bestKey = k;
                }
            }
            tempFrame.release();
            
            currentR = (bestKey >> 7) & 0xFF;
            currentS = bestKey & 0x7F;
            
            // On rembobine la vidéo au début pour le vrai traitement
            capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
        }

        while (capture.read(frame)) {
            Mat processedFrame;
            
            if (encrypt) {
                processedFrame = scrambler.encrypt(frame, currentR, currentS);
                if (embedKey) {
                    steganography.embedKey(processedFrame, currentR, currentS);
                }
            } else {
                // currentR et currentS ont déjà été extraits de façon robuste au début
                processedFrame = scrambler.decrypt(frame, currentR, currentS);
            }
            
            writer.write(processedFrame);
            
            frameCount++;
            if (frameCallback != null) {
                double progress = totalFrames > 0 ? (double) frameCount / totalFrames : 0.0;
                // Cloner pour éviter les problèmes d'accès concurrentiel dans l'IHM
                frameCallback.onFrameProcessed(frame.clone(), processedFrame.clone(), progress, currentR, currentS);
            }
        }

        capture.release();
        writer.release();
    }

    /**
     * Extrait une frame de la vidéo qui n'est pas noire (ex: pour le Brute Force).
     * @param inputPath Chemin de la vidéo
     * @return La frame OpenCV (Mat), ou null si échec.
     */
    public Mat extractRepresentativeFrame(String inputPath) {
        VideoCapture capture = new VideoCapture(inputPath);
        if (!capture.isOpened()) {
            return null;
        }

        Mat frame = new Mat();
        // On passe les 10 premières frames qui sont souvent noires / transitions
        for(int i = 0; i < 10; i++) {
            capture.read(frame);
        }
        
        // On s'assure d'avoir lu une frame
        if (frame.empty()) {
            capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
            capture.read(frame);
        }
        
        capture.release();
        return frame.clone();
    }
}
