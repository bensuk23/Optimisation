package xor_neat;

import java.util.Objects;

public class ConnectionGene {
    public final int innovation;
    public final int inNodeId;
    public final int outNodeId;
    public double weight;
    public boolean enabled;

    public ConnectionGene(int innovation, int in, int out, double w, boolean en) {
        this.innovation = innovation;
        this.inNodeId = in;
        this.outNodeId = out;
        this.weight = w;
        this.enabled = en;
    }

    public ConnectionGene copy() {
        return new ConnectionGene(innovation, inNodeId, outNodeId, weight, enabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionGene that = (ConnectionGene) o;
        return innovation == that.innovation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(innovation);
    }
}