package com.gadynamicapp.itc.fitness;

public record FitnessBreakdown(
        long timeChoicePenalty,
        long roomChoicePenalty,
        int roomConflictViolationCount,
        int roomUnavailableViolationCount,
        int requiredDistributionViolationCount,
        int unsupportedRequiredDistributionCount,
        int unsupportedNonRequiredDistributionCount
) {
    public long softPenaltyTotal() {
        return timeChoicePenalty + roomChoicePenalty;
    }

    public int hardViolationCount() {
        return roomConflictViolationCount
                + roomUnavailableViolationCount
                + requiredDistributionViolationCount
                + unsupportedRequiredDistributionCount;
    }
}
