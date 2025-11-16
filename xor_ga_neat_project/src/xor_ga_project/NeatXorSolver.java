package xor_neat_project;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Moteur d'évolution NEAT SIMPLIFIÉ pour XOR.
 */
public class NeatXorSolver {

    // --- Constantes et Variables ---
    private static final Random RANDOM = new Random();
    private static final int POPULATION_SIZE = 150;
    private static final int MAX_GENERATIONS = 150;
    private static final String LOG_FILE = "data_and_results/neat_progression_log.csv";

    // Jeu de Données XOR
    private static final float[][] XOR_INPUTS = {
            {0, 0, 1}, // x1, x2, Biais
            {0, 1, 1},
            {1, 0, 1},
            {1, 1, 1}
    };
    private static final float[] XOR_TARGETS = {0, 1, 1, 0};

    // --- 1. Fonctions Mathématiques ---

    private static double sigmoid(double z) {
        if (z > 500) z = 500;
        if (z < -500) z = -500;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    // --- 2. Propagation Avant (Dynamique) ---

    private static double predict(NeatGenome genome, float[] input) {

        Map<Integer, Double> nodeOutputs = new HashMap<>();

        // 1. Initialiser les entrées
        for (int i = 0; i < NeatGenome.INPUT_NODES; i++) {
            nodeOutputs.put(i, (double) input[i]);
        }

        // 2. Calculer les sorties des noeuds
        for (NeatGenome.Node node : genome.getNodes()) {
            if (node.id >= NeatGenome.INPUT_NODES) { // Nœuds cachés et sortie
                double sum = node.bias;

                for (NeatGenome.Connection conn : genome.getConnections()) {
                    if (conn.outNodeId == node.id && conn.enabled) {
                        if (nodeOutputs.containsKey(conn.inNodeId)) {
                            // L'accès direct aux champs 'public' est désormais possible
                            sum += nodeOutputs.get(conn.inNodeId) * conn.weight;
                        }
                    }
                }

                double output = sigmoid(sum);
                nodeOutputs.put(node.id, output);
            }
        }

        // 3. Retourner la sortie du noeud d'Output (ID 3)
        return nodeOutputs.getOrDefault(NeatGenome.INPUT_NODES, 0.0);
    }

    // --- 3. Fonction de Fitness ---

    private static double calculateFitness(NeatGenome genome) {
        double totalError = 0.0;

        for (int i = 0; i < XOR_INPUTS.length; i++) {
            float[] input = XOR_INPUTS[i];
            float target = XOR_TARGETS[i];

            double prediction = predict(genome, input);
            double error = Math.abs(target - prediction);
            totalError += error;
        }

        // Fitness = 4.0 - Erreur Totale (Max 4.0 pour la perfection)
        return 4.0 - totalError;
    }

    // --- 4. Cycle NEAT Simplifié ---

    private static List<NeatGenome> selection(List<NeatGenome> population) {
        Collections.sort(population, Comparator.comparingDouble(NeatGenome::getFitness).reversed());
        int eliteSize = POPULATION_SIZE / 2;
        return population.subList(0, eliteSize);
    }

    private static List<NeatGenome> nextGeneration(List<NeatGenome> parents) {
        List<NeatGenome> nextGen = new ArrayList<>();

        // Elitisme
        nextGen.add(parents.get(0));

        // Remplissage
        while (nextGen.size() < POPULATION_SIZE) {

            NeatGenome p1 = parents.get(RANDOM.nextInt(parents.size()));
            NeatGenome p2 = parents.get(RANDOM.nextInt(parents.size()));

            NeatGenome child = (p1.getFitness() > p2.getFitness()) ? p1 : p2;

            NeatGenome mutantChild = new NeatGenome();

            // Mutations NEAT
            mutantChild.mutateWeights();
            mutantChild.mutateAddConnection();
            mutantChild.mutateAddNode();

            nextGen.add(mutantChild);
        }
        return nextGen;
    }


    // --- 5. Boucle Principale ---

    public static void main(String[] args) {

        System.out.println("--- Démarrage de NEAT Simplifié pour XOR (Java) ---");

        List<NeatGenome> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(new NeatGenome());
        }

        NeatGenome bestOverallGenome = null;

        try (PrintWriter logWriter = new PrintWriter(new FileWriter(LOG_FILE))) {
            logWriter.println("Generation,MaxFitness,AvgFitness");

            for (int gen = 1; gen <= MAX_GENERATIONS; gen++) {

                // Évaluation
                population.forEach(genome -> genome.setFitness(calculateFitness(genome)));

                // Statistiques
                population.sort(Comparator.comparingDouble(NeatGenome::getFitness).reversed());
                double maxFitness = population.get(0).getFitness();
                double avgFitness = population.stream().mapToDouble(NeatGenome::getFitness).average().orElse(0.0);

                if (bestOverallGenome == null || maxFitness > bestOverallGenome.getFitness()) {
                    bestOverallGenome = population.get(0);
                }

                // Journalisation
                logWriter.printf("%d,%.6f,%.6f\n", gen, maxFitness, avgFitness);
                System.out.printf("Génération %4d | Max Fitness: %.6f | Noeuds/Connexions du meilleur: %d/%d\n",
                        gen, maxFitness, bestOverallGenome.getNodes().size(), bestOverallGenome.getConnections().size());

                // Condition de Succès
                if (maxFitness >= 3.99) {
                    System.out.println("\n✅ NEAT (Simplifié) a résolu le XOR !");
                    break;
                }

                // Prochaine Génération
                List<NeatGenome> parents = selection(population);
                population = nextGeneration(parents);
            }

            displayFinalResults(bestOverallGenome);

        } catch (IOException e) {
            System.err.println("Erreur de fichier lors de la journalisation : " + e.getMessage());
        }
    }

    private static void displayFinalResults(NeatGenome bestGenome) {
        System.out.println("\n--- Résultat Final NEAT Simplifié ---");
        System.out.printf("Fitness Max Atteinte: %.6f\n", bestGenome.getFitness());
        System.out.printf("Structure Finale: %d Noeuds, %d Connexions\n", bestGenome.getNodes().size(), bestGenome.getConnections().size());

        for (int i = 0; i < XOR_INPUTS.length; i++) {
            float[] input = XOR_INPUTS[i];
            float target = XOR_TARGETS[i];

            double prediction = predict(bestGenome, input);

            System.out.printf("  Entrée: (%.0f, %.0f) | Cible: %.0f | Sortie Prédite: %.4f\n",
                    input[0], input[1], target, prediction);
        }
    }
}