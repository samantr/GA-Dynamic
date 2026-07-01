package com.gadynamicapp.itc.solution;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;

import java.util.Objects;
import java.util.Optional;

public record ClassAssignment(
        String classId,
        ClassTimeOption selectedTimeOption,
        Optional<ClassRoomOption> selectedRoomOption
) {
    public ClassAssignment {
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(selectedTimeOption, "selectedTimeOption");
        Objects.requireNonNull(selectedRoomOption, "selectedRoomOption");
    }

    public boolean hasRoomAssignment() {
        return selectedRoomOption.isPresent();
    }
}
