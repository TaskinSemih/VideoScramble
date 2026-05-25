package videoscramble.core;

import org.opencv.core.Mat;

/**
 * @file    Steganography.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Stéganographie robuste par blocs 16×16.
 *          Encodage : 64 = bit 0, 192 = bit 1 → tolérance ±63 contre la compression MP4.
 *          5 blocs × 3 canaux (BGR) = 15 bits = clé complète.
 */
public class Steganography {

    public static class ExtractedKey {
        public int r;
        public int s;
        public boolean valid;

        public ExtractedKey(int r, int s) {
            this.r = r;
            this.s = s;
            this.valid = (r >= 0 && r <= 255 && s >= 0 && s <= 127);
        }
    }

    /**
     * Embarque la clé (r, s) dans 5 blocs 16×16 spatialement espacés.
     * 5 blocs × 3 canaux (B, G, R) = 15 bits — clé complète de 15 bits.
     * Chaque pixel du bloc est fixé à 64 (bit=0) ou 192 (bit=1).
     *
     * @param frame L'image à modifier (modifiée sur place)
     * @param r     Offset (8 bits)
     * @param s     Step (7 bits)
     */
    public void embedKey(Mat frame, int r, int s) {
        int key = ((r & 0xFF) << 7) | (s & 0x7F);

        int[] bits = new int[15];
        for (int i = 0; i < 15; i++) {
            bits[i] = (key >> i) & 1;
        }

        int width  = frame.cols();
        int height = frame.rows();
        int[][] origins = blockOrigins(width, height);

        for (int p = 0; p < 5; p++) {
            int bx = origins[p][0];
            int by = origins[p][1];
            double vB = bits[3 * p]     == 1 ? 192.0 : 64.0;
            double vG = bits[3 * p + 1] == 1 ? 192.0 : 64.0;
            double vR = bits[3 * p + 2] == 1 ? 192.0 : 64.0;

            for (int dy = 0; dy < 16; dy++) {
                for (int dx = 0; dx < 16; dx++) {
                    int px = bx + dx;
                    int py = by + dy;
                    if (px >= width || py >= height) continue;
                    double[] pixel = frame.get(py, px);
                    if (pixel == null) continue;
                    pixel[0] = vB;
                    pixel[1] = vG;
                    pixel[2] = vR;
                    frame.put(py, px, pixel);
                }
            }
        }
    }

    /**
     * Extrait la clé depuis les 5 blocs 16×16 encodés par embedKey.
     * Calcule la moyenne de chaque bloc : moyenne > 128 → bit=1, sinon → bit=0.
     *
     * @param frame L'image contenant la clé
     * @return La clé extraite (r, s)
     */
    public ExtractedKey extractKey(Mat frame) {
        int width  = frame.cols();
        int height = frame.rows();
        int[][] origins = blockOrigins(width, height);

        int key = 0;
        for (int p = 0; p < 5; p++) {
            int bx = origins[p][0];
            int by = origins[p][1];

            double sumB = 0, sumG = 0, sumR = 0;
            int count = 0;
            for (int dy = 0; dy < 16; dy++) {
                for (int dx = 0; dx < 16; dx++) {
                    int px = bx + dx;
                    int py = by + dy;
                    if (px >= width || py >= height) continue;
                    double[] pixel = frame.get(py, px);
                    if (pixel == null) continue;
                    sumB += pixel[0];
                    sumG += pixel[1];
                    sumR += pixel[2];
                    count++;
                }
            }
            if (count == 0) continue;

            int bBit = (sumB / count) > 128 ? 1 : 0;
            int gBit = (sumG / count) > 128 ? 1 : 0;
            int rBit = (sumR / count) > 128 ? 1 : 0;

            key |= bBit << (3 * p);
            key |= gBit << (3 * p + 1);
            key |= rBit << (3 * p + 2);
        }

        int extractedR = (key >> 7) & 0xFF;
        int extractedS = key & 0x7F;
        return new ExtractedKey(extractedR, extractedS);
    }

    /**
     * Retourne les origines (coin haut-gauche) des 5 blocs 16×16 dans l'image.
     */
    private int[][] blockOrigins(int width, int height) {
        return new int[][] {
            { 16,             16              },
            { width / 2 - 8, 16              },
            { width - 32,    16              },
            { 16,            height / 2 - 8  },
            { width / 2 - 8, height / 2 - 8 }
        };
    }
}
