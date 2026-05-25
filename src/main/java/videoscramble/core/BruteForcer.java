package videoscramble.core;

import org.opencv.core.Mat;
import java.util.stream.IntStream;

/**
 * @file    BruteForcer.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Recherche de la clé (r, s) par force brute.
 */
public class BruteForcer {

    public static class BruteForceResult {
        public int r;
        public int s;
        public double score;

        public BruteForceResult(int r, int s, double score) {
            this.r = r;
            this.s = s;
            this.score = score;
        }
    }

    /**
     * Recherche la meilleure clé.
     * 
     * @param frame      L'image OpenCV brouillée
     * @param usePearson Si true, utilise Pearson (max), sinon Euclidien (min)
     * @return Le meilleur résultat
     */
    public BruteForceResult findKey(Mat frame, boolean usePearson) {
        int width = frame.cols();
        int height = frame.rows();
        int channels = frame.channels();
        int rowBytes = width * channels;

        // Extraction de la matrice en un tableau flat très rapide d'accès
        byte[] flatData = new byte[width * height * channels];
        frame.get(0, 0, flatData);

        // Découpage en lignes
        byte[][] lines = new byte[height][rowBytes];
        for (int i = 0; i < height; i++) {
            System.arraycopy(flatData, i * rowBytes, lines[i], 0, rowBytes);
        }

        // On parallélise sur l'ensemble des 32768 combinaisons possibles de clés
        // r est sur 8 bits (0-255) et s sur 7 bits (0-127).
        // Une clé peut être représentée par un entier unique: key = (r << 7) | s
        
        return IntStream.range(0, 32768).parallel()
            .mapToObj(key -> {
                int r = key >> 7;
                int s = key & 127;
                
                // Déchiffrement purement virtuel (affectation de pointeurs)
                byte[][] decrypted = virtualDecrypt(lines, height, r, s);
                
                // Calcul du score
                double score = usePearson ? calculatePearsonScore(decrypted, rowBytes) 
                                          : calculateEuclideanScore(decrypted, rowBytes);
                
                return new BruteForceResult(r, s, score);
            })
            // Réduction pour trouver le meilleur score
            .reduce((r1, r2) -> {
                if (usePearson) {
                    return r1.score > r2.score ? r1 : r2; // Pearson : maximiser
                } else {
                    return r1.score < r2.score ? r1 : r2; // Euclidien : minimiser
                }
            })
            .orElse(new BruteForceResult(0, 0, 0));
    }

    /**
     * Déchiffrement virtuel (ne copie pas les pixels, réorganise juste les pointeurs des lignes)
     */
    private byte[][] virtualDecrypt(byte[][] input, int height, int r, int s) {
        byte[][] output = new byte[height][];
        int offset = 0;
        int remainingHeight = height;

        while (remainingHeight > 1) {
            int powerOf2 = getLargestPowerOf2(remainingHeight);
            
            for (int i = 0; i < powerOf2; i++) {
                int destY = offset + ((r + (2 * s + 1) * i) % powerOf2);
                int srcY = offset + i;
                output[srcY] = input[destY];
            }

            offset += powerOf2;
            remainingHeight -= powerOf2;
        }
        
        // Si il reste une ligne toute seule à la fin, on la laisse inchangée
        if (remainingHeight == 1) {
            output[offset] = input[offset];
        }

        return output;
    }

    private double calculateEuclideanScore(byte[][] decrypted, int n) {
        double totalScore = 0;
        for (int row = 0; row < decrypted.length - 1; row++) {
            byte[] x = decrypted[row];
            byte[] y = decrypted[row + 1];
            
            double lineDistSq = 0;
            for (int i = 0; i < n; i++) {
                int diff = (x[i] & 0xFF) - (y[i] & 0xFF);
                lineDistSq += diff * diff;
            }
            totalScore += Math.sqrt(lineDistSq);
        }
        return totalScore;
    }

    private double calculatePearsonScore(byte[][] decrypted, int n) {
        double totalScore = 0;
        for (int row = 0; row < decrypted.length - 1; row++) {
            byte[] x = decrypted[row];
            byte[] y = decrypted[row + 1];

            long sumX = 0, sumY = 0;
            for (int i = 0; i < n; i++) {
                sumX += x[i] & 0xFF;
                sumY += y[i] & 0xFF;
            }
            
            double xMean = (double) sumX / n;
            double yMean = (double) sumY / n;

            double num = 0;
            double denX = 0;
            double denY = 0;

            for (int i = 0; i < n; i++) {
                double xi = (x[i] & 0xFF) - xMean;
                double yi = (y[i] & 0xFF) - yMean;
                
                num += xi * yi;
                denX += xi * xi;
                denY += yi * yi;
            }

            if (denX != 0 && denY != 0) {
                totalScore += num / (Math.sqrt(denX) * Math.sqrt(denY));
            }
        }
        return totalScore;
    }

    private int getLargestPowerOf2(int n) {
        int power = 1;
        while (power * 2 <= n) {
            power *= 2;
        }
        return power;
    }
}
