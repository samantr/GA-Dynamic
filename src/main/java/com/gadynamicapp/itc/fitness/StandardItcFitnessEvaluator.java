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
        DistributionEvaluation distributionEvaluation = evaluateDistributions(problem.distributions(), solution.assignments(), violations);

        FitnessBreakdown breakdown = new FitnessBreakdown(
                timeChoicePenalty,
                roomChoicePenalty,
                roomConflictViolationCount,
                roomUnavailableViolationCount,
                distributionEvaluation.requiredNotOverlapViolationCount(),
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
            List<ConstraintViolation> violations
    ) {
        Map<String, ClassAssignment> assignmentsByClassId = new HashMap<>();
        for (ClassAssignment assignment : assignments) {
            assignmentsByClassId.put(assignment.classId(), assignment);
        }

        int requiredNotOverlapViolationCount = 0;
        int unsupportedRequiredDistributionCount = 0;
        int unsupportedNonRequiredDistributionCount = 0;

        for (int distributionIndex = 0; distributionIndex < distributions.size(); distributionIndex++) {
            Distribution distribution = distributions.get(distributionIndex);
            if (distribution.required() && "NotOverlap".equals(distribution.type())) {
                requiredNotOverlapViolationCount += evaluateRequiredNotOverlap(
                        distributionIndex,
                        distribution,
                        assignmentsByClassId,
                        violations
                );
            } else if (distribution.required()) {
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
                requiredNotOverlapViolationCount,
                unsupportedRequiredDistributionCount,
                unsupportedNonRequiredDistributionCount
        );
    }

    private int evaluateRequiredNotOverlap(
            int distributionIndex,
            Distribution distribution,
            Map<String, ClassAssignment> assignmentsByClassId,
            List<ConstraintViolation> violations
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

                if (overlaps(left.selectedTimeOption(), right.selectedTimeOption())) {
                    count++;
                    addViolation(violations, hardViolation(
                            "REQUIRED_NOT_OVERLAP",
                            "Required NotOverlap distribution " + distributionIndex
                                    + " is violated by classes " + left.classId()
                                    + " and " + right.classId() + "."
                    ));
                }
            }
        }
        return count;
    }

    private void addViolation(List<ConstraintViolation> violations, ConstraintViolation violation) {
        if (collectViolations) {
            violations.add(violation);
        }
    }

    private ConstraintViolation hardViolation(String category, String message) {
        return new ConstraintViolation(category, true, HARD_VIOLATION_PENALTY, message);
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
            int requiredNotOverlapViolationCount,
            int unsupportedRequiredDistributionCount,
            int unsupportedNonRequiredDistributionCount
    ) {
    }
}