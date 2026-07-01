package com.gadynamicapp.itc.solution;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;

import java.io.PrintStream;

public final class SolutionSummaryService {
    private static final int DEFAULT_ASSIGNMENT_LIMIT = 20;

    public void print(PrintStream out, TimetableSolution solution) {
        print(out, solution, DEFAULT_ASSIGNMENT_LIMIT);
    }

    public void print(PrintStream out, TimetableSolution solution, int assignmentLimit) {
        out.println("Random Timetable Solution");
        out.println("Seed: " + solution.seed());
        out.println("Assigned classes: " + solution.assignedClassCount());
        out.println("Roomless classes: " + solution.roomlessClassCount());
        out.println();

        int visibleCount = Math.min(assignmentLimit, solution.assignments().size());
        out.println("First " + visibleCount + " assignments:");
        for (int i = 0; i < visibleCount; i++) {
            printAssignment(out, i + 1, solution.assignments().get(i));
        }
    }

    private void printAssignment(PrintStream out, int index, ClassAssignment assignment) {
        ClassTimeOption time = assignment.selectedTimeOption();
        out.println("  " + index + ". classId=" + assignment.classId());
        out.println("     time: days=" + time.days()
                + ", weeks=" + time.weeks()
                + ", start=" + time.start()
                + ", length=" + time.length()
                + ", penalty=" + time.penalty());

        if (assignment.selectedRoomOption().isPresent()) {
            ClassRoomOption room = assignment.selectedRoomOption().get();
            out.println("     room: id=" + room.roomId() + ", penalty=" + room.penalty());
        } else {
            out.println("     room: <none>");
        }
    }
}
