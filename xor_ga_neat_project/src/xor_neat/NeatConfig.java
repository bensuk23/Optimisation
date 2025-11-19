package xor_neat;

public class NeatConfig {
    // Population
    public static final int POPULATION_SIZE = 150;
    public static final int MAX_GENERATIONS = 1000;
    public static final double FITNESS_THRESHOLD = 15.9; // 3.99^2

    // Taux de Mutation (Ajustés d'après NEAT-Python)
    public static final double MUTATE_WEIGHT_RATE = 0.8;
    public static final double WEIGHT_PERTURB_RATE = 0.9;
    public static final double WEIGHT_PERTURB_STD_DEV = 2.5; // Augmenté car sigmoid raide demande plus de variance
    public static final double MUTATE_NODE_BIAS_RATE = 0.1; // Important

    public static final double MUTATE_ADD_NODE_RATE = 0.03;
    public static final double MUTATE_ADD_CONNECTION_RATE = 0.05;
    public static final double MUTATE_TOGGLE_ENABLE_RATE = 0.01;

    // Spéciation
    public static final double C1_EXCESS = 1.0;
    public static final double C2_DISJOINT = 1.0;
    public static final double C3_WEIGHTS = 0.4; // Poids moins important que la structure


    public static final double COMPATIBILITY_THRESHOLD = 4.0;

    public static final double ELITISM = 0.1; // Garder le top 10%
    public static final double CROSSOVER_RATE = 0.75;
}