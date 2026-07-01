package com.gadynamicapp.itc.model;

import java.util.Objects;

public record ClassTimeOption(String days, int start, int length, String weeks, int penalty) {
    public ClassTimeOption {
        Objects.requireNonNull(days, "days");
        Objects.requireNonNull(weeks, "weeks");
    }
}
