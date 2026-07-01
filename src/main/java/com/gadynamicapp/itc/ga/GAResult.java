package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.fitness.FitnessResult;
import com.gadynamicapp.itc.solution.TimetableSolution;

import java.util.List;
import java.util.Objects;

public record GAResult(
        GAConfig config,
        long initialBestScore,
        TimetableSolution bestSolution,
        FitnessResult bestFitness,
        List<GAHistoryEntry> history,
        long runtimeMillis
) {
    public GAResult {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(bestSolution, "bestSolution");
        Objects.requireNonNull(bestFitness, "bestFitness");
        Objects.requireNonNull(history, "history");
        history = List.copyOf(history);
    }

    public double runtimeSeconds() {
        return runtimeMillis / 1000.0;
    }
}
