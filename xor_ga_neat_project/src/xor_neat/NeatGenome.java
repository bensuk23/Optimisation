package xor_neat;

import java.util.*;

public class NeatGenome {

    private static final Random RANDOM = new Random();

    private final Map<Integer, NodeGene> nodes = new HashMap<>();
    // TreeMap assure que les gènes sont triés par innovation pour le crossover/distance
    private final Map<Integer, ConnectionGene> connections = new TreeMap<>();

    private double fitness = 0.0;
    private double adjustedFitness = 0.0;

    /**
     * Constructeur initial
     */
    public NeatGenome(int inputCount, int outputCount, InnovationTracker tracker) {
        // Inputs
        for (int i = 0; i < inputCount; i++) {
            nodes.put(i, new NodeGene(i, NodeGene.NodeType.INPUT, 0.0));
        }
        // Biais (fixe à 1.0)
        nodes.put(inputCount, new NodeGene(inputCount, NodeGene.NodeType.BIAS, 1.0));

        // Outputs
        for (int i = 0; i < outputCount; i++) {
            int nodeId = inputCount + 1 + i;
            nodes.put(nodeId, new NodeGene(nodeId, NodeGene.NodeType.OUTPUT, 0.0));
        }

        // Connexions initiales complètes
        for (int i = 0; i <= inputCount; i++) {
            for (int j = 0; j < outputCount; j++) {
                int outNodeId = inputCount + 1 + j;
                double initialWeight = RANDOM.nextDouble() * 2 - 1;
                int innovation = tracker.getInnovation(i, outNodeId);
                connections.put(innovation, new ConnectionGene(innovation, i, outNodeId, initialWeight, true));
            }
        }
    }

    /**
     * Constructeur vide pour crossover
     */
    private NeatGenome() { }

    /**
     * Crossover standard NEAT
     */
    public static NeatGenome crossover(NeatGenome parent1, NeatGenome parent2) {
        NeatGenome fitter = (parent1.getFitness() >= parent2.getFitness()) ? parent1 : parent2;
        NeatGenome lessFit = (fitter == parent1) ? parent2 : parent1;

        NeatGenome child = new NeatGenome();

        // Copie des nœuds du parent le plus fort
        for (NodeGene node : fitter.getNodes().values()) {
            child.nodes.put(node.id, node.copy());
        }

        // Copie des connexions
        for (ConnectionGene connFitter : fitter.getConnections().values()) {
            ConnectionGene connLessFit = lessFit.getConnections().get(connFitter.innovation);

            if (connLessFit != null) {
                // Gène commun : héritage aléatoire + gestion enable
                ConnectionGene childGene = (RANDOM.nextBoolean()) ? connFitter.copy() : connLessFit.copy();
                if (!connFitter.enabled || !connLessFit.enabled) {
                    if (RANDOM.nextDouble() < 0.75) childGene.enabled = false;
                }
                child.connections.put(childGene.innovation, childGene);
            } else {
                // Gène excédentaire/disjoint : on hérite du fitter
                child.connections.put(connFitter.innovation, connFitter.copy());
            }
        }
        return child;
    }

    /**
     * Distance de compatibilité (Corrigée pour éviter l'explosion des espèces)
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


        // On normalise par la taille du génome SEULEMENT si le génome est grand (>20).
        // Pour XOR, N restera souvent à 1, ce qui est correct.
        int maxSize = Math.max(this.connections.size(), other.connections.size());
        double N = (maxSize < 20) ? 1.0 : maxSize;

        return (config.C1_EXCESS * excess / N) +
                (config.C2_DISJOINT * disjoint / N) +
                (config.C3_WEIGHTS * avgWeightDiff);
    }

    // --- MUTATIONS ---

    public void mutate(NeatConfig config, InnovationTracker tracker) {
        // Poids
        if (RANDOM.nextDouble() < config.MUTATE_WEIGHT_RATE) mutateWeights(config);

        // Biais (CRUCIAL POUR XOR)
        // Assure-toi d'avoir MUTATE_NODE_BIAS_RATE dans NeatConfig (ex: 0.1)
        // Si tu ne l'as pas, remplace config.MUTATE_NODE_BIAS_RATE par 0.1 ici.
        if (RANDOM.nextDouble() < 0.1) mutateNodeBias(config);

        // Ajout Nœud
        if (RANDOM.nextDouble() < config.MUTATE_ADD_NODE_RATE) mutateAddNode(tracker);

        // Ajout Connexion
        if (RANDOM.nextDouble() < config.MUTATE_ADD_CONNECTION_RATE) mutateAddConnection(tracker);

        // Toggle
        if (RANDOM.nextDouble() < config.MUTATE_TOGGLE_ENABLE_RATE) mutateToggleEnable();
    }

    private void mutateWeights(NeatConfig config) {
        for (ConnectionGene conn : connections.values()) {
            if (RANDOM.nextDouble() < config.WEIGHT_PERTURB_RATE) {
                conn.weight += (RANDOM.nextGaussian() * config.WEIGHT_PERTURB_STD_DEV);
            } else {
                conn.weight = RANDOM.nextDouble() * 4 - 2;
            }
        }
    }

    private void mutateNodeBias(NeatConfig config) {
        for (NodeGene node : nodes.values()) {
            // On ne touche pas aux inputs bruts ni au Bias Node (ID 2)
            if (node.type == NodeGene.NodeType.INPUT || node.type == NodeGene.NodeType.BIAS) continue;

            // Même logique que poids
            if (RANDOM.nextDouble() < config.WEIGHT_PERTURB_RATE) {
                // Utilise un écart type de 0.2 par défaut si non défini
                node.bias += (RANDOM.nextGaussian() * 0.2);
            } else {
                node.bias = RANDOM.nextDouble() * 4 - 2;
            }
        }
    }

    private void mutateAddNode(InnovationTracker tracker) {
        if (connections.isEmpty()) return;

        // Trouve une connexion active
        List<ConnectionGene> possibleConns = new ArrayList<>();
        for(ConnectionGene c : connections.values()) if(c.enabled) possibleConns.add(c);
        if (possibleConns.isEmpty()) return;

        ConnectionGene oldConn = possibleConns.get(RANDOM.nextInt(possibleConns.size()));
        oldConn.enabled = false; // Désactive l'ancienne

        int newNodeId = tracker.getNewNodeId(oldConn.innovation);

        // Si le nœud est nouveau, on l'initialise avec 0.0 bias (sera muté plus tard)
        if (!nodes.containsKey(newNodeId)) {
            nodes.put(newNodeId, new NodeGene(newNodeId, NodeGene.NodeType.HIDDEN, 0.0));
        }

        int inToNewInn = tracker.getInnovation(oldConn.inNodeId, newNodeId);
        int newToOutInn = tracker.getInnovation(newNodeId, oldConn.outNodeId);

        connections.put(inToNewInn, new ConnectionGene(inToNewInn, oldConn.inNodeId, newNodeId, 1.0, true));
        connections.put(newToOutInn, new ConnectionGene(newToOutInn, newNodeId, oldConn.outNodeId, oldConn.weight, true));
    }

    private void mutateAddConnection(InnovationTracker tracker) {
        List<NodeGene> nodeValues = new ArrayList<>(nodes.values());

        for (int i = 0; i < 20; i++) { // 20 tentatives
            NodeGene node1 = nodeValues.get(RANDOM.nextInt(nodeValues.size()));
            NodeGene node2 = nodeValues.get(RANDOM.nextInt(nodeValues.size()));

            // Règles de validité basiques
            if (node1.type == NodeGene.NodeType.OUTPUT) continue;
            if (node2.type == NodeGene.NodeType.INPUT || node2.type == NodeGene.NodeType.BIAS) continue;
            if (node1.id == node2.id) continue;

            // Vérifie si la connexion existe déjà
            boolean exists = false;
            for (ConnectionGene conn : connections.values()) {
                if (conn.inNodeId == node1.id && conn.outNodeId == node2.id) {
                    exists = true;
                    break;
                }
            }
            if (exists) continue;

            // --- DETECTION DE CYCLES ---
            // Si ajouter cette connexion crée une boucle, on abandonne immédiatement.
            // Sinon le réseau plante et renvoie 0.0000.
            if (createsCycle(node1.id, node2.id)) {
                continue;
            }

            int inn = tracker.getInnovation(node1.id, node2.id);
            connections.put(inn, new ConnectionGene(inn, node1.id, node2.id, RANDOM.nextDouble() * 2 - 1, true));
            return; // Succès
        }
    }

    /**
     * Vérifie si ajouter une connexion de sourceId -> targetId crée un cycle.
     * Utilise un parcours DFS.
     */
    private boolean createsCycle(int sourceId, int targetId) {
        // Si targetId peut déjà atteindre sourceId, alors ajouter sourceId->targetId fermera la boucle.
        if (sourceId == targetId) return true;

        Set<Integer> visited = new HashSet<>();
        Stack<Integer> stack = new Stack<>();
        stack.push(targetId);
        visited.add(targetId);

        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (current == sourceId) return true; // Cycle trouvé

            for (ConnectionGene conn : connections.values()) {
                // On suit le chemin même si la connexion est désactivée (structurellement risqué)
                if (conn.inNodeId == current) {
                    if (!visited.contains(conn.outNodeId)) {
                        visited.add(conn.outNodeId);
                        stack.push(conn.outNodeId);
                    }
                }
            }
        }
        return false;
    }

    private void mutateToggleEnable() {
        if (connections.isEmpty()) return;
        List<ConnectionGene> allConns = new ArrayList<>(connections.values());
        ConnectionGene conn = allConns.get(RANDOM.nextInt(allConns.size()));
        conn.enabled = !conn.enabled;
    }

    // Getters & Setters
    public Map<Integer, NodeGene> getNodes() { return nodes; }
    public Map<Integer, ConnectionGene> getConnections() { return connections; }
    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }
    public double getAdjustedFitness() { return adjustedFitness; }
    public void setAdjustedFitness(double fitness) { this.adjustedFitness = fitness; }

    public NeatGenome copy() {
        NeatGenome newGenome = new NeatGenome();
        for (NodeGene node : nodes.values()) newGenome.nodes.put(node.id, node.copy());
        for (ConnectionGene conn : connections.values()) newGenome.connections.put(conn.innovation, conn.copy());
        newGenome.fitness = this.fitness;
        newGenome.adjustedFitness = this.adjustedFitness;
        return newGenome;
    }
}