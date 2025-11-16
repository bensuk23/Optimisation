package xor_ga_classic;

import java.util.Arrays;

public class NeuralNet {

    // --- 1. Constantes d'Architecture et d'Encodage ---
    public static final int INPUT_SIZE = 2;
    public static final int HIDDEN_SIZE = 2;
    public static final int OUTPUT_SIZE = 1;

    // Nombre total de paramètres: (2*2 + 2) + (2*1 + 1) = 9
    public static final int TOTAL_PARAMS =
            (INPUT_SIZE * HIDDEN_SIZE) + HIDDEN_SIZE + (HIDDEN_SIZE * OUTPUT_SIZE) + OUTPUT_SIZE;

    public static final int BITS_PER_PARAM = 16;
    public static final int GENOME_LENGTH = TOTAL_PARAMS * BITS_PER_PARAM;
    public static final double MAX_MAGNITUDE = 10.0;
    public static final int MAX_INT_VALUE = (1 << 15) - 1; // 2^15 - 1

    // Jeu de données XOR
    public static final double[][] XOR_DATA = {
            {0, 0}, {0, 1}, {1, 0}, {1, 1}
    };
    public static final double[] XOR_TARGET = {0, 1, 1, 0};

    // --- 2. Fonctions Mathématiques ---

    /**
     * Fonction d'activation sigmoïde.
     */
    private static double sigmoid(double z) {
        // Similaire à np.clip, on s'assure que l'exponentielle ne déborde pas.
        if (z > 500) z = 500;
        if (z < -500) z = -500;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    // --- 3. Décodage Binaire vers Réel ---

    /**
     * Décode la chaîne de bits en 9 paramètres réels (poids et biais).
     */
    public static double[] decodeBitString(String bitString) {
        if (bitString.length() != GENOME_LENGTH) {
            throw new IllegalArgumentException("Longueur du génome invalide.");
        }

        double[] realParams = new double[TOTAL_PARAMS];

        for (int i = 0; i < TOTAL_PARAMS; i++) {
            int start = i * BITS_PER_PARAM;
            int end = start + BITS_PER_PARAM;
            String segment = bitString.substring(start, end);

            // 1. Décodage du signe
            int sign = segment.charAt(0) == '1' ? -1 : 1;

            // 2. Décodage de la magnitude (bits 1 à 15)
            String magnitudeBits = segment.substring(1);
            int magnitudeInt = Integer.parseInt(magnitudeBits, 2);

            // 3. Mise à l'échelle (Mapping vers [-MAX_MAGNITUDE, MAX_MAGNITUDE])
            double normalizedValue = (double) magnitudeInt / MAX_INT_VALUE;
            double realValue = sign * (normalizedValue * MAX_MAGNITUDE);

            realParams[i] = realValue;
        }
        return realParams;
    }

    // --- 4. Propagation Avant (Feedforward) ---

    /**
     * Effectue la propagation avant pour une entrée donnée.
     * @param realParams Les 9 poids et biais décodés.
     * @param input L'entrée XOR (e.g., {0, 1}).
     * @return La sortie prédite du réseau (un double).
     */
    public static double predict(double[] realParams, double[] input) {
        if (realParams.length != TOTAL_PARAMS) {
            throw new IllegalArgumentException("Nombre de paramètres invalide.");
        }

        // Indices de début pour le slicing des 9 paramètres
        int w1End = INPUT_SIZE * HIDDEN_SIZE; // 4
        int b1End = w1End + HIDDEN_SIZE; // 6
        int w2End = b1End + HIDDEN_SIZE * OUTPUT_SIZE; // 8
        // b2End est 9 (TOTAL_PARAMS)

        // Paramètres
        double[] W1 = Arrays.copyOfRange(realParams, 0, w1End);
        double[] B1 = Arrays.copyOfRange(realParams, w1End, b1End);
        double[] W2 = Arrays.copyOfRange(realParams, b1End, w2End);
        double B2 = realParams[w2End];

        double[] hiddenOutput = new double[HIDDEN_SIZE];

        // 1. Couche Cachée (2 neurones)
        for (int j = 0; j < HIDDEN_SIZE; j++) {
            double sum = 0.0;
            // W1 est stocké linéairement: W1[i*HIDDEN_SIZE + j]
            for (int i = 0; i < INPUT_SIZE; i++) {
                sum += input[i] * W1[i * HIDDEN_SIZE + j];
            }
            sum += B1[j]; // Ajout du biais
            hiddenOutput[j] = sigmoid(sum);
        }

        // 2. Couche de Sortie (1 neurone)
        double outputSum = 0.0;
        for (int j = 0; j < HIDDEN_SIZE; j++) {
            // W2 est stocké linéairement: W2[j * OUTPUT_SIZE]
            outputSum += hiddenOutput[j] * W2[j * OUTPUT_SIZE];
        }
        outputSum += B2; // Ajout du biais

        return sigmoid(outputSum);
    }

    // --- 5. Fonction d'Évaluation (Fitness) ---

    /**
     * Calcule la fitness à maximiser pour un génome donné.
     */
    public static double calculateFitness(String bitString) {
        try {
            double[] realParams = decodeBitString(bitString);
            double sse = 0.0; // Sum of Squared Errors

            for (int i = 0; i < XOR_DATA.length; i++) {
                double prediction = predict(realParams, XOR_DATA[i]);
                double error = XOR_TARGET[i] - prediction;
                sse += error * error;
            }

            // Fitness (Maximisation): 1 / (SSE + epsilon)
            return 1.0 / (sse + 1e-6);

        } catch (IllegalArgumentException e) {
            return 0.0; // Fitness minimale en cas d'erreur
        }
    }
}