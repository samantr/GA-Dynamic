package com.gadynamicapp.itc.summary;

import java.util.List;
import java.util.Objects;

public record ItcProblemSummary(
        String problemName,
        int nrDays,
        int nrWeeks,
        int slotsPerDay,
        int roomCount,
        int courseCount,
        int configCount,
        int subpartCount,
        int classCount,
        int distributionCount,
        int studentCount,
        int roomUnavailableCount,
        int classRoomOptionCount,
        int classTimeOptionCount,
        RoomCapacitySummary roomCapacity,
        int classesWithNoRoomOptions,
        int classesWithOneRoomOption,
        int classesWithMultipleRoomOptions,
        int classesWithNoTimeOptions,
        int classesWithOneTimeOption,
        int classesWithMultipleTimeOptions,
        List<DistributionTypeSummary> distributionTypes,
        List<String> unsupportedOrUnknownDistributionTypes
) {
    public ItcProblemSummary {
        Objects.requireNonNull(problemName, "problemName");
        Objects.requireNonNull(roomCapacity, "roomCapacity");
        Objects.requireNonNull(distributionTypes, "distributionTypes");
        Objects.requireNonNull(unsupportedOrUnknownDistributionTypes, "unsupportedOrUnknownDistributionTypes");
        distributionTypes = List.copyOf(distributionTypes);
        unsupportedOrUnknownDistributionTypes = List.copyOf(unsupportedOrUnknownDistributionTypes);
    }
}
