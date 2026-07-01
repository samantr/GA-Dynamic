package com.gadynamicapp.itc.validation;

import java.util.Objects;

public record ValidationIssue(
        String location,
        String message
) {
    public ValidationIssue {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(message, "message");
    }
}
