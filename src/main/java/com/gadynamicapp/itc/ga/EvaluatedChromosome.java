package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.fitness.FitnessResult;
import com.gadynamicapp.itc.solution.TimetableSolution;

import java.util.Objects;

public record EvaluatedChromosome(
        Chromosome chromosome,
        TimetableSolution solution,
        FitnessResult fitnessResult
) {
    public EvaluatedChromosome {
        Objects.requireNonNull(chromosome, "chromosome");
        Objects.requireNonNull(solution, "solution");
        Objects.requireNonNull(fitnessResult, "fitnessResult");
    }
}
