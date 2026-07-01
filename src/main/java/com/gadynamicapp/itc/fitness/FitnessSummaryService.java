package com.gadynamicapp.itc.fitness;

import java.io.PrintStream;

public final class FitnessSummaryService {
    private static final int DEFAULT_VIOLATION_LIMIT = 20;

    public void print(PrintStream out, FitnessResult result) {
        print(out, result, DEFAULT_VIOLATION_LIMIT);
    }

    public void print(PrintStream out, FitnessResult result, int violationLimit) {
        FitnessBreakdown breakdown = result.breakdown();

        out.println("Fitness Breakdown");
        out.println("Total score: " + result.totalScore());
        out.println("Hard violation count: " + result.hardViolationCount());
        out.println("Soft penalty total: " + result.softPenaltyTotal());
        out.println();

        out.println("Choice penalties:");
        out.println("  Time choice penalty: " + breakdown.timeChoicePenalty());
        out.println("  Room choice penalty: " + breakdown.roomChoicePenalty());
        out.println();

        out.println("Hard constraints:");
        out.println("  Room conflict violations: " + breakdown.roomConflictViolationCount());
        out.println("  Room unavailable violations: " + breakdown.roomUnavailableViolationCount());
        out.println("  Required distribution violations: " + breakdown.requiredDistributionViolationCount());
        out.println("  Unsupported required distributions: " + breakdown.unsupportedRequiredDistributionCount());
        out.println();

        out.println("Unsupported soft constraints:");
        out.println("  Unsupported non-required distributions: " + breakdown.unsupportedNonRequiredDistributionCount());
        out.println();

        int visibleCount = Math.min(violationLimit, result.violations().size());
        out.println("Constraint reports shown: " + visibleCount + " of " + result.violations().size());
        for (int i = 0; i < visibleCount; i++) {
            ConstraintViolation violation = result.violations().get(i);
            out.println("  " + (i + 1) + ". [" + violation.category() + "] "
                    + (violation.hard() ? "hard" : "soft")
                    + ", penalty=" + violation.penalty()
                    + " - " + violation.message());
        }
    }
}
