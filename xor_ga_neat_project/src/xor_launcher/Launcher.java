package xor_launcher;

import xor_ga_classic.Main;
import xor_neat.NeatXorSolver;

import javax.swing.*;
import java.awt.*;

public class Launcher {

    private JTextArea logArea;
    private JButton gaButton;
    private JButton neatButton;

    public Launcher() {
        // 1. Créer la fenêtre principale
        JFrame frame = new JFrame("Lanceur de Projet XOR (GA vs NEAT)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 350);
        frame.setLocationRelativeTo(null); // Centrer

        // 2. Créer le panneau des boutons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        gaButton = new JButton("Lancer GA Classique");
        neatButton = new JButton("Lancer NEAT");
        buttonPanel.add(gaButton);
        buttonPanel.add(neatButton);

        // 3. Créer la zone de log (pour voir ce qui se passe)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        logArea.append("Veuillez choisir une application à lancer.\n");
        logArea.append("La sortie détaillée apparaîtra dans la console de votre IDE.\n");

        // 4. Ajouter les panneaux à la fenêtre
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // 5. Définir les actions des boutons

        // Action pour le bouton GA
        gaButton.addActionListener(e -> runGA());

        // Action pour le bouton NEAT
        neatButton.addActionListener(e -> runNEAT());

        // Afficher la fenêtre
        frame.setVisible(true);
    }

    /**
     * Lance l'application GA dans un thread séparé pour ne pas geler l'interface.
     */
    private void runGA() {
        logToGui("Lancement du GA Classique... (Vérifiez la console pour la progression)");
        setButtonsEnabled(false);

        // SwingWorker est la bonne façon de lancer des tâches longues en arrière-plan
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Appelle la méthode main() de votre classe GA
                Main.main(new String[0]);
                return null;
            }

            @Override
            protected void done() {
                logToGui("--- GA Classique Terminé ---");
                logToGui("Fichier 'ga_progression_log.csv' créé/mis à jour.\n");
                setButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    /**
     * Lance l'application NEAT dans un thread séparé.
     */
    private void runNEAT() {
        logToGui("Lancement de NEAT... (Vérifiez la console pour la progression)");
        setButtonsEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Appelle la méthode main() de votre classe NEAT
                NeatXorSolver.main(new String[0]);
                return null;
            }

            @Override
            protected void done() {
                logToGui("--- NEAT Terminé ---");
                logToGui("Fichier 'neat_progression_log_complet.csv' créé/mis à jour.\n");
                setButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    // --- Méthodes Utilitaires ---

    private void setButtonsEnabled(boolean enabled) {
        gaButton.setEnabled(enabled);
        neatButton.setEnabled(enabled);
    }

    private void logToGui(String message) {
        // S'assure que la mise à jour de l'interface se fait sur le bon thread
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Fait défiler automatiquement vers le bas
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Point d'entrée principal pour le lanceur GUI.
     */
    public static void main(String[] args) {
        // Assure que l'interface Swing est créée sur le "Event Dispatch Thread"
        SwingUtilities.invokeLater(() -> new Launcher());
    }
}