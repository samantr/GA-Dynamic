package com.gadynamicapp.itc.ga;

public record Gene(int timeOptionIndex, int roomOptionIndex) {
    public static final int NO_ROOM = -1;

    public Gene {
        if (timeOptionIndex < 0) {
            throw new IllegalArgumentException("timeOptionIndex must be non-negative.");
        }
        if (roomOptionIndex < NO_ROOM) {
            throw new IllegalArgumentException("roomOptionIndex must be -1 or non-negative.");
        }
    }
}
