package com.gadynamicapp.itc.fitness;

import java.util.List;
import java.util.Objects;

public record FitnessResult(
        long totalScore,
        int hardViolationCount,
        long softPenaltyTotal,
        FitnessBreakdown breakdown,
        List<ConstraintViolation> violations
) {
    public FitnessResult {
        Objects.requireNonNull(breakdown, "breakdown");
        Objects.requireNonNull(violations, "violations");
        violations = List.copyOf(violations);
    }
}
