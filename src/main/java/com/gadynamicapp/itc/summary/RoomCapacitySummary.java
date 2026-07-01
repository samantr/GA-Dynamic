package com.gadynamicapp.itc.summary;

public record RoomCapacitySummary(
        int minimum,
        int maximum,
        double average
) {
    public static RoomCapacitySummary empty() {
        return new RoomCapacitySummary(0, 0, 0.0);
    }
}
