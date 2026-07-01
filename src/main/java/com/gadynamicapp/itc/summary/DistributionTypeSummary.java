package com.gadynamicapp.itc.summary;

import java.util.Objects;

public record DistributionTypeSummary(
        String type,
        int requiredCount,
        int nonRequiredCount
) {
    public DistributionTypeSummary {
        Objects.requireNonNull(type, "type");
    }

    public int totalCount() {
        return requiredCount + nonRequiredCount;
    }
}
