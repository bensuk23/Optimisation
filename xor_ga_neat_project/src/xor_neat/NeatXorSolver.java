package xor_neat;

import xor_common.CSVLogger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NeatXorSolver {

    private static final Random RANDOM = new Random();
    private static final NeatConfig config = new NeatConfig();

    private static final double[][] XOR_INPUTS = {
            {0, 0, 1}, {0, 1, 1}, {1, 0, 1}, {1, 1, 1}
    };
    private static final double[] XOR_TARGETS = {0, 1, 1, 0};

    private static final int INPUT_COUNT = 2;
    private static final int OUTPUT_COUNT = 1;
    private static final int INITIAL_NODES = INPUT_COUNT + 1 + OUTPUT_COUNT;

    private static InnovationTracker innovationTracker;
    private static List<NeatGenome> population;
    private static List<Species> species;

    public static void main(String[] args) {
        System.out.println("--- Démarrage de NEAT 'Complet' pour XOR (Java) ---");

        innovationTracker = new InnovationTracker(INITIAL_NODES);
        population = new ArrayList<>();
        species = new ArrayList<>();

        for (int i = 0; i < NeatConfig.POPULATION_SIZE; i++) {
            population.add(new NeatGenome(INPUT_COUNT, OUTPUT_COUNT, innovationTracker));
        }

        NeatGenome bestOverallGenome = null;

        try (CSVLogger logger = new CSVLogger("neat_progression_log_complet.csv")) {

            for (int gen = 1; gen <= NeatConfig.MAX_GENERATIONS; gen++) {

                // 2. Évaluation (Calculer le fitness brut)
                for (NeatGenome genome : population) {
                    genome.setFitness(calculateFitness(genome));
                }

                // Trier la population par fitness brut
                population.sort(Collections.reverseOrder(Comparator.comparingDouble(NeatGenome::getFitness)));

                if (bestOverallGenome == null || population.get(0).getFitness() > bestOverallGenome.getFitness()) {
                    bestOverallGenome = population.get(0).copy();
                }

                double maxFitness = population.get(0).getFitness();
                double avgFitness = population.stream().mapToDouble(NeatGenome::getFitness).average().orElse(0.0);

                logger.logGeneration(gen, maxFitness, avgFitness);

                if (gen % 10 == 0 || gen == 1) {
                    System.out.printf("Gén %4d | Max Fitness: %.6f | Avg: %.4f | Espèces: %d | Meilleur (N/C): %d/%d\n",
                            gen, maxFitness, avgFitness, species.size(),
                            bestOverallGenome.getNodes().size(), bestOverallGenome.getConnections().size());
                }

                if (maxFitness >= NeatConfig.FITNESS_THRESHOLD) {
                    System.out.println("\n✅ NEAT (Complet) a résolu le XOR à la génération " + gen);
                    break;
                }

                // 3. Spéciation
                speciate();

                // 4. Calculer le fitness ajusté (partage de fitness)
                double totalAdjustedFitness = 0.0;
                for (Species s : species) {
                    s.adjustFitness();
                    totalAdjustedFitness += s.getTotalAdjustedFitness();
                }

                // 5. Reproduction
                List<NeatGenome> nextGeneration = new ArrayList<>();

                if (totalAdjustedFitness <= 0) {
                    // Si tout est à zéro, recréer (évite division par zéro)
                    for (int i = 0; i < NeatConfig.POPULATION_SIZE; i++) {
                        nextGeneration.add(new NeatGenome(INPUT_COUNT, OUTPUT_COUNT, innovationTracker));
                    }
                    population = nextGeneration;
                    continue; // Sauter le reste et passer à la prochaine génération
                }

                for (Species s : species) {
                    if (s.getMembers().isEmpty()) continue;

                    int offspringCount = (int) Math.round((s.getTotalAdjustedFitness() / totalAdjustedFitness) * NeatConfig.POPULATION_SIZE);

                    // Appliquer l'élitisme (conserver le meilleur de l'espèce)
                    if (offspringCount > 0 && NeatConfig.ELITISM > 0) {
                        // Tri basé sur le VRAI fitness (corrigé dans Species.java)
                        s.getMembers().sort(Collections.reverseOrder(Comparator.comparingDouble(NeatGenome::getFitness)));
                        nextGeneration.add(s.getMembers().get(0).copy()); // Ajouter le meilleur
                        offspringCount--;
                    }

                    // Créer le reste des enfants
                    for (int i = 0; i < offspringCount; i++) {
                        nextGeneration.add(s.createOffspring(config, innovationTracker));
                    }
                }

                // Remplir si nécessaire (arrondis)
                if (species.isEmpty() || species.get(0).getMembers().isEmpty()) {
                    while (nextGeneration.size() < NeatConfig.POPULATION_SIZE) {
                        nextGeneration.add(new NeatGenome(INPUT_COUNT, OUTPUT_COUNT, innovationTracker));
                    }
                } else {
                    while (nextGeneration.size() < NeatConfig.POPULATION_SIZE && !species.isEmpty()) {
                        nextGeneration.add(species.get(0).createOffspring(config, innovationTracker)); // de la meilleure espèce
                    }
                }

                population = nextGeneration;
            }

            displayFinalResults(bestOverallGenome);

        } catch (IOException e) {
            System.err.println("Erreur de logger: " + e.getMessage());
        }
    }

    private static void speciate() {
        // ... (inchangé) ...
        for (Species s : species) {
            s.clear();
        }
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
        species.removeIf(s -> s.getMembers().isEmpty());
        for (Species s : species) {
            s.prepareForReproduction(config);
        }
    }

    private static double calculateFitness(NeatGenome genome) {
        // ... (inchangé) ...
        double totalError = 0.0;
        for (int i = 0; i < XOR_INPUTS.length; i++) {
            double prediction = predict(genome, XOR_INPUTS[i]);
            totalError += Math.abs(XOR_TARGETS[i] - prediction);
        }
        double fitness = 4.0 - totalError;
        return fitness * fitness; // Fitness au carré
    }

    // --- CORRECTION MAJEURE : FONCTION PREDICT ---
    /**
     * Prédiction Feedforward avec un tri topologique correct.
     */
    private static double predict(NeatGenome genome, double[] input) {
        Map<Integer, Double> nodeOutputs = new HashMap<>();

        // 1. Séparer les nœuds par type
        List<NodeGene> inputs = new ArrayList<>();
        List<NodeGene> outputs = new ArrayList<>();
        List<NodeGene> hidden = new ArrayList<>();
        for (NodeGene n : genome.getNodes().values()) {
            if (n.type == NodeGene.NodeType.INPUT || n.type == NodeGene.NodeType.BIAS) {
                inputs.add(n);
            } else if (n.type == NodeGene.NodeType.OUTPUT) {
                outputs.add(n);
            } else {
                hidden.add(n);
            }
        }

        // 2. Initialiser les sorties des nœuds d'entrée
        int inputIndex = 0;
        for (NodeGene n : inputs) {
            if (n.type == NodeGene.NodeType.INPUT) {
                nodeOutputs.put(n.id, input[inputIndex++]);
            }
            if (n.type == NodeGene.NodeType.BIAS) {
                nodeOutputs.put(n.id, 1.0);
            }
        }

        // 3. Créer la liste des nœuds à activer (ordre topologique)
        List<NodeGene> pendingNodes = new ArrayList<>(hidden);
        pendingNodes.addAll(outputs);
        List<Integer> activatedNodeIds = new ArrayList<>(nodeOutputs.keySet());

        int safeguard = 0; // Empêche les boucles infinies
        while (!pendingNodes.isEmpty() && safeguard < genome.getNodes().size() * 2) {
            safeguard++;
            List<NodeGene> activatedThisRound = new ArrayList<>();

            for (NodeGene node : pendingNodes) {
                boolean allInputsReady = true;

                // Vérifier si toutes les connexions entrantes proviennent de nœuds déjà activés
                for (ConnectionGene conn : genome.getConnections().values()) {
                    if (conn.outNodeId == node.id && conn.enabled) {
                        if (!activatedNodeIds.contains(conn.inNodeId)) {
                            allInputsReady = false;
                            break;
                        }
                    }
                }

                if (allInputsReady) {
                    // Tous les prérequis sont là, activer ce nœud
                    double sum = 0.0;
                    for (ConnectionGene conn : genome.getConnections().values()) {
                        if (conn.outNodeId == node.id && conn.enabled) {
                            // Utiliser getOrDefault au cas où une entrée (ex: biais) n'a pas de connexion
                            sum += nodeOutputs.getOrDefault(conn.inNodeId, 0.0) * conn.weight;
                        }
                    }
                    nodeOutputs.put(node.id, sigmoid(sum + node.bias));
                    activatedNodeIds.add(node.id);
                    activatedThisRound.add(node);
                }
            }

            if (activatedThisRound.isEmpty() && !pendingNodes.isEmpty()) {
                // Erreur : Boucle ou structure impossible (réseau non-feedforward)
                // Cela ne devrait pas arriver dans ce projet, mais par sécurité :
                // Forcer l'activation avec 0.0 pour les entrées manquantes
                for (NodeGene node : pendingNodes) {
                    double sum = 0.0;
                    for (ConnectionGene conn : genome.getConnections().values()) {
                        if (conn.outNodeId == node.id && conn.enabled) {
                            sum += nodeOutputs.getOrDefault(conn.inNodeId, 0.0) * conn.weight;
                        }
                    }
                    nodeOutputs.put(node.id, sigmoid(sum + node.bias));
                    activatedThisRound.add(node);
                }
            }

            pendingNodes.removeAll(activatedThisRound);
        }

        // 3. Renvoyer la sortie du (premier) nœud de sortie
        // Note: suppose un seul nœud de sortie, ce qui est correct pour XOR
        return nodeOutputs.getOrDefault(outputs.get(0).id, 0.0);
    }


    private static double sigmoid(double z) {
        if (z > 500) z = 500;
        if (z < -500) z = -500;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private static void displayFinalResults(NeatGenome bestGenome) {
        // ... (inchangé) ...
        System.out.println("\n--- Résultat Final NEAT (Complet) ---");
        System.out.printf("Fitness Max Atteinte: %.6f\n", Math.sqrt(bestGenome.getFitness())); // sqrt pour revenir à l'échelle 4.0
        System.out.printf("Structure Finale: %d Noeuds, %d Connexions\n", bestGenome.getNodes().size(), bestGenome.getConnections().size());

        for (int i = 0; i < XOR_INPUTS.length; i++) {
            double prediction = predict(bestGenome, XOR_INPUTS[i]);
            System.out.printf("  Entrée: (%.0f, %.0f) | Cible: %.0f | Sortie Prédite: %.4f\n",
                    XOR_INPUTS[i][0], XOR_INPUTS[i][1], XOR_TARGETS[i], prediction);
        }
    }
}