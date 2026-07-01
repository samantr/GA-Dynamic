package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.experiment.ExperimentRunOutput;
import com.gadynamicapp.itc.fitness.FitnessSummaryService;

import java.io.PrintStream;
import java.util.Locale;

public final class GASummaryService {

    public void print(PrintStream out, GAResult result, ExperimentRunOutput output) {
        out.println("Standard Genetic Algorithm Result");
        out.println("Initial best score: " + result.initialBestScore());
        out.println();

        out.println("Best score checkpoints:");
        for (GAHistoryEntry entry : result.history()) {
            if (entry.generation() > 0
                    && (entry.generation() % 10 == 0 || entry.generation() == result.config().generations())) {
                out.println(
                        " Generation " + entry.generation()
                                + ": bestScore=" + entry.bestScore()
                                + ", averageScore=" + String.format(Locale.ROOT, "%.2f", entry.averageScore())
                );
            }
        }

        out.println();
        out.println("Final best score: " + result.bestFitness().totalScore());
        out.println("Final hard violation count: " + result.bestFitness().hardViolationCount());
        out.println("Final soft penalty: " + result.bestFitness().softPenaltyTotal());
        out.println("Runtime seconds: " + String.format(Locale.ROOT, "%.2f", result.runtimeSeconds()));
        out.println("Run output directory: " + output.runDirectory());
        out.println("Convergence history: " + output.convergence());
        out.println("Best solution assignments: " + output.bestSolutionAssignments());
        out.println("Constraint violations: " + output.constraintViolations());

        out.println();
        out.println("Final fitness breakdown:");
        new FitnessSummaryService().print(out, result.bestFitness());
    }
}