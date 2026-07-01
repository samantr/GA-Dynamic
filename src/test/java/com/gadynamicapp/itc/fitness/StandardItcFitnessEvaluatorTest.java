package com.gadynamicapp.itc.fitness;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.Distribution;
import com.gadynamicapp.itc.model.DistributionClassRef;
import com.gadynamicapp.itc.model.ItcConfig;
import com.gadynamicapp.itc.model.OptimizationWeights;
import com.gadynamicapp.itc.parser.ParseResult;
import com.gadynamicapp.itc.solution.ClassAssignment;
import com.gadynamicapp.itc.solution.TimetableSolution;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardItcFitnessEvaluatorTest {
    @Test
    void requiredSameTimeRequiresMatchingStartAndLengthOnly() {
        ClassAssignment first = assignment("a", time("1000000", 10, 2, "1"), "r1");
        ClassAssignment sameTime = assignment("b", time("0100000", 10, 2, "1"), "r2");
        ClassAssignment differentStart = assignment("b", time("0100000", 11, 2, "1"), "r2");
        ClassAssignment differentLength = assignment("b", time("0100000", 10, 3, "1"), "r2");

        assertRequiredDistributionViolations(0, evaluate(requiredDistribution("SameTime", "a", "b"), first, sameTime));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("SameTime", "a", "b"), first, differentStart));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("SameTime", "a", "b"), first, differentLength));
    }

    @Test
    void requiredSameDaysAndDifferentDaysUseDayBitstrings() {
        ClassAssignment monday = assignment("a", time("1000000", 10, 2, "1"), "r1");
        ClassAssignment sameMonday = assignment("b", time("1000000", 20, 2, "1"), "r2");
        ClassAssignment tuesday = assignment("b", time("0100000", 20, 2, "1"), "r2");
        ClassAssignment mondayTuesday = assignment("b", time("1100000", 20, 2, "1"), "r2");

        assertRequiredDistributionViolations(0, evaluate(requiredDistribution("SameDays", "a", "b"), monday, sameMonday));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("SameDays", "a", "b"), monday, tuesday));
        assertRequiredDistributionViolations(0, evaluate(requiredDistribution("DifferentDays", "a", "b"), monday, tuesday));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("DifferentDays", "a", "b"), monday, mondayTuesday));
    }

    @Test
    void requiredSameRoomRequiresMatchingAssignedRoomIds() {
        ClassAssignment first = assignment("a", time("1000000", 10, 2, "1"), "r1");
        ClassAssignment sameRoom = assignment("b", time("1000000", 20, 2, "1"), "r1");
        ClassAssignment differentRoom = assignment("b", time("1000000", 20, 2, "1"), "r2");
        ClassAssignment missingRoom = assignment("b", time("1000000", 20, 2, "1"), null);

        assertRequiredDistributionViolations(0, evaluate(requiredDistribution("SameRoom", "a", "b"), first, sameRoom));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("SameRoom", "a", "b"), first, differentRoom));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("SameRoom", "a", "b"), first, missingRoom));
    }

    @Test
    void requiredPrecedenceOrdersMeetingsByWeekDayAndSlot() {
        ClassAssignment first = assignment("a", time("1000000", 10, 2, "1"), "r1");
        ClassAssignment laterSameDay = assignment("b", time("1000000", 12, 2, "1"), "r2");
        ClassAssignment overlappingSameDay = assignment("b", time("1000000", 11, 2, "1"), "r2");
        ClassAssignment earlierDay = assignment("b", time("0100000", 10, 2, "1"), "r2");
        ClassAssignment laterDay = assignment("a", time("0100000", 10, 2, "1"), "r1");

        assertRequiredDistributionViolations(0, evaluate(requiredDistribution("Precedence", "a", "b"), first, laterSameDay));
        assertRequiredDistributionViolations(0, evaluate(requiredDistribution("Precedence", "a", "b"), first, earlierDay));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("Precedence", "a", "b"), first, overlappingSameDay));
        assertRequiredDistributionViolations(1, evaluate(requiredDistribution("Precedence", "a", "b"), laterDay, first));
    }

    @Test
    void requiredSameAttendeesUsesPairwiseTimeOverlap() {
        ClassAssignment first = assignment("a", time("1000000", 10, 4, "1"), "r1");
        ClassAssignment overlapsFirst = assignment("b", time("1000000", 12, 1, "1"), "r2");
        ClassAssignment touchesSecond = assignment("c", time("1000000", 13, 1, "1"), "r3");
        ClassAssignment differentWeek = assignment("b", time("1000000", 12, 1, "01"), "r2");

        assertRequiredDistributionViolations(
                2,
                evaluate(requiredDistribution("SameAttendees", "a", "b", "c"), first, overlapsFirst, touchesSecond)
        );
        assertRequiredDistributionViolations(
                0,
                evaluate(requiredDistribution("SameAttendees", "a", "b"), first, differentWeek)
        );
    }

    private static void assertRequiredDistributionViolations(int expected, FitnessResult result) {
        assertEquals(expected, result.breakdown().requiredDistributionViolationCount());
        assertEquals(0, result.breakdown().unsupportedRequiredDistributionCount());
        assertEquals(expected, result.hardViolationCount());
    }

    private static FitnessResult evaluate(Distribution distribution, ClassAssignment... assignments) {
        ParseResult problem = new ParseResult(
                new ItcConfig("test", 7, 2, 288, new OptimizationWeights(0, 0, 0, 0), 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(distribution)
        );
        return new StandardItcFitnessEvaluator().evaluate(problem, new TimetableSolution(1L, List.of(assignments)));
    }

    private static Distribution requiredDistribution(String type, String... classIds) {
        List<DistributionClassRef> classRefs = new ArrayList<>();
        for (String classId : classIds) {
            classRefs.add(new DistributionClassRef(classId));
        }
        return new Distribution(type, true, null, classRefs);
    }

    private static ClassAssignment assignment(String classId, ClassTimeOption time, String roomId) {
        Optional<ClassRoomOption> room = roomId == null
                ? Optional.empty()
                : Optional.of(new ClassRoomOption(roomId, 0));
        return new ClassAssignment(classId, time, room);
    }

    private static ClassTimeOption time(String days, int start, int length, String weeks) {
        return new ClassTimeOption(days, start, length, weeks, 0);
    }
}