package com.gadynamicapp.itc.validation;

import java.util.List;
import java.util.Objects;

public record ValidationResult(List<ValidationIssue> issues) {
    public ValidationResult {
        Objects.requireNonNull(issues, "issues");
        issues = List.copyOf(issues);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }
}
