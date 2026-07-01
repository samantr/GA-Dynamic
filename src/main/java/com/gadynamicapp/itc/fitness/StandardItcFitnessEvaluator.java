package com.gadynamicapp.itc.fitness;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.Distribution;
import com.gadynamicapp.itc.model.DistributionClassRef;
import com.gadynamicapp.itc.model.ItcRoom;
import com.gadynamicapp.itc.model.OptimizationWeights;
import com.gadynamicapp.itc.model.RoomUnavailable;
import com.gadynamicapp.itc.parser.ParseResult;
import com.gadynamicapp.itc.solution.ClassAssignment;
import com.gadynamicapp.itc.solution.TimetableSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiPredicate;

public final class StandardItcFitnessEvaluator implements FitnessEvaluator {
    public static final long HARD_VIOLATION_PENALTY = 1_000_000L;

    private final boolean collectViolations;

    public StandardItcFitnessEvaluator() {
        this(true);
    }

    public StandardItcFitnessEvaluator(boolean collectViolations) {
        this.collectViolations = collectViolations;
    }

    @Override
    public FitnessResult evaluate(ParseResult problem, TimetableSolution solution) {
        List<ConstraintViolation> violations = new ArrayList<>();
        OptimizationWeights weights = problem.config().optimizationWeights();

        long timeChoicePenalty = 0L;
        long roomChoicePenalty = 0L;
        for (ClassAssignment assignment : solution.assignments()) {
            timeChoicePenalty += (long) assignment.selectedTimeOption().penalty() * weights.time();
            roomChoicePenalty += assignment.selectedRoomOption()
                    .map(room -> (long) room.penalty() * weights.room())
                    .orElse(0L);
        }

        int roomConflictViolationCount = evaluateRoomConflicts(solution.assignments(), violations);
        int roomUnavailableViolationCount = evaluateRoomUnavailable(problem.rooms(), solution.assignments(), violations);
        DistributionEvaluation distributionEvaluation = evaluateDistributions(
                problem.distributions(),
                solution.assignments(),
                problem.config().nrDays(),
                problem.config().slotsPerDay(),
                violations
        );

        FitnessBreakdown breakdown = new FitnessBreakdown(
                timeChoicePenalty,
                roomChoicePenalty,
                roomConflictViolationCount,
                roomUnavailableViolationCount,
                distributionEvaluation.requiredDistributionViolationCount(),
                distributionEvaluation.unsupportedRequiredDistributionCount(),
                distributionEvaluation.unsupportedNonRequiredDistributionCount()
        );

        int hardViolationCount = breakdown.hardViolationCount();
        long softPenaltyTotal = breakdown.softPenaltyTotal();
        long totalScore = softPenaltyTotal + (hardViolationCount * HARD_VIOLATION_PENALTY);

        return new FitnessResult(totalScore, hardViolationCount, softPenaltyTotal, breakdown, violations);
    }

    private int evaluateRoomConflicts(List<ClassAssignment> assignments, List<ConstraintViolation> violations) {
        Map<String, List<ClassAssignment>> assignmentsByRoom = new HashMap<>();
        for (ClassAssignment assignment : assignments) {
            assignment.selectedRoomOption()
                    .map(ClassRoomOption::roomId)
                    .ifPresent(roomId -> assignmentsByRoom
                            .computeIfAbsent(roomId, ignored -> new ArrayList<>())
                            .add(assignment));
        }

        int count = 0;
        for (Map.Entry<String, List<ClassAssignment>> entry : assignmentsByRoom.entrySet()) {
            String roomId = entry.getKey();
            List<ClassAssignment> roomAssignments = entry.getValue();
            for (int i = 0; i < roomAssignments.size(); i++) {
                ClassAssignment left = roomAssignments.get(i);
                for (int j = i + 1; j < roomAssignments.size(); j++) {
                    ClassAssignment right = roomAssignments.get(j);
                    if (!overlaps(left.selectedTimeOption(), right.selectedTimeOption())) {
                        continue;
                    }

                    count++;
                    addViolation(violations, hardViolation(
                            "ROOM_CONFLICT",
                            "Classes " + left.classId() + " and " + right.classId()
                                    + " overlap in room " + roomId + "."
                    ));
                }
            }
        }
        return count;
    }

    private int evaluateRoomUnavailable(
            List<ItcRoom> rooms,
            List<ClassAssignment> assignments,
            List<ConstraintViolation> violations
    ) {
        Map<String, ItcRoom> roomsById = new HashMap<>();
        for (ItcRoom room : rooms) {
            roomsById.put(room.id(), room);
        }

        int count = 0;
        for (ClassAssignment assignment : assignments) {
            if (assignment.selectedRoomOption().isEmpty()) {
                continue;
            }

            String roomId = assignment.selectedRoomOption().get().roomId();
            ItcRoom room = roomsById.get(roomId);
            if (room == null) {
                continue;
            }

            for (RoomUnavailable unavailable : room.unavailableTimes()) {
                if (overlaps(assignment.selectedTimeOption(), unavailable)) {
                    count++;
                    addViolation(violations, hardViolation(
                            "ROOM_UNAVAILABLE",
                            "Class " + assignment.classId() + " uses room " + roomId
                                    + " during an unavailable period."
                    ));
                }
            }
        }
        return count;
    }

    private DistributionEvaluation evaluateDistributions(
            List<Distribution> distributions,
            List<ClassAssignment> assignments,
            int nrDays,
            int slotsPerDay,
            List<ConstraintViolation> violations
    ) {
        Map<String, ClassAssignment> assignmentsByClassId = new HashMap<>();
        for (ClassAssignment assignment : assignments) {
            assignmentsByClassId.put(assignment.classId(), assignment);
        }

        int requiredDistributionViolationCount = 0;
        int unsupportedRequiredDistributionCount = 0;
        int unsupportedNonRequiredDistributionCount = 0;

        for (int distributionIndex = 0; distributionIndex < distributions.size(); distributionIndex++) {
            Distribution distribution = distributions.get(distributionIndex);
            String baseType = baseDistributionType(distribution.type());
            if (distribution.required()) {
                BiPredicate<ClassAssignment, ClassAssignment> violationPredicate =
                        requiredDistributionViolationPredicate(baseType, nrDays, slotsPerDay);
                if (violationPredicate != null) {
                    requiredDistributionViolationCount += evaluateRequiredDistributionPairs(
                            distributionIndex,
                            baseType,
                            distribution,
                            assignmentsByClassId,
                            violations,
                            violationPredicate
                    );
                    continue;
                }

                unsupportedRequiredDistributionCount++;
                addViolation(violations, hardViolation(
                        "UNSUPPORTED_REQUIRED_DISTRIBUTION",
                        "Required distribution " + distributionIndex + " has unsupported type '"
                                + distribution.type() + "'."
                ));
            } else {
                unsupportedNonRequiredDistributionCount++;
                addViolation(violations, new ConstraintViolation(
                        "UNSUPPORTED_NON_REQUIRED_DISTRIBUTION",
                        false,
                        0L,
                        "Non-required distribution " + distributionIndex + " has unsupported type '"
                                + distribution.type() + "'."
                ));
            }
        }

        return new DistributionEvaluation(
                requiredDistributionViolationCount,
                unsupportedRequiredDistributionCount,
                unsupportedNonRequiredDistributionCount
        );
    }

    private int evaluateRequiredDistributionPairs(
            int distributionIndex,
            String baseType,
            Distribution distribution,
            Map<String, ClassAssignment> assignmentsByClassId,
            List<ConstraintViolation> violations,
            BiPredicate<ClassAssignment, ClassAssignment> violationPredicate
    ) {
        int count = 0;
        List<DistributionClassRef> classRefs = distribution.classRefs();
        for (int i = 0; i < classRefs.size(); i++) {
            ClassAssignment left = assignmentsByClassId.get(classRefs.get(i).classId());
            if (left == null) {
                continue;
            }

            for (int j = i + 1; j < classRefs.size(); j++) {
                ClassAssignment right = assignmentsByClassId.get(classRefs.get(j).classId());
                if (right == null) {
                    continue;
                }

                if (violationPredicate.test(left, right)) {
                    count++;
                    addViolation(violations, hardViolation(
                            requiredDistributionCategory(baseType),
                            "Required " + baseType + " distribution " + distributionIndex
                                    + " is violated by classes " + left.classId()
                                    + " and " + right.classId() + "."
                    ));
                }
            }
        }
        return count;
    }

    private BiPredicate<ClassAssignment, ClassAssignment> requiredDistributionViolationPredicate(
            String baseType,
            int nrDays,
            int slotsPerDay
    ) {
        return switch (baseType) {
            case "NotOverlap" -> (left, right) -> overlaps(left.selectedTimeOption(), right.selectedTimeOption());
            case "SameTime" -> (left, right) -> !sameTime(left.selectedTimeOption(), right.selectedTimeOption());
            case "SameDays" -> (left, right) -> !sameDays(left.selectedTimeOption(), right.selectedTimeOption());
            case "DifferentDays" -> (left, right) -> !differentDays(left.selectedTimeOption(), right.selectedTimeOption());
            case "SameRoom" -> (left, right) -> !sameRoom(left, right);
            case "Precedence" -> (left, right) -> !precedes(
                    left.selectedTimeOption(),
                    right.selectedTimeOption(),
                    nrDays,
                    slotsPerDay
            );
            case "SameAttendees" -> (left, right) -> overlaps(left.selectedTimeOption(), right.selectedTimeOption());
            default -> null;
        };
    }

    private void addViolation(List<ConstraintViolation> violations, ConstraintViolation violation) {
        if (collectViolations) {
            violations.add(violation);
        }
    }

    private ConstraintViolation hardViolation(String category, String message) {
        return new ConstraintViolation(category, true, HARD_VIOLATION_PENALTY, message);
    }

    private String baseDistributionType(String type) {
        String normalized = type == null ? "" : type.trim();
        int parameterStart = normalized.indexOf('(');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart);
        }
        return normalized;
    }

    private String requiredDistributionCategory(String baseType) {
        return switch (baseType) {
            case "NotOverlap" -> "REQUIRED_NOT_OVERLAP";
            case "SameTime" -> "REQUIRED_SAME_TIME";
            case "SameDays" -> "REQUIRED_SAME_DAYS";
            case "DifferentDays" -> "REQUIRED_DIFFERENT_DAYS";
            case "SameRoom" -> "REQUIRED_SAME_ROOM";
            case "Precedence" -> "REQUIRED_PRECEDENCE";
            case "SameAttendees" -> "REQUIRED_SAME_ATTENDEES";
            default -> "REQUIRED_DISTRIBUTION";
        };
    }

    private boolean sameTime(ClassTimeOption left, ClassTimeOption right) {
        return left.start() == right.start()
                && left.length() == right.length();
    }

    private boolean sameDays(ClassTimeOption left, ClassTimeOption right) {
        return left.days().equals(right.days());
    }

    private boolean differentDays(ClassTimeOption left, ClassTimeOption right) {
        return !bitstringsIntersect(left.days(), right.days());
    }

    private boolean sameRoom(ClassAssignment left, ClassAssignment right) {
        Optional<String> leftRoomId = left.selectedRoomOption().map(ClassRoomOption::roomId);
        Optional<String> rightRoomId = right.selectedRoomOption().map(ClassRoomOption::roomId);
        return leftRoomId.isPresent()
                && rightRoomId.isPresent()
                && leftRoomId.get().equals(rightRoomId.get());
    }

    private boolean precedes(ClassTimeOption left, ClassTimeOption right, int nrDays, int slotsPerDay) {
        int dayCount = Math.max(nrDays, Math.max(left.days().length(), right.days().length()));
        int effectiveSlotsPerDay = slotsPerDay > 0
                ? slotsPerDay
                : Math.max(left.start() + left.length(), right.start() + right.length());
        OptionalLong leftLastEnd = lastMeetingEndSlot(left, dayCount, effectiveSlotsPerDay);
        OptionalLong rightFirstStart = firstMeetingStartSlot(right, dayCount, effectiveSlotsPerDay);
        return leftLastEnd.isPresent()
                && rightFirstStart.isPresent()
                && leftLastEnd.getAsLong() <= rightFirstStart.getAsLong();
    }

    private OptionalLong firstMeetingStartSlot(ClassTimeOption time, int dayCount, int slotsPerDay) {
        OptionalLong firstStart = OptionalLong.empty();
        for (int weekIndex = 0; weekIndex < time.weeks().length(); weekIndex++) {
            if (time.weeks().charAt(weekIndex) != '1') {
                continue;
            }

            for (int dayIndex = 0; dayIndex < time.days().length(); dayIndex++) {
                if (time.days().charAt(dayIndex) != '1') {
                    continue;
                }

                long startSlot = absoluteSlot(weekIndex, dayIndex, time.start(), dayCount, slotsPerDay);
                if (firstStart.isEmpty() || startSlot < firstStart.getAsLong()) {
                    firstStart = OptionalLong.of(startSlot);
                }
            }
        }
        return firstStart;
    }

    private OptionalLong lastMeetingEndSlot(ClassTimeOption time, int dayCount, int slotsPerDay) {
        OptionalLong lastEnd = OptionalLong.empty();
        for (int weekIndex = 0; weekIndex < time.weeks().length(); weekIndex++) {
            if (time.weeks().charAt(weekIndex) != '1') {
                continue;
            }

            for (int dayIndex = 0; dayIndex < time.days().length(); dayIndex++) {
                if (time.days().charAt(dayIndex) != '1') {
                    continue;
                }

                long endSlot = absoluteSlot(
                        weekIndex,
                        dayIndex,
                        time.start() + time.length(),
                        dayCount,
                        slotsPerDay
                );
                if (lastEnd.isEmpty() || endSlot > lastEnd.getAsLong()) {
                    lastEnd = OptionalLong.of(endSlot);
                }
            }
        }
        return lastEnd;
    }

    private long absoluteSlot(int weekIndex, int dayIndex, int slot, int dayCount, int slotsPerDay) {
        return ((long) weekIndex * dayCount + dayIndex) * slotsPerDay + slot;
    }

    private boolean overlaps(ClassTimeOption left, ClassTimeOption right) {
        return bitstringsIntersect(left.days(), right.days())
                && bitstringsIntersect(left.weeks(), right.weeks())
                && intervalsOverlap(left.start(), left.length(), right.start(), right.length());
    }

    private boolean overlaps(ClassTimeOption time, RoomUnavailable unavailable) {
        return bitstringsIntersect(time.days(), unavailable.days())
                && bitstringsIntersect(time.weeks(), unavailable.weeks())
                && intervalsOverlap(time.start(), time.length(), unavailable.start(), unavailable.length());
    }

    private boolean bitstringsIntersect(String left, String right) {
        int length = Math.min(left.length(), right.length());
        for (int i = 0; i < length; i++) {
            if (left.charAt(i) == '1' && right.charAt(i) == '1') {
                return true;
            }
        }
        return false;
    }

    private boolean intervalsOverlap(int leftStart, int leftLength, int rightStart, int rightLength) {
        int leftEnd = leftStart + leftLength;
        int rightEnd = rightStart + rightLength;
        return leftStart < rightEnd && rightStart < leftEnd;
    }

    private record DistributionEvaluation(
            int requiredDistributionViolationCount,
            int unsupportedRequiredDistributionCount,
            int unsupportedNonRequiredDistributionCount
    ) {
    }
}