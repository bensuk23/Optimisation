package xor_ga_classic;

import xor_common.CSVLogger; // CORRECTION : Import
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator; // CORRECTION : Import
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public class Main {

    // ... (Paramètres de l'Algorithme Génétique non changés) ...
    private static final int POPULATION_SIZE = 100;
    private static final int GENERATIONS = 500;
    private static final int ELITISM_COUNT = 1;
    private static final Random RANDOM = new Random();

    private static final String CSV_OUTPUT_FILE = "ga_progression_log.csv";

    public static void main(String[] args) {

        System.out.println("--- Algorithme Génétique Classique pour XOR (Java) ---");
        System.out.println("Taille du génome: " + NeuralNet.GENOME_LENGTH + " bits");
        System.out.println("Population: " + POPULATION_SIZE + ", Générations: " + GENERATIONS + "\n");

        // Initialisation du logger CSV
        CSVLogger logger = null;
        try {
            logger = new CSVLogger(CSV_OUTPUT_FILE);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation du logger CSV : " + e.getMessage());
            return;
        }

        // 1. Initialisation de la population
        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(new Individual());
        }

        Individual bestOverallIndividual = null;
        double maxFitnessOverall = -Double.MAX_VALUE;

        // 2. Boucle des Générations
        for (int gen = 1; gen <= GENERATIONS; gen++) {

            // ... (Code d'évaluation et de tri inchangé) ...
            for (Individual ind : population) {
                ind.calculateFitness();
            }
            population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());

            double currentMaxFitness = population.get(0).getFitness();
            if (currentMaxFitness > maxFitnessOverall) {
                maxFitnessOverall = currentMaxFitness;
                bestOverallIndividual = new Individual(population.get(0).getGenome());
                bestOverallIndividual.setFitness(maxFitnessOverall);
            }

            double avgFitness = population.stream().mapToDouble(Individual::getFitness).average().orElse(0.0);

            // --- Journalisation des données ---
            logger.logGeneration(gen, currentMaxFitness, avgFitness);

            // Affichage du progrès dans la console
            if (gen % 10 == 0 || gen == 1) { // Affiche moins souvent
                System.out.printf("Génération %4d | Max Fitness: %.6f | Avg Fitness: %.6f\n",
                        gen, currentMaxFitness, avgFitness);
            }

            if (maxFitnessOverall >= 3.99) {
                System.out.println("\n✅ Succès: Solution XOR trouvée à la génération " + gen);
                break;
            }

            // ... (Code de création de la prochaine population inchangé) ...
            List<Individual> nextPopulation = new ArrayList<>();
            // Elitisme
            for (int i = 0; i < ELITISM_COUNT; i++) {
                nextPopulation.add(population.get(i));
            }
            // Sélection, Croisement, Mutation
            while (nextPopulation.size() < POPULATION_SIZE) {
                Individual parent1 = selectionRoulette(population);
                Individual parent2 = selectionRoulette(population);
                Individual[] children = Individual.crossover(parent1, parent2);

                children[0].mutate();
                nextPopulation.add(children[0]);

                if (nextPopulation.size() < POPULATION_SIZE) {
                    children[1].mutate();
                    nextPopulation.add(children[1]);
                }
            }

            population = nextPopulation;
        }

        logger.close(); // Fermeture du fichier CSV

        // --- Affichage des Résultats Finaux ---
        if (bestOverallIndividual != null) {
            displayFinalResults(bestOverallIndividual);
            displayCSVContent(); // Affiche le contenu du CSV à la fin
        }
    }

    // ... (Fonction selectionRoulette inchangée) ...
    private static Individual selectionRoulette(List<Individual> population) {
        double totalFitness = population.stream().mapToDouble(Individual::getFitness).sum();
        // ... (Logique de sélection) ...
        if (totalFitness <= 1e-9) {
            return population.get(RANDOM.nextInt(population.size()));
        }
        double slice = RANDOM.nextDouble() * totalFitness;
        double cumulativeFitness = 0.0;
        for (Individual ind : population) {
            cumulativeFitness += ind.getFitness();
            if (cumulativeFitness >= slice) {
                return ind;
            }
        }
        return population.get(RANDOM.nextInt(population.size()));
    }

    // ... (Fonction displayFinalResults inchangée) ...
    private static void displayFinalResults(Individual best) {
        double[] bestParams = NeuralNet.decodeBitString(best.getGenome());

        System.out.println("\n--- Résultat Final (Meilleur Individu) ---");
        System.out.printf("Meilleure Fitness: %.6f\n", best.getFitness());
        System.out.println("Prédictions du Réseau (doivent être proches de 0 ou 1):");

        for (int i = 0; i < NeuralNet.XOR_DATA.length; i++) {
            double prediction = NeuralNet.predict(bestParams, NeuralNet.XOR_DATA[i]);
            double x1 = NeuralNet.XOR_DATA[i][0];
            double x2 = NeuralNet.XOR_DATA[i][1];
            double target = NeuralNet.XOR_TARGET[i];

            System.out.printf("  Entrée: (%.0f, %.0f) | Cible: %.0f | Sortie Prédite: %.4f\n",
                    x1, x2, target, prediction);
        }
    }

    // --- Nouvelle Fonction d'Affichage du CSV ---
    private static void displayCSVContent() {
        System.out.println("\n--- Aperçu de la Progression (CSV) ---");
        try (Stream<String> stream = Files.lines(Paths.get(CSV_OUTPUT_FILE))) {
            stream.limit(5).forEach(System.out::println);
            System.out.println("...");
        } catch (IOException e) {
            System.err.println("Impossible de lire le fichier CSV : " + e.getMessage());
        }
    }
}