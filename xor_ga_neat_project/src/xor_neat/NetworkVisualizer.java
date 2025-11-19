package xor_neat;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkVisualizer extends JFrame {

    private NeuralPanel panel;

    public NetworkVisualizer() {
        super("NEAT XOR - Visualisation Temps Réel");
        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        // On initialise avec un panneau vide
        this.panel = new NeuralPanel();
        this.add(panel);
        this.setVisible(true);
    }

    /**
     * Met à jour le dessin avec le nouveau meilleur génome.
     */
    public void updateGenome(NeatGenome genome, int generation) {
        this.setTitle("NEAT XOR - Génération: " + generation + " | Fitness: " + String.format("%.4f", genome.getFitness()));
        panel.setGenome(genome);
        panel.repaint(); // Force le redessin immédiat
    }

    // --- Panneau de Dessin Interne ---
    private class NeuralPanel extends JPanel {
        private NeatGenome genome;
        private Map<Integer, Point> nodePositions = new HashMap<>();

        public void setGenome(NeatGenome genome) {
            // On fait une copie pour éviter les problèmes de threads
            this.genome = genome;
            calculatePositions();
        }

        private void calculatePositions() {
            if (genome == null) return;

            nodePositions.clear();
            int width = getWidth();
            int height = getHeight();
            int padding = 50;

            List<NodeGene> inputs = new ArrayList<>();
            List<NodeGene> outputs = new ArrayList<>();
            List<NodeGene> hidden = new ArrayList<>();

            for (NodeGene n : genome.getNodes().values()) {
                if (n.type == NodeGene.NodeType.INPUT || n.type == NodeGene.NodeType.BIAS) inputs.add(n);
                else if (n.type == NodeGene.NodeType.OUTPUT) outputs.add(n);
                else hidden.add(n);
            }

            // Inputs (Gauche)
            int yStep = height / (inputs.size() + 1);
            for (int i = 0; i < inputs.size(); i++) {
                nodePositions.put(inputs.get(i).id, new Point(padding, yStep * (i + 1)));
            }

            // Outputs (Droite)
            yStep = height / (outputs.size() + 1);
            for (int i = 0; i < outputs.size(); i++) {
                nodePositions.put(outputs.get(i).id, new Point(width - padding, yStep * (i + 1)));
            }

            // Hidden (Au milieu - Disposition en grille simple)
            if (!hidden.isEmpty()) {
                // On sépare l'espace central en colonnes virtuelles basées sur l'ID (approximation de la profondeur)
                int areaWidth = width - (2 * padding) - 100;
                int startX = padding + 50;

                for (int i = 0; i < hidden.size(); i++) {
                    NodeGene node = hidden.get(i);
                    // Pseudo-aléatoire stable basé sur l'ID pour que le noeud ne saute pas partout
                    int x = startX + (Math.abs(node.id * 137) % areaWidth);
                    int y = 50 + (Math.abs(node.id * 53) % (height - 100));
                    nodePositions.put(node.id, new Point(x, y));
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Efface l'écran
            if (genome == null) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Connexions
            for (ConnectionGene conn : genome.getConnections().values()) {
                if (!conn.enabled) continue;

                Point p1 = nodePositions.get(conn.inNodeId);
                Point p2 = nodePositions.get(conn.outNodeId);

                if (p1 != null && p2 != null) {
                    float thickness = (float) Math.abs(conn.weight);
                    thickness = Math.min(thickness, 6.0f);
                    thickness = Math.max(thickness, 1.0f);

                    g2.setStroke(new BasicStroke(thickness));
                    // Vert = Positif, Rouge = Négatif
                    if (conn.weight > 0) g2.setColor(new Color(0, 200, 0, 180));
                    else g2.setColor(new Color(200, 0, 0, 180));

                    g2.draw(new Line2D.Float(p1, p2));
                }
            }

            // 2. Noeuds
            for (NodeGene node : genome.getNodes().values()) {
                Point p = nodePositions.get(node.id);
                if (p == null) continue;

                int r = 24; // Taille du noeud
                // Couleurs selon le type
                if (node.type == NodeGene.NodeType.INPUT || node.type == NodeGene.NodeType.BIAS) g2.setColor(new Color(100, 255, 100));
                else if (node.type == NodeGene.NodeType.OUTPUT) g2.setColor(new Color(255, 100, 100));
                else g2.setColor(new Color(100, 100, 255));

                g2.fillOval(p.x - r/2, p.y - r/2, r, r);

                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(p.x - r/2, p.y - r/2, r, r);

                // Texte (ID et Biais)
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.drawString("" + node.id, p.x - 5, p.y + 5);

                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                if(node.type != NodeGene.NodeType.INPUT) {
                    g2.drawString(String.format("%.2f", node.bias), p.x - 10, p.y - 15);
                }
            }
        }
    }
}