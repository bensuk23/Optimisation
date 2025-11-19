package xor_neat;

import xor_common.CSVLogger;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.*;

public class NeatXorSolver {

    private static final Random RANDOM = new Random();
    private static final NeatConfig config = new NeatConfig();

    // Données XOR
    private static final double[][] XOR_INPUTS = {
            {0, 0, 1}, {0, 1, 1}, {1, 0, 1}, {1, 1, 1}
    };
    private static final double[] XOR_TARGETS = {0, 1, 1, 0};

    private static final int INPUT_COUNT = 2;
    private static final int OUTPUT_COUNT = 1;
    // Inputs + Biais + Output
    private static final int INITIAL_NODES = INPUT_COUNT + 1 + OUTPUT_COUNT;

    private static InnovationTracker innovationTracker;
    private static List<NeatGenome> population;
    private static List<Species> species;

    // Référence au visualiseur
    private static NetworkVisualizer visualizer;

    public static void main(String[] args) {
        System.out.println("--- Démarrage de NEAT (Mode Live Visual) ---");

        // 1. Ouvrir la fenêtre AVANT la boucle
        // On utilise invokeLater pour respecter le thread Swing
        SwingUtilities.invokeLater(() -> {
            visualizer = new NetworkVisualizer();
        });

        // Attendre un peu que la fenêtre s'ouvre
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        innovationTracker = new InnovationTracker(INITIAL_NODES);
        population = new ArrayList<>();
        species = new ArrayList<>();

        // Initialisation de la population
        for (int i = 0; i < NeatConfig.POPULATION_SIZE; i++) {
            population.add(new NeatGenome(INPUT_COUNT, OUTPUT_COUNT, innovationTracker));
        }

        NeatGenome bestOverallGenome = null;

        try (CSVLogger logger = new CSVLogger("neat_progression_log_complet.csv")) {

            for (int gen = 1; gen <= NeatConfig.MAX_GENERATIONS; gen++) {

                // 2. Évaluation
                for (NeatGenome genome : population) {
                    genome.setFitness(calculateFitness(genome));
                }

                // 3. Tri de la population (Le meilleur en premier)
                population.sort(Collections.reverseOrder(Comparator.comparingDouble(NeatGenome::getFitness)));

                // Sauvegarde du meilleur absolu
                if (bestOverallGenome == null || population.get(0).getFitness() > bestOverallGenome.getFitness()) {
                    bestOverallGenome = population.get(0).copy();
                }

                NeatGenome currentGenBest = population.get(0);

                // --- MISE A JOUR VISUELLE ---
                final int currentGen = gen;
                final NeatGenome genomeToDraw = currentGenBest; // Copie de référence
                SwingUtilities.invokeLater(() -> {
                    if (visualizer != null) {
                        visualizer.updateGenome(genomeToDraw, currentGen);
                    }
                });

                // --- Logs ---
                double maxFitness = currentGenBest.getFitness();
                double avgFitness = population.stream().mapToDouble(NeatGenome::getFitness).average().orElse(0.0);

                logger.logGeneration(gen, maxFitness, avgFitness);

                // Affichage console (toutes les 10 gén ou si on a un bon score)
                if (gen % 10 == 0 || gen == 1 || maxFitness > 14.0) {
                    System.out.printf("Gén %4d | MaxFit: %.4f | Espèces: %3d | Structure: %d N, %d L\n",
                            gen, maxFitness, species.size(),
                            currentGenBest.getNodes().size(), currentGenBest.getConnections().size());
                }

                // --- Vérification de Succès ---
                if (maxFitness >= NeatConfig.FITNESS_THRESHOLD) {
                    System.out.println("\n✅ SUCCÈS ! Solution trouvée à la génération " + gen);
                    // On force une dernière mise à jour visuelle
                    SwingUtilities.invokeLater(() -> visualizer.updateGenome(genomeToDraw, currentGen));
                    break;
                }

                // 4. Spéciation
                speciate();

                // 5. Reproduction
                List<NeatGenome> nextGeneration = new ArrayList<>();

                // Calcul du fitness total ajusté
                double totalAdjustedFitness = 0.0;
                for (Species s : species) {
                    s.adjustFitness();
                    totalAdjustedFitness += s.getTotalAdjustedFitness();
                }

                if (totalAdjustedFitness <= 0) {
                    // Sécurité : si tout le monde est nul, on garde la population actuelle ou on en recrée une
                    nextGeneration.addAll(population);
                } else {
                    for (Species s : species) {
                        // Nombre d'enfants proportionnel au fitness de l'espèce
                        int offspringCount = (int) ((s.getTotalAdjustedFitness() / totalAdjustedFitness) * NeatConfig.POPULATION_SIZE);

                        // Elitisme : on garde le champion de l'espèce directement
                        if (offspringCount > 0) {
                            s.getMembers().sort(Collections.reverseOrder(Comparator.comparingDouble(NeatGenome::getFitness)));
                            nextGeneration.add(s.getMembers().get(0).copy());
                            offspringCount--;
                        }

                        // Création des enfants (Crossover / Mutation)
                        for (int i = 0; i < offspringCount; i++) {
                            nextGeneration.add(s.createOffspring(config, innovationTracker));
                        }
                    }
                }

                // Remplissage (si erreurs d'arrondi)
                while (nextGeneration.size() < NeatConfig.POPULATION_SIZE) {
                    if (!species.isEmpty()) {
                        Species randomSpecies = species.get(RANDOM.nextInt(species.size()));
                        nextGeneration.add(randomSpecies.createOffspring(config, innovationTracker));
                    } else {
                        nextGeneration.add(new NeatGenome(INPUT_COUNT, OUTPUT_COUNT, innovationTracker));
                    }
                }
                population = nextGeneration;
            }

            displayFinalResults(bestOverallGenome);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Méthodes Logiques ---

    private static void speciate() {
        // Vider les espèces
        for (Species s : species) s.clear();

        // Réassigner chaque génome
        for (NeatGenome genome : population) {
            boolean foundSpecies = false;
            for (Species s : species) {
                if (s.addMember(genome, config)) {
                    foundSpecies = true;
                    break;
                }
            }
            if (!foundSpecies) {
                species.add(new Species(genome));
            }
        }

        // Supprimer les espèces vides
        species.removeIf(s -> s.getMembers().isEmpty());

        // Préparer pour la reproduction (tri interne, choix du représentant)
        for (Species s : species) {
            s.prepareForReproduction(config);
        }
    }

    private static double calculateFitness(NeatGenome genome) {
        double totalError = 0.0;
        for (int i = 0; i < XOR_INPUTS.length; i++) {
            double output = predict(genome, XOR_INPUTS[i]);
            double expected = XOR_TARGETS[i];
            // Erreur quadratique
            totalError += (expected - output) * (expected - output);
        }

        // Fitness Max théorique = 4.0 (4 tests corrects)
        // On met au carré pour punir les erreurs (standard NEAT)
        double fitness = 4.0 - totalError;
        if (fitness < 0) fitness = 0;
        return fitness * fitness;
    }

    // --- Cœur du Réseau de Neurones ---

    private static double predict(NeatGenome genome, double[] input) {
        Map<Integer, Double> nodeOutputs = new HashMap<>();

        // 1. Initialiser les entrées
        // Input 0 -> XOR_INPUTS[...][0]
        // Input 1 -> XOR_INPUTS[...][1]
        // Input 2 -> Bias (1.0)
        nodeOutputs.put(0, input[0]);
        nodeOutputs.put(1, input[1]);
        nodeOutputs.put(2, 1.0);

        // 2. Propagation (Relaxation)
        // Pour gérer les boucles ou les structures complexes, on itère
        // un nombre de fois suffisant pour que le signal traverse le réseau.
        // depthMax est une sécurité.
        int maxDepth = genome.getNodes().size() + 2;

        for (int step = 0; step < maxDepth; step++) {
            boolean unstable = false;

            // On parcourt tous les noeuds (sauf inputs)
            for (NodeGene node : genome.getNodes().values()) {
                if (node.type == NodeGene.NodeType.INPUT || node.type == NodeGene.NodeType.BIAS) continue;

                double sum = 0.0;
                boolean hasInputSignal = false;

                // Somme pondérée des connexions entrantes
                for (ConnectionGene conn : genome.getConnections().values()) {
                    if (conn.outNodeId == node.id && conn.enabled) {
                        hasInputSignal = true;
                        // Si l'entrée n'est pas encore calculée, on prend 0.0 (sera corrigé à l'itération suivante)
                        sum += nodeOutputs.getOrDefault(conn.inNodeId, 0.0) * conn.weight;
                    }
                }

                if (hasInputSignal) {
                    // Ajout du biais interne (mutation du noeud)
                    sum += node.bias;

                    double output = sigmoid(sum);

                    // Vérifier si la valeur a changé (stabilité)
                    double oldVal = nodeOutputs.getOrDefault(node.id, -999.0);
                    if (Math.abs(output - oldVal) > 1e-5) {
                        unstable = true;
                        nodeOutputs.put(node.id, output);
                    }
                }
            }

            // Si le réseau s'est stabilisé, on arrête plus tôt
            if (!unstable) break;
        }

        // Retourner la valeur du noeud de sortie (ID = 3 pour XOR avec 2 inputs + 1 biais)
        // Structure standard : 0,1 (Inputs), 2 (Bias), 3 (Output)
        return nodeOutputs.getOrDefault(3, 0.0);
    }

    /**
     * Fonction d'activation Sigmoïde "Raidie" (Steepened Sigmoid).
     * Crucial pour XOR avec NEAT.
     */
    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-4.9 * x));
    }

    private static void displayFinalResults(NeatGenome best) {
        System.out.println("\n--- Résultat Final ---");
        System.out.printf("Fitness Atteinte: %.6f\n", best.getFitness());
        System.out.printf("Structure: %d Noeuds, %d Connexions\n", best.getNodes().size(), best.getConnections().size());

        System.out.println("Prédictions :");
        for (int i = 0; i < XOR_INPUTS.length; i++) {
            double prediction = predict(best, XOR_INPUTS[i]);
            System.out.printf("  In: (%.0f, %.0f) | Cible: %.0f | Out: %.4f\n",
                    XOR_INPUTS[i][0], XOR_INPUTS[i][1], XOR_TARGETS[i], prediction);
        }
    }
}