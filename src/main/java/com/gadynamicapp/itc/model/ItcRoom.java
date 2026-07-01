package com.gadynamicapp.itc.model;

import java.util.List;
import java.util.Objects;

public record ItcRoom(
        String id,
        int capacity,
        List<RoomUnavailable> unavailableTimes
) {
    public ItcRoom {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(unavailableTimes, "unavailableTimes");
        unavailableTimes = List.copyOf(unavailableTimes);
    }
}
