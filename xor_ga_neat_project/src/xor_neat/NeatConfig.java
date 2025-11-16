package xor_neat; // CORRECTION : Package

// Fichier: NeatConfig.java
// Contient tous les hyperparamètres pour l'algorithme NEAT.
public class NeatConfig {

    // --- Population et Évolution ---
    public static final int POPULATION_SIZE = 150;
    public static final int MAX_GENERATIONS = 1000;

    // CORRECTION : Le seuil doit être au carré (3.99 * 3.99 = 15.9201)
    // car le fitness est mis au carré dans NeatXorSolver.
    public static final double FITNESS_THRESHOLD = 15.9201;

    // --- Taux de Mutation ---
    public static final double MUTATE_WEIGHT_RATE = 0.8;      // Taux de mutation des poids
    public static final double WEIGHT_PERTURB_RATE = 0.9;     // 90% de chance de perturber, 10% de nouveau poids
    public static final double WEIGHT_PERTURB_STD_DEV = 0.2;  // Écart-type pour la perturbation
    public static final double MUTATE_ADD_NODE_RATE = 0.03;   // Taux d'ajout de nœud
    public static final double MUTATE_ADD_CONNECTION_RATE = 0.05; // Taux d'ajout de connexion
    public static final double MUTATE_TOGGLE_ENABLE_RATE = 0.01;  // Taux de réactivation/désactivation

    // --- Spéciation et Distance de Compatibilité ---
    public static final double C1_EXCESS = 1.0;  // Coefficient pour les gènes en excès
    public static final double C2_DISJOINT = 1.0; // Coefficient pour les gènes disjoints
    public static final double C3_WEIGHTS = 0.4;  // Coefficient pour la différence de poids

    public static final double COMPATIBILITY_THRESHOLD = 3.0; // Seuil de distance pour la spéciation

    // --- Reproduction ---
    public static final double ELITISM = 0.05; // 5% des meilleurs individus par espèce sont conservés
    public static final double CROSSOVER_RATE = 0.75; // 75% des enfants proviennent du crossover
    public static final int STAGNATION_THRESHOLD = 15; // Générations sans amélioration avant pénalité
}