package com.gadynamicapp.itc.model;

import java.util.List;
import java.util.Objects;

public record ItcClass(
        String id,
        String courseId,
        String configId,
        String subpartId,
        int limit,
        String parentId,
        Boolean roomRequired,
        List<ClassRoomOption> roomOptions,
        List<ClassTimeOption> timeOptions
) {
    public ItcClass {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(configId, "configId");
        Objects.requireNonNull(subpartId, "subpartId");
        Objects.requireNonNull(roomOptions, "roomOptions");
        Objects.requireNonNull(timeOptions, "timeOptions");
        roomOptions = List.copyOf(roomOptions);
        timeOptions = List.copyOf(timeOptions);
    }
}