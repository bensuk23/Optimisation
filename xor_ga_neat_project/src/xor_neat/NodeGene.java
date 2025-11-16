package xor_neat;

// Fichier: NodeGene.java
import java.util.Objects;

public class NodeGene {
    public enum NodeType { INPUT, OUTPUT, HIDDEN, BIAS }

    public final int id;
    public double bias;
    public final NodeType type;

    public NodeGene(int id, NodeType type, double bias) {
        this.id = id;
        this.type = type;
        this.bias = bias;
    }

    public NodeGene copy() {
        return new NodeGene(id, type, bias);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeGene nodeGene = (NodeGene) o;
        return id == nodeGene.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}