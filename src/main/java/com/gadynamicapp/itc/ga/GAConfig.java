package com.gadynamicapp.itc.ga;

public record GAConfig(
        int populationSize,
        int generations,
        int tournamentSize,
        double mutationRate,
        int elitismCount,
        long seed
) {
    public static final int DEFAULT_POPULATION_SIZE = 50;
    public static final int DEFAULT_GENERATIONS = 100;
    public static final int DEFAULT_TOURNAMENT_SIZE = 3;
    public static final double DEFAULT_MUTATION_RATE = 0.05;
    public static final int DEFAULT_ELITISM_COUNT = 2;

    public GAConfig {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("populationSize must be greater than 0.");
        }
        if (generations < 0) {
            throw new IllegalArgumentException("generations must be non-negative.");
        }
        if (tournamentSize <= 0) {
            throw new IllegalArgumentException("tournamentSize must be greater than 0.");
        }
        if (Double.isNaN(mutationRate) || mutationRate < 0.0 || mutationRate > 1.0) {
            throw new IllegalArgumentException("mutationRate must be between 0.0 and 1.0.");
        }
        if (elitismCount < 0) {
            throw new IllegalArgumentException("elitismCount must be non-negative.");
        }
        elitismCount = Math.min(elitismCount, populationSize);
    }

    public static GAConfig defaults(long seed) {
        return new GAConfig(
                DEFAULT_POPULATION_SIZE,
                DEFAULT_GENERATIONS,
                DEFAULT_TOURNAMENT_SIZE,
                DEFAULT_MUTATION_RATE,
                DEFAULT_ELITISM_COUNT,
                seed
        );
    }
}
