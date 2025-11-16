package xor_neat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Species {

    private static final Random RANDOM = new Random();

    private NeatGenome representative;
    private final List<NeatGenome> members = new ArrayList<>();
    private double totalAdjustedFitness = 0.0;
    private int generationsSinceImprovement = 0;

    public Species(NeatGenome firstMember) {
        this.representative = firstMember;
        this.members.add(firstMember);
    }

    public boolean addMember(NeatGenome genome, NeatConfig config) {
        double distance = representative.compatibilityDistance(genome, config);
        if (distance <= config.COMPATIBILITY_THRESHOLD) {
            members.add(genome);
            return true;
        }
        return false;
    }

    /**
     * Calcule le "fitness ajusté" pour chaque membre, basé sur la taille de l'espèce.
     * C'est le "Fitness Sharing" de NEAT.
     */
    public void adjustFitness() {
        if (members.isEmpty()) {
            totalAdjustedFitness = 0.0;
            return;
        }

        totalAdjustedFitness = 0.0;
        for (NeatGenome member : members) {

            // On lit le VRAI fitness (getFitness())
            double adjustedFitness = member.getFitness() / members.size();

            // On écrit dans le NOUVEAU champ (setAdjustedFitness())
            // LE VRAI FITNESS N'EST PLUS ÉCRASÉ
            member.setAdjustedFitness(adjustedFitness);

            totalAdjustedFitness += adjustedFitness;
        }
    }

    /**
     * Prépare l'espèce pour la prochaine génération.
     */
    public void prepareForReproduction(NeatConfig config) {
        // Trie les membres sur la base de leur VRAI fitness
        members.sort(Comparator.comparingDouble(NeatGenome::getFitness).reversed());

        // (Logique de stagnation... toujours pas implémentée)

        // Choisir un nouveau représentant au hasard
        if (!members.isEmpty()) {
            representative = members.get(RANDOM.nextInt(members.size()));
        }
    }

    /**
     * Crée un enfant pour cette espèce.
     */
    public NeatGenome createOffspring(NeatConfig config, InnovationTracker tracker) {
        NeatGenome child;

        if (RANDOM.nextDouble() < config.CROSSOVER_RATE && members.size() > 1) {
            // Crossover
            NeatGenome p1 = selectMember();
            NeatGenome p2 = selectMember();
            // Crossover utilise le VRAI fitness (corrigé dans NeatGenome)
            child = NeatGenome.crossover(p1, p2);
        } else {
            // Mutation simple (clonage)
            child = selectMember().copy();
        }

        child.mutate(config, tracker);
        return child;
    }

    // Sélection d'un parent (basée sur le VRAI fitness)
    private NeatGenome selectMember() {
        if (members.isEmpty()) {
            throw new RuntimeException("Erreur : Tentative de sélection dans une espèce vide.");
        }
        // Sélection par tournoi (taille 2)
        NeatGenome p1 = members.get(RANDOM.nextInt(members.size()));
        NeatGenome p2 = members.get(RANDOM.nextInt(members.size()));

        // Compare le VRAI fitness
        return (p1.getFitness() >= p2.getFitness()) ? p1 : p2;
    }

    public List<NeatGenome> getMembers() {
        return members;
    }

    public double getTotalAdjustedFitness() {
        return totalAdjustedFitness;
    }

    public void clear() {
        members.clear();
    }
}