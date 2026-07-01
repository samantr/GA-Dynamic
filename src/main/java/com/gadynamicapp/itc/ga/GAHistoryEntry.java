package com.gadynamicapp.itc.ga;

public record GAHistoryEntry(
        int generation,
        long bestScore,
        double averageScore,
        int bestHardViolations,
        long bestSoftPenalty
) {
}
