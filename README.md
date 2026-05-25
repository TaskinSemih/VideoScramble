<div align="center">

# 🎬 VideoScramble

**Chiffrement et déchiffrement de vidéos par permutation de lignes (Analop)**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.1-blue?logo=java)](https://openjfx.io/)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.8.1-green?logo=opencv)](https://opencv.org/)
[![Maven](https://img.shields.io/badge/Maven-3.x-red?logo=apachemaven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/Licence-Académique-lightgrey)](#)

*Projet académique — Semih Taskin & Mathéo Rose — S6*

</div>

---

## 📋 Table des matières

- [Présentation](#-présentation)
- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#-architecture)
- [Prérequis](#-prérequis)
- [Installation & Lancement](#-installation--lancement)
- [Guide d'utilisation](#-guide-dutilisation)
- [Algorithmes](#-algorithmes)
- [Structure du projet](#-structure-du-projet)
- [Auteurs](#-auteurs)

---

## 🎯 Présentation

**VideoScramble** est une application de bureau JavaFX implémentant le chiffrement vidéo par permutation de lignes selon l'algorithme **Analop**. La clé de chiffrement est définie par deux paramètres :

- **R** (offset) — entier de 0 à 255
- **S** (pas) — entier de 0 à 127

Le projet implémente trois fonctionnalités majeures :

| # | Fonctionnalité | Description |
|---|---|---|
| 1 | **Chiffrement / Déchiffrement** | Permutation des lignes de chaque frame vidéo |
| 2 | **Cassage par force brute** | Test des 32 768 clés possibles en parallèle avec score de qualité |
| 3 | **Stéganographie** | Embarquement de la clé directement dans la vidéo chiffrée |

---

## ✨ Fonctionnalités

### 🔐 Chiffrement Analop
- Permutation des lignes de chaque frame par blocs de puissances de 2
- Chiffrement et déchiffrement symétriques
- Prévisualisation en temps réel (flux source + flux traité côte à côte)
- Barre de progression avec statut

### 💥 Force Brute Parallèle
- Test exhaustif des **32 768 combinaisons** de clés (R ∈ [0,255], S ∈ [0,127])
- Exécution **multi-thread** via `ForkJoinPool` (utilise tous les cœurs CPU)
- Deux critères de qualité au choix :
  - **Euclide** — gradient moyen inter-lignes (rapide, efficace)
  - **Pearson** — corrélation entre lignes adjacentes (plus fin)
- Affichage de la frame déchiffrée et de la clé trouvée en temps réel

### 🕵️ Stéganographie Robuste (Anti-MP4)
- Embarquement de la clé (15 bits) dans **5 blocs 16×16 pixels** par frame
- Encodage binaire : `64 = bit 0`, `192 = bit 1`
- **Tolérance ±63 pixels** → résiste totalement à la compression MP4
- Vote majoritaire temporel sur les 15 premières frames pour une extraction fiable
- Déchiffrement automatique sans saisir la clé

---

## 🏗 Architecture

```
VideoScramble
├── Main.java                    ← Point d'entrée JavaFX
├── core/
│   ├── Scrambler.java          ← Algorithme Analop (encrypt / decrypt)
│   ├── BruteForcer.java        ← Force brute multi-thread (Euclide / Pearson)
│   └── Steganography.java      ← Stéganographie robuste par blocs 16×16
├── video/
│   └── VideoProcessor.java     ← Lecture/écriture vidéo OpenCV, orchestration
├── ui/
│   └── MainController.java     ← Contrôleur FXML (JavaFX)
├── util/
│   └── Utils.java              ← Conversion Mat OpenCV → Image JavaFX
└── resources/
    └── ui/
        ├── main.fxml           ← Layout JavaFX
        └── style.css           ← Thème dark professionnel
```

### Flux de données

```
Vidéo source (.mp4)
      │
      ▼
VideoProcessor ──── frame ──▶ Scrambler.encrypt(R, S)
      │                              │
      │              [optionnel]     ▼
      │          Steganography.embedKey() → écrit la clé dans la frame
      │
      ▼
Vidéo chiffrée (_scrambled.mp4)


Vidéo chiffrée
      │
      ▼ [si Clé Embarquée activée]
Steganography.extractKey()  ←── vote majoritaire sur 15 frames
      │
      ▼
Scrambler.decrypt(R_extrait, S_extrait)
      │
      ▼
Vidéo déchiffrée (_unscrambled.mp4)
```

---

## 📦 Prérequis

| Outil | Version minimale | Notes |
|---|---|---|
| **JDK** | 17 | OpenJDK ou Oracle JDK |
| **Maven** | 3.8+ | Gestion des dépendances |
| **Git** | 2.x | Pour cloner le dépôt |

> **Aucune installation manuelle d'OpenCV requise.** Le wrapper `openpnp/opencv` télécharge et charge automatiquement les binaires natifs pour Windows, macOS et Linux.

---

## 🚀 Installation & Lancement

### 1. Cloner le dépôt

```bash
git clone https://github.com/TaskinSemih/VideoScramble.git
cd VideoScramble
```

### 2. Compiler

```bash
mvn clean compile
```

### 3. Lancer l'application

```bash
mvn javafx:run
```

> La première exécution peut prendre quelques secondes (téléchargement des dépendances OpenCV).

---

## 📖 Guide d'utilisation

### Interface principale

L'interface est divisée en trois panneaux :

```
┌─────────────────────────────────────────────────────────────┐
│              Flux Vidéo Source   |   Flux Vidéo Traité      │
├───────────────────┬──────────────────┬──────────────────────┤
│ 1. SOURCE &       │ 2. PARAMÈTRES    │ 3. ACTIONS           │
│ STÉGANOGRAPHIE    │ DE LA CLÉ        │                      │
│                   │                  │ [LANCER LE FLUX]     │
│ [Parcourir...]    │ R : [____]       │ [Casser la Clé]      │
│ [x] Clé Embarquée│ S : [____]       │                      │
└───────────────────┴──────────────────┴──────────────────────┘
│ Barre de statut                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### 🔐 Scénario 1 — Chiffrement simple

1. Cliquer **Parcourir** → sélectionner une vidéo `.mp4`
2. Entrer une clé : **Décalage R** (ex: `40`) et **Pas S** (ex: `12`)
3. Sélectionner **Chiffrement (Encrypt)** dans le menu déroulant
4. Cliquer **LANCER LE FLUX**
5. ✅ La vidéo `nom_scrambled.mp4` est créée dans le même dossier

---

### 🔓 Scénario 2 — Déchiffrement avec la clé connue

1. Cliquer **Parcourir** → sélectionner la vidéo `_scrambled.mp4`
2. Entrer la même clé **R** et **S** utilisée au chiffrement
3. Sélectionner **Déchiffrement (Decrypt)**
4. Cliquer **LANCER LE FLUX**
5. ✅ La vidéo `nom_scrambled_unscrambled.mp4` est reconstituée

---

### 🕵️ Scénario 3 — Stéganographie (clé embarquée)

Ce mode permet de chiffrer **sans avoir besoin de mémoriser la clé** — elle est cachée dans la vidéo.

#### Étape A : Chiffrer avec clé embarquée

1. Sélectionner une vidéo source
2. Entrer **R** et **S** (ex: `40` et `12`)
3. ✅ Cocher **Activer la Clé Embarquée**
4. Sélectionner **Chiffrement (Encrypt)**
5. Cliquer **LANCER LE FLUX**
6. → La vidéo `_scrambled.mp4` contient la clé cachée dans ses pixels

#### Étape B : Déchiffrer sans connaître la clé

1. Sélectionner la vidéo `_scrambled.mp4`
2. Les champs R et S peuvent rester à `0`
3. ✅ Cocher **Activer la Clé Embarquée**
4. Sélectionner **Déchiffrement (Decrypt)**
5. Cliquer **LANCER LE FLUX**
6. → L'application extrait automatiquement la clé (`R=40, S=12`) et déchiffre la vidéo
7. ✅ Les champs R et S se mettent à jour automatiquement avec la clé trouvée

---

### 💥 Scénario 4 — Force brute

1. Sélectionner une vidéo **chiffrée** (dont vous ne connaissez pas la clé)
2. Choisir le critère : **Euclide** (rapide) ou **Pearson** (précis)
3. Cliquer **Casser la Clé (Force Brute)**
4. → L'application teste les 32 768 clés en parallèle
5. ✅ La meilleure clé trouvée s'affiche avec son score, et la frame est déchiffrée

---

## 🔬 Algorithmes

### Permutation Analop

Pour chaque frame, l'algorithme décompose la hauteur en blocs de puissances de 2 successives :

```
Chiffrement :  destRow = offset + (R + (2S + 1) × i) mod blockSize
Déchiffrement: srcRow  = offset + (R + (2S + 1) × i) mod blockSize  (inversé)
```

### Stéganographie par blocs 16×16

```
Clé 15 bits = (R << 7) | S
         ↓
bits[0..14]    (un bit par position)
         ↓
5 blocs 16×16 dans la frame (coin haut-gauche de chaque bloc) :
  Bloc p → Canal B encode bit[3p], G encode bit[3p+1], R encode bit[3p+2]
  Pixel = 64 si bit=0, 192 si bit=1
         ↓
Lecture : moyenne du bloc > 128 → bit=1
          Tolérance ±63 pixels (résiste au codec MP4)
```

### Score de qualité — Critère Euclide

```
score = Σ |ligne[i] - ligne[i+1]| / (largeur × (hauteur-1))
```
Une bonne clé donne une vidéo avec des gradients inter-lignes faibles → score bas = meilleure clé.

---

## 📁 Structure du projet

```
VideoScramble/
├── pom.xml                                          ← Dépendances Maven
├── README.md
├── .gitignore
└── src/
    └── main/
        ├── java/
        │   └── videoscramble/
        │       ├── Main.java
        │       ├── core/
        │       │   ├── Scrambler.java
        │       │   ├── BruteForcer.java
        │       │   └── Steganography.java
        │       ├── video/
        │       │   └── VideoProcessor.java
        │       ├── ui/
        │       │   └── MainController.java
        │       └── util/
        │           └── Utils.java
        └── resources/
            └── videoscramble/
                └── ui/
                    ├── main.fxml
                    └── style.css
```

---

## 👥 Auteurs

| Nom | Groupe |
|---|---|
| **Semih Taskin** | Groupe B2 |
| **Mathéo Rose** | Groupe Alt |

*Projet de S6 — Université — 2026*

---

## 📄 Licence

Projet académique — usage interne et éducatif uniquement.
