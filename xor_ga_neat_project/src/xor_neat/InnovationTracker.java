package xor_neat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Fichier: InnovationTracker.java
// Suit les innovations (nouveaux gènes) pour assurer un croisement correct.
public class InnovationTracker {

    private int nextInnovationNumber = 0;
    private int nextNodeId = 0;

    // Map pour suivre les innovations de connexion (inNode, outNode) -> innovationId
    private final Map<ConnectionKey, Integer> connectionInnovations = new HashMap<>();
    
    // Map pour suivre les innovations de nœud (basées sur l'innovation de la connexion qu'ils divisent)
    private final Map<Integer, Integer> nodeInnovations = new HashMap<>();

    public InnovationTracker(int initialNodeCount) {
        this.nextNodeId = initialNodeCount;
    }

    /**
     * Obtient ou crée un numéro d'innovation pour une nouvelle connexion.
     */
    public int getInnovation(int inNodeId, int outNodeId) {
        ConnectionKey key = new ConnectionKey(inNodeId, outNodeId);
        if (connectionInnovations.containsKey(key)) {
            return connectionInnovations.get(key);
        } else {
            int innovation = nextInnovationNumber++;
            connectionInnovations.put(key, innovation);
            return innovation;
        }
    }

    /**
     * Obtient ou crée un ID de nœud pour une nouvelle mutation de nœud.
     * Le "splitInnovationId" est l'ID d'innovation de la connexion qui est divisée.
     */
    public int getNewNodeId(int splitInnovationId) {
        if (nodeInnovations.containsKey(splitInnovationId)) {
            return nodeInnovations.get(splitInnovationId);
        } else {
            int newNodeId = nextNodeId++;
            nodeInnovations.put(splitInnovationId, newNodeId);
            return newNodeId;
        }
    }
    
    public int getNextNodeId() {
        return nextNodeId++;
    }

    // Classe helper pour la clé de la map
    private static class ConnectionKey {
        int in;
        int out;
        public ConnectionKey(int in, int out) { this.in = in; this.out = out; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConnectionKey that = (ConnectionKey) o;
            return in == that.in && out == that.out;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(in, out);
        }
    }
}