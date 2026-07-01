package com.gadynamicapp.itc.model;

import java.util.Objects;

public record RoomUnavailable(
        String days,
        int start,
        int length,
        String weeks
) {
    public RoomUnavailable {
        Objects.requireNonNull(days, "days");
        Objects.requireNonNull(weeks, "weeks");
    }
}
