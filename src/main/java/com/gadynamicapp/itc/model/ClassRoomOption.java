package com.gadynamicapp.itc.model;

import java.util.Objects;

public record ClassRoomOption(String roomId, int penalty) {
    public ClassRoomOption {
        Objects.requireNonNull(roomId, "roomId");
    }
}
