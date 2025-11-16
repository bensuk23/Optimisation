package xor_ga_classic;
import java.util.Random;

public class Individual {

    private String genome;
    private double fitness;
    private static final Random RANDOM = new Random();

    // Constantes de la GA
    private static final double MUTATION_RATE = 0.01;
    private static final double CROSSOVER_RATE = 0.9;

    /**
     * Constructeur pour initialiser un individu aléatoirement.
     */
    public Individual() {
        this.genome = generateRandomGenome();
        this.fitness = 0.0;
    }

    /**
     * Constructeur pour créer un individu à partir d'un génome existant (pour le croisement).
     */
    public Individual(String genome) {
        this.genome = genome;
        this.fitness = 0.0;
    }

    // --- Méthodes d'Initialisation ---

    private String generateRandomGenome() {
        StringBuilder sb = new StringBuilder(NeuralNet.GENOME_LENGTH);
        for (int i = 0; i < NeuralNet.GENOME_LENGTH; i++) {
            sb.append(RANDOM.nextInt(2)); // 0 ou 1
        }
        return sb.toString();
    }

    // --- Opérateurs Génétiques ---

    /**
     * Croisement en un point (One-Point Crossover).
     */
    public static Individual[] crossover(Individual parent1, Individual parent2) {
        if (RANDOM.nextDouble() < CROSSOVER_RATE) {
            int crossoverPoint = RANDOM.nextInt(NeuralNet.GENOME_LENGTH - 2) + 1; // Point entre 1 et Longueur-1

            String genome1 = parent1.getGenome();
            String genome2 = parent2.getGenome();

            String childGenome1 = genome1.substring(0, crossoverPoint) + genome2.substring(crossoverPoint);
            String childGenome2 = genome2.substring(0, crossoverPoint) + genome1.substring(crossoverPoint);

            return new Individual[]{new Individual(childGenome1), new Individual(childGenome2)};
        } else {
            // Pas de croisement, les enfants sont des copies
            return new Individual[]{new Individual(parent1.getGenome()), new Individual(parent2.getGenome())};
        }
    }

    /**
     * Mutation par inversion de bit (Bit-Flip).
     */
    public void mutate() {
        char[] genomeChars = this.genome.toCharArray();
        for (int i = 0; i < genomeChars.length; i++) {
            if (RANDOM.nextDouble() < MUTATION_RATE) {
                // Inverser le bit: '0' devient '1' et '1' devient '0'
                genomeChars[i] = (genomeChars[i] == '0' ? '1' : '0');
            }
        }
        this.genome = new String(genomeChars);
    }

    // --- Évaluation et Getters/Setters ---

    public void calculateFitness() {
        this.fitness = NeuralNet.calculateFitness(this.genome);
    }

    public String getGenome() {
        return genome;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}