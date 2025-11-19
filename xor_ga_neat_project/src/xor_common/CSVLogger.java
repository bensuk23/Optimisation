package xor_common;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

public class CSVLogger implements java.lang.AutoCloseable {

    private final String filename;
    private PrintWriter writer;

    /**
     * Initialise le logger et écrit l'en-tête du fichier CSV.
     */
    public CSVLogger(String filename) throws IOException {
        this.filename = filename;
        this.writer = new PrintWriter(new FileWriter(this.filename, false));

        // En-tête du CSV
        writer.println("Generation,MaxFitness,AvgFitness");
        writer.flush();
    }

    /**
     * Ajoute une ligne de données (une génération) au fichier CSV.
     */
    public void logGeneration(int generation, double maxFitness, double avgFitness) {
        writer.printf(Locale.US, "%d,%.6f,%.6f\n", generation, maxFitness, avgFitness);
    }

    /**
     * Ferme le PrintWriter. DOIT être appelé à la fin du programme.
     */
    @Override // Ajouté pour AutoCloseable
    public void close() {
        if (writer != null) {
            writer.close();
        }
        System.out.println("Fichier de progression CSV enregistré dans : " + filename);
    }
}