package com.gadynamicapp.itc.model;

import java.util.Objects;

public record DistributionClassRef(String classId) {
    public DistributionClassRef {
        Objects.requireNonNull(classId, "classId");
    }
}
