package com.gadynamicapp.itc.summary;

import com.gadynamicapp.itc.model.Distribution;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.model.ItcConfig;
import com.gadynamicapp.itc.model.ItcRoom;
import com.gadynamicapp.itc.parser.ParseResult;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ItcProblemSummaryService {
    public ItcProblemSummary summarize(ParseResult result) {
        ItcConfig config = result.config();
        List<ItcRoom> rooms = result.rooms();
        List<ItcClass> classes = result.classes();
        List<Distribution> distributions = result.distributions();

        int roomUnavailableCount = rooms.stream()
                .mapToInt(room -> room.unavailableTimes().size())
                .sum();
        int classRoomOptionCount = classes.stream()
                .mapToInt(itcClass -> itcClass.roomOptions().size())
                .sum();
        int classTimeOptionCount = classes.stream()
                .mapToInt(itcClass -> itcClass.timeOptions().size())
                .sum();

        return new ItcProblemSummary(
                config.name(),
                config.nrDays(),
                config.nrWeeks(),
                config.slotsPerDay(),
                rooms.size(),
                config.courseCount(),
                config.configCount(),
                config.subpartCount(),
                classes.size(),
                distributions.size(),
                config.studentCount(),
                roomUnavailableCount,
                classRoomOptionCount,
                classTimeOptionCount,
                summarizeRoomCapacity(rooms),
                countClassesByRoomOptions(classes, 0),
                countClassesByRoomOptions(classes, 1),
                countClassesByRoomOptionsGreaterThan(classes, 1),
                countClassesByTimeOptions(classes, 0),
                countClassesByTimeOptions(classes, 1),
                countClassesByTimeOptionsGreaterThan(classes, 1),
                summarizeDistributionTypes(distributions),
                unsupportedOrUnknownDistributionTypes(distributions)
        );
    }

    public void print(PrintStream out, Path input, ParseResult result) {
        ItcProblemSummary summary = summarize(result);

        out.println("ITC Problem Summary");
        out.println("Input: " + input);
        out.println("Problem: " + display(summary.problemName()));
        out.println("Metadata: nrDays=" + summary.nrDays()
                + ", nrWeeks=" + summary.nrWeeks()
                + ", slotsPerDay=" + summary.slotsPerDay());
        out.println();

        out.println("Basic counts:");
        out.println("  Rooms: " + summary.roomCount());
        out.println("  Courses: " + summary.courseCount());
        out.println("  Configs: " + summary.configCount());
        out.println("  Subparts: " + summary.subpartCount());
        out.println("  Total classes: " + summary.classCount());
        out.println("  Distributions: " + summary.distributionCount());
        out.println("  Students: " + summary.studentCount());
        out.println("  Room unavailable records: " + summary.roomUnavailableCount());
        out.println("  Class room options: " + summary.classRoomOptionCount());
        out.println("  Class time options: " + summary.classTimeOptionCount());
        out.println();

        RoomCapacitySummary capacity = summary.roomCapacity();
        out.println("Room capacity:");
        out.println("  Minimum: " + capacity.minimum());
        out.println("  Maximum: " + capacity.maximum());
        out.println("  Average: " + String.format(Locale.ROOT, "%.2f", capacity.average()));
        out.println();

        out.println("Class room options:");
        out.println("  Classes with no room options: " + summary.classesWithNoRoomOptions());
        out.println("  Classes with exactly one room option: " + summary.classesWithOneRoomOption());
        out.println("  Classes with multiple room options: " + summary.classesWithMultipleRoomOptions());
        out.println();

        out.println("Class time options:");
        out.println("  Classes with no time options: " + summary.classesWithNoTimeOptions());
        out.println("  Classes with exactly one time option: " + summary.classesWithOneTimeOption());
        out.println("  Classes with multiple time options: " + summary.classesWithMultipleTimeOptions());
        out.println();

        out.println("Distribution types:");
        if (summary.distributionTypes().isEmpty()) {
            out.println("  None");
        } else {
            for (DistributionTypeSummary distributionType : summary.distributionTypes()) {
                out.println("  " + display(distributionType.type())
                        + ": required=" + distributionType.requiredCount()
                        + ", non-required=" + distributionType.nonRequiredCount()
                        + ", total=" + distributionType.totalCount());
            }
        }
        out.println();

        out.println("Unsupported or unknown distribution types:");
        if (summary.unsupportedOrUnknownDistributionTypes().isEmpty()) {
            out.println("  None");
        } else {
            for (String type : summary.unsupportedOrUnknownDistributionTypes()) {
                out.println("  " + display(type));
            }
        }
    }

    private RoomCapacitySummary summarizeRoomCapacity(List<ItcRoom> rooms) {
        if (rooms.isEmpty()) {
            return RoomCapacitySummary.empty();
        }

        IntSummaryStatistics statistics = rooms.stream()
                .mapToInt(ItcRoom::capacity)
                .summaryStatistics();
        return new RoomCapacitySummary(statistics.getMin(), statistics.getMax(), statistics.getAverage());
    }

    private int countClassesByRoomOptions(List<ItcClass> classes, int optionCount) {
        return (int) classes.stream()
                .filter(itcClass -> itcClass.roomOptions().size() == optionCount)
                .count();
    }

    private int countClassesByRoomOptionsGreaterThan(List<ItcClass> classes, int optionCount) {
        return (int) classes.stream()
                .filter(itcClass -> itcClass.roomOptions().size() > optionCount)
                .count();
    }

    private int countClassesByTimeOptions(List<ItcClass> classes, int optionCount) {
        return (int) classes.stream()
                .filter(itcClass -> itcClass.timeOptions().size() == optionCount)
                .count();
    }

    private int countClassesByTimeOptionsGreaterThan(List<ItcClass> classes, int optionCount) {
        return (int) classes.stream()
                .filter(itcClass -> itcClass.timeOptions().size() > optionCount)
                .count();
    }

    private List<DistributionTypeSummary> summarizeDistributionTypes(List<Distribution> distributions) {
        Map<String, DistributionCounter> countersByType = new TreeMap<>();
        for (Distribution distribution : distributions) {
            DistributionCounter counter = countersByType.computeIfAbsent(distribution.type(), ignored -> new DistributionCounter());
            counter.add(distribution.required());
        }

        List<DistributionTypeSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, DistributionCounter> entry : countersByType.entrySet()) {
            summaries.add(new DistributionTypeSummary(
                    entry.getKey(),
                    entry.getValue().requiredCount,
                    entry.getValue().nonRequiredCount
            ));
        }
        summaries.sort(Comparator.comparing(DistributionTypeSummary::type));
        return summaries;
    }

    private List<String> unsupportedOrUnknownDistributionTypes(List<Distribution> distributions) {
        return distributions.stream()
                .map(Distribution::type)
                .distinct()
                .filter(type -> !KnownDistributionTypes.isKnown(type))
                .sorted()
                .toList();
    }

    private String display(String value) {
        if (value == null || value.isBlank()) {
            return "<missing>";
        }
        return value;
    }

    private static final class DistributionCounter {
        private int requiredCount;
        private int nonRequiredCount;

        private void add(boolean required) {
            if (required) {
                requiredCount++;
            } else {
                nonRequiredCount++;
            }
        }
    }
}
