package com.gadynamicapp.itc.experiment;

import java.nio.file.Path;
import java.util.Objects;

public record ExperimentRunOutput(
        Path runDirectory,
        Path runConfig,
        Path problemSummary,
        Path finalFitnessBreakdown,
        Path convergence,
        Path bestSolutionAssignments,
        Path constraintViolations
) {
    public ExperimentRunOutput {
        Objects.requireNonNull(runDirectory, "runDirectory");
        Objects.requireNonNull(runConfig, "runConfig");
        Objects.requireNonNull(problemSummary, "problemSummary");
        Objects.requireNonNull(finalFitnessBreakdown, "finalFitnessBreakdown");
        Objects.requireNonNull(convergence, "convergence");
        Objects.requireNonNull(bestSolutionAssignments, "bestSolutionAssignments");
        Objects.requireNonNull(constraintViolations, "constraintViolations");
    }
}