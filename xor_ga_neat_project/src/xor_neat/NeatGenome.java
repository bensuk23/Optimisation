package xor_neat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class NeatGenome {

    private static final Random RANDOM = new Random();

    private final Map<Integer, NodeGene> nodes = new HashMap<>();
    private final Map<Integer, ConnectionGene> connections = new TreeMap<>();

    private double fitness = 0.0;
    private double adjustedFitness = 0.0; // Corrigé de la dernière fois

    /**
     * Constructeur pour un génome minimal (Inputs -> Outputs)
     */
    public NeatGenome(int inputCount, int outputCount, InnovationTracker tracker) {
        // ... (constructeur inchangé) ...
        // Inputs + Biais (le +1)
        for (int i = 0; i < inputCount; i++) {
            nodes.put(i, new NodeGene(i, NodeGene.NodeType.INPUT, 0.0));
        }
        nodes.put(inputCount, new NodeGene(inputCount, NodeGene.NodeType.BIAS, 1.0)); // Nœud de Biais

        // Outputs
        for (int i = 0; i < outputCount; i++) {
            int nodeId = inputCount + 1 + i;
            nodes.put(nodeId, new NodeGene(nodeId, NodeGene.NodeType.OUTPUT, 0.0));
        }

        // Connexions initiales (de tous les inputs + biais vers tous les outputs)
        for (int i = 0; i <= inputCount; i++) { // <= inclut le biais
            for (int j = 0; j < outputCount; j++) {
                int outNodeId = inputCount + 1 + j;
                double initialWeight = RANDOM.nextDouble() * 2 - 1;
                int innovation = tracker.getInnovation(i, outNodeId);
                connections.put(innovation, new ConnectionGene(innovation, i, outNodeId, initialWeight, true));
            }
        }
    }

    /**
     * Constructeur pour le Crossover
     */
    private NeatGenome() {
        // Constructeur vide pour l'enfant
    }

    /**
     * Crossover
     */
    public static NeatGenome crossover(NeatGenome parent1, NeatGenome parent2) {
        NeatGenome fitter = (parent1.getFitness() >= parent2.getFitness()) ? parent1 : parent2;
        NeatGenome lessFit = (fitter == parent1) ? parent2 : parent1;

        NeatGenome child = new NeatGenome();

        // ... (logique de crossover inchangée) ...
        for (NodeGene node : fitter.getNodes().values()) {
            child.nodes.put(node.id, node.copy());
        }

        for (ConnectionGene connFitter : fitter.getConnections().values()) {
            ConnectionGene connLessFit = lessFit.getConnections().get(connFitter.innovation);

            if (connLessFit != null) {
                ConnectionGene childGene = (RANDOM.nextBoolean()) ? connFitter.copy() : connLessFit.copy();
                if (!connFitter.enabled || !connLessFit.enabled) {
                    if (RANDOM.nextDouble() < 0.75) {
                        childGene.enabled = false;
                    }
                }
                child.connections.put(childGene.innovation, childGene);
            } else {
                child.connections.put(connFitter.innovation, connFitter.copy());
            }
        }
        return child;
    }

    /**
     * LE CŒUR DE NEAT : Distance de Compatibilité
     */
    public double compatibilityDistance(NeatGenome other, NeatConfig config) {
        int excess = 0;
        int disjoint = 0;
        double weightDiff = 0.0;
        int matching = 0;

        var iter1 = this.connections.values().iterator();
        var iter2 = other.connections.values().iterator();

        ConnectionGene conn1 = iter1.hasNext() ? iter1.next() : null;
        ConnectionGene conn2 = iter2.hasNext() ? iter2.next() : null;

        while (conn1 != null || conn2 != null) {
            // ... (logique de comptage inchangée) ...
            if (conn1 == null) {
                excess++;
                conn2 = iter2.hasNext() ? iter2.next() : null;
            } else if (conn2 == null) {
                excess++;
                conn1 = iter1.hasNext() ? iter1.next() : null;
            } else {
                if (conn1.innovation == conn2.innovation) {
                    matching++;
                    weightDiff += Math.abs(conn1.weight - conn2.weight);
                    conn1 = iter1.hasNext() ? iter1.next() : null;
                    conn2 = iter2.hasNext() ? iter2.next() : null;
                } else if (conn1.innovation < conn2.innovation) {
                    disjoint++;
                    conn1 = iter1.hasNext() ? iter1.next() : null;
                } else {
                    disjoint++;
                    conn2 = iter2.hasNext() ? iter2.next() : null;
                }
            }
        }

        double avgWeightDiff = (matching == 0) ? 0 : weightDiff / matching;

        // --- LA CORRECTION EST ICI ---

        // La normalisation par 'N' (le nombre de gènes) rend la distance
        // structurelle (excess, disjoint) négligeable pour les grands génomes.
        // Nous la supprimons pour C1 et C2.

        // ANCIENNE LOGIQUE (BOGUÉE) :
        // double N = Math.max(1, Math.max(this.connections.size(), other.connections.size()));
        // return (config.C1_EXCESS * excess / N) +
        //       (config.C2_DISJOINT * disjoint / N) +
        //       (config.C3_WEIGHTS * avgWeightDiff);

        // NOUVELLE LOGIQUE (CORRIGÉE) :
        // Les coefficients C1 et C2 sont maintenant absolus.
        // Seule la différence de poids (C3) est moyennée.
        return (config.C1_EXCESS * excess) +
                (config.C2_DISJOINT * disjoint) +
                (config.C3_WEIGHTS * avgWeightDiff);
    }

    // --- Mutations (Inchangées) ---

    public void mutate(NeatConfig config, InnovationTracker tracker) {
        if (RANDOM.nextDouble() < config.MUTATE_WEIGHT_RATE) {
            mutateWeights(config);
        }
        if (RANDOM.nextDouble() < config.MUTATE_ADD_NODE_RATE) {
            mutateAddNode(tracker);
        }
        if (RANDOM.nextDouble() < config.MUTATE_ADD_CONNECTION_RATE) {
            mutateAddConnection(tracker);
        }
        if (RANDOM.nextDouble() < config.MUTATE_TOGGLE_ENABLE_RATE) {
            mutateToggleEnable();
        }
    }

    private void mutateWeights(NeatConfig config) {
        // ... (inchangé) ...
        for (ConnectionGene conn : connections.values()) {
            if (RANDOM.nextDouble() < config.WEIGHT_PERTURB_RATE) {
                conn.weight += (RANDOM.nextGaussian() * config.WEIGHT_PERTURB_STD_DEV);
            } else {
                conn.weight = RANDOM.nextDouble() * 4 - 2;
            }
        }
    }

    private void mutateAddNode(InnovationTracker tracker) {
        // ... (inchangé) ...
        if (connections.isEmpty()) return;
        List<ConnectionGene> possibleConns = new ArrayList<>(connections.values());
        ConnectionGene oldConn = possibleConns.get(RANDOM.nextInt(possibleConns.size()));
        oldConn.enabled = false;

        int newNodeId = tracker.getNewNodeId(oldConn.innovation);

        if (!nodes.containsKey(newNodeId)) {
            nodes.put(newNodeId, new NodeGene(newNodeId, NodeGene.NodeType.HIDDEN, 0.0));
        }

        int inToNewInn = tracker.getInnovation(oldConn.inNodeId, newNodeId);
        int newToOutInn = tracker.getInnovation(newNodeId, oldConn.outNodeId);

        connections.put(inToNewInn, new ConnectionGene(inToNewInn, oldConn.inNodeId, newNodeId, 1.0, true));
        connections.put(newToOutInn, new ConnectionGene(newToOutInn, newNodeId, oldConn.outNodeId, oldConn.weight, true));
    }

    private void mutateAddConnection(InnovationTracker tracker) {
        // ... (inchangé) ...
        List<NodeGene> nodeValues = new ArrayList<>(nodes.values());

        for (int i = 0; i < 50; i++) {
            NodeGene node1 = nodeValues.get(RANDOM.nextInt(nodeValues.size()));
            NodeGene node2 = nodeValues.get(RANDOM.nextInt(nodeValues.size()));

            if (node2.type == NodeGene.NodeType.INPUT || node1.type == NodeGene.NodeType.OUTPUT) {
                continue;
            }

            if (node1.id == node2.id) continue;

            boolean exists = false;
            for (ConnectionGene conn : connections.values()) {
                if (conn.inNodeId == node1.id && conn.outNodeId == node2.id) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                int inn = tracker.getInnovation(node1.id, node2.id);
                connections.put(inn, new ConnectionGene(inn, node1.id, node2.id, RANDOM.nextDouble() * 2 - 1, true));
                return;
            }
        }
    }

    private void mutateToggleEnable() {
        // ... (inchangé) ...
        if (connections.isEmpty()) return;
        List<ConnectionGene> possibleConns = new ArrayList<>(connections.values());
        ConnectionGene conn = possibleConns.get(RANDOM.nextInt(possibleConns.size()));
        conn.enabled = !conn.enabled;
    }

    // --- Getters et Setters (Corrigés la dernière fois) ---
    public Map<Integer, NodeGene> getNodes() { return nodes; }
    public Map<Integer, ConnectionGene> getConnections() { return connections; }

    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }

    public double getAdjustedFitness() { return adjustedFitness; }
    public void setAdjustedFitness(double fitness) { this.adjustedFitness = fitness; }

    public NeatGenome copy() {
        // ... (inchangé) ...
        NeatGenome newGenome = new NeatGenome();
        for (NodeGene node : nodes.values()) {
            newGenome.nodes.put(node.id, node.copy());
        }
        for (ConnectionGene conn : connections.values()) {
            newGenome.connections.put(conn.innovation, conn.copy());
        }
        newGenome.fitness = this.fitness;
        return newGenome;
    }
}