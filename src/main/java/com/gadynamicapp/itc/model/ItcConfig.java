package com.gadynamicapp.itc.model;

import java.util.Objects;

public record ItcConfig(
        String name,
        int nrDays,
        int nrWeeks,
        int slotsPerDay,
        OptimizationWeights optimizationWeights,
        int courseCount,
        int configCount,
        int subpartCount,
        int studentCount
) {
    public ItcConfig {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(optimizationWeights, "optimizationWeights");
    }
}
