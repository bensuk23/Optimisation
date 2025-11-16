package xor_neat_project;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Représente un Génome NEAT simplifié (noeuds et connexions) capable d'évoluer.
 */
public class NeatGenome {

    // Structure du réseau
    public static final int INPUT_NODES = 3; // x1, x2, Biais
    public static final int OUTPUT_NODES = 1;
    private static final Random RANDOM = new Random();

    // Propriétés du Génome
    private double fitness = 0.0;
    private int nextNodeId = INPUT_NODES + OUTPUT_NODES;

    private List<Node> nodes;
    private List<Connection> connections;

    // --- Classes Internes Correctes (PUBLIC STATIC) ---

    public static class Node {
        public int id; // <--- PUBLIC pour l'accès
        public double bias; // <--- PUBLIC pour l'accès
        public Node(int id, double bias) { this.id = id; this.bias = bias; }
    }

    public static class Connection {
        public int inNodeId; // <--- PUBLIC pour l'accès
        public int outNodeId; // <--- PUBLIC pour l'accès
        public double weight; // <--- PUBLIC pour l'accès
        public boolean enabled = true; // <--- PUBLIC pour l'accès
        public Connection(int in, int out, double w) { this.inNodeId = in; this.outNodeId = out; this.weight = w; }
    }

    // --- 1. Constructeur : Réseau Minimal de Départ ---

    public NeatGenome() {
        nodes = new ArrayList<>();
        connections = new ArrayList<>();

        for (int i = 0; i < INPUT_NODES; i++) {
            nodes.add(new Node(i, 0.0));
        }
        for (int i = 0; i < OUTPUT_NODES; i++) {
            nodes.add(new Node(INPUT_NODES + i, 0.0));
        }

        for (int i = 0; i < INPUT_NODES; i++) {
            for (int j = 0; j < OUTPUT_NODES; j++) {
                connections.add(new Connection(i, INPUT_NODES + j, RANDOM.nextDouble() * 2 - 1));
            }
        }
    }

    // --- 2. Mutations de Complexification (Simulées) ---

    public void mutateAddConnection() {
        if (RANDOM.nextDouble() < 0.05) {
            int inNode = RANDOM.nextInt(nodes.size());
            int outNode = RANDOM.nextInt(nodes.size());

            if (inNode != outNode && inNode != 2) {
                connections.add(new Connection(nodes.get(inNode).id, nodes.get(outNode).id, RANDOM.nextDouble() * 2 - 1));
            }
        }
    }

    public void mutateAddNode() {
        if (RANDOM.nextDouble() < 0.03 && !connections.isEmpty()) {
            Connection oldConn = connections.get(RANDOM.nextInt(connections.size()));
            oldConn.enabled = false;

            Node newNode = new Node(nextNodeId++, RANDOM.nextDouble() * 2 - 1);
            nodes.add(newNode);

            connections.add(new Connection(oldConn.inNodeId, newNode.id, 1.0));
            connections.add(new Connection(newNode.id, oldConn.outNodeId, oldConn.weight));
        }
    }

    // --- 3. Mutation de Poids ---

    public void mutateWeights() {
        for (Connection conn : connections) {
            if (RANDOM.nextDouble() < 0.9) {
                conn.weight += (RANDOM.nextDouble() * 0.4) - 0.2;
            }
        }
    }

    // --- Getters et Setters ---

    public List<Node> getNodes() { return nodes; }
    public List<Connection> getConnections() { return connections; }
    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }
}