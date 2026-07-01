package com.gadynamicapp.itc.solution;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.parser.ParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class RandomSolutionGenerator {
    public TimetableSolution generate(ParseResult problem, long seed) {
        Random random = new Random(seed);
        List<ClassAssignment> assignments = new ArrayList<>();

        for (ItcClass itcClass : problem.classes()) {
            if (itcClass.timeOptions().isEmpty()) {
                throw new IllegalArgumentException(
                        "Class '" + itcClass.id() + "' has no allowed time options."
                );
            }

            ClassTimeOption selectedTime = selectRandom(itcClass.timeOptions(), random);
            Optional<ClassRoomOption> selectedRoom = itcClass.roomOptions().isEmpty()
                    ? Optional.empty()
                    : Optional.of(selectRandom(itcClass.roomOptions(), random));

            assignments.add(new ClassAssignment(itcClass.id(), selectedTime, selectedRoom));
        }

        return new TimetableSolution(seed, assignments);
    }

    private <T> T selectRandom(List<T> options, Random random) {
        return options.get(random.nextInt(options.size()));
    }
}
