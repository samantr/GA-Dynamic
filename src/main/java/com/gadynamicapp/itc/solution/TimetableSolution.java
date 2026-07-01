package com.gadynamicapp.itc.solution;

import java.util.List;
import java.util.Objects;

public record TimetableSolution(
        long seed,
        List<ClassAssignment> assignments
) {
    public TimetableSolution {
        Objects.requireNonNull(assignments, "assignments");
        assignments = List.copyOf(assignments);
    }

    public int assignedClassCount() {
        return assignments.size();
    }

    public int roomlessClassCount() {
        return (int) assignments.stream()
                .filter(assignment -> assignment.selectedRoomOption().isEmpty())
                .count();
    }
}
