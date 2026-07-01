package com.gadynamicapp.itc.fitness;

import java.util.Objects;

public record ConstraintViolation(
        String category,
        boolean hard,
        long penalty,
        String message
) {
    public ConstraintViolation {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(message, "message");
    }
}
