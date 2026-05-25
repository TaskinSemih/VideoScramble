package videoscramble.core;

import org.opencv.core.Mat;

/**
 * @file    Scrambler.java
 * @authors Mathéo Rose (Groupe Alt), Semih Taskin (Groupe B2)
 * @date    2026-05-25
 * @brief   Implémentation de l'algorithme de chiffrement/déchiffrement par permutation de lignes.
 */
public class Scrambler {

    /**
     * Chiffre l'image en mélangeant les lignes par blocs de tailles de puissances de 2.
     * 
     * @param input L'image d'entrée (en clair)
     * @param r     Décalage (offset) de la clé (8 bits)
     * @param s     Pas (step) de la clé (7 bits)
     * @return      L'image chiffrée
     */
    public Mat encrypt(Mat input, int r, int s) {
        Mat output = input.clone();
        int height = input.rows();
        int offset = 0;
        int remainingHeight = height;

        while (remainingHeight > 1) {
            int powerOf2 = getLargestPowerOf2(remainingHeight);
            
            // Appliquer la permutation sur le bloc courant
            for (int i = 0; i < powerOf2; i++) {
                int destY = offset + ((r + (2 * s + 1) * i) % powerOf2);
                int srcY = offset + i;
                input.row(srcY).copyTo(output.row(destY));
            }

            offset += powerOf2;
            remainingHeight -= powerOf2;
        }

        return output;
    }

    /**
     * Déchiffre l'image en inversant le mélange des lignes.
     * 
     * @param input L'image d'entrée (chiffrée)
     * @param r     Décalage (offset) de la clé (8 bits)
     * @param s     Pas (step) de la clé (7 bits)
     * @return      L'image déchiffrée
     */
    public Mat decrypt(Mat input, int r, int s) {
        Mat output = input.clone();
        int height = input.rows();
        int offset = 0;
        int remainingHeight = height;

        while (remainingHeight > 1) {
            int powerOf2 = getLargestPowerOf2(remainingHeight);
            
            // Inverser la permutation sur le bloc courant
            for (int i = 0; i < powerOf2; i++) {
                int destY = offset + ((r + (2 * s + 1) * i) % powerOf2);
                int srcY = offset + i;
                input.row(destY).copyTo(output.row(srcY));
            }

            offset += powerOf2;
            remainingHeight -= powerOf2;
        }

        return output;
    }

    /**
     * Trouve la plus grande puissance de 2 inférieure ou égale à n.
     * 
     * @param n La valeur limite
     * @return  La plus grande puissance de 2 <= n
     */
    private int getLargestPowerOf2(int n) {
        int power = 1;
        while (power * 2 <= n) {
            power *= 2;
        }
        return power;
    }
}
