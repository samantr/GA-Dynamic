package com.gadynamicapp.itc.ga;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record Population(List<EvaluatedChromosome> individuals) {
    public Population {
        Objects.requireNonNull(individuals, "individuals");
        if (individuals.isEmpty()) {
            throw new IllegalArgumentException("Population must contain at least one individual.");
        }
        individuals = individuals.stream()
                .sorted(Comparator.comparingLong(individual -> individual.fitnessResult().totalScore()))
                .toList();
    }

    public EvaluatedChromosome best() {
        return individuals.get(0);
    }

    public List<EvaluatedChromosome> elites(int count) {
        return individuals.subList(0, Math.min(count, individuals.size()));
    }

    public double averageScore() {
        return individuals.stream()
                .mapToLong(individual -> individual.fitnessResult().totalScore())
                .average()
                .orElse(0.0);
    }
}
