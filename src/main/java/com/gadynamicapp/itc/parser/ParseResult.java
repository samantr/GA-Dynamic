package com.gadynamicapp.itc.parser;

import com.gadynamicapp.itc.model.Distribution;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.model.ItcConfig;
import com.gadynamicapp.itc.model.ItcRoom;

import java.util.List;
import java.util.Objects;

public record ParseResult(
        ItcConfig config,
        List<ItcRoom> rooms,
        List<ItcClass> classes,
        List<Distribution> distributions
) {
    public ParseResult {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(rooms, "rooms");
        Objects.requireNonNull(classes, "classes");
        Objects.requireNonNull(distributions, "distributions");
        rooms = List.copyOf(rooms);
        classes = List.copyOf(classes);
        distributions = List.copyOf(distributions);
    }
}
