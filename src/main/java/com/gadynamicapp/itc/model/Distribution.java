package com.gadynamicapp.itc.model;

import java.util.List;
import java.util.Objects;

public record Distribution(
        String type,
        boolean required,
        Integer penalty,
        List<DistributionClassRef> classRefs
) {
    public Distribution {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(classRefs, "classRefs");
        classRefs = List.copyOf(classRefs);
    }
}
