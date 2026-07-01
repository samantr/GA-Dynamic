package com.gadynamicapp.itc.validation;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.Distribution;
import com.gadynamicapp.itc.model.DistributionClassRef;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.model.ItcConfig;
import com.gadynamicapp.itc.model.ItcRoom;
import com.gadynamicapp.itc.model.RoomUnavailable;
import com.gadynamicapp.itc.parser.ParseResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class ItcProblemValidator {
    private static final Pattern BITSTRING = Pattern.compile("[01]+");

    public ValidationResult validate(ParseResult result) {
        List<ValidationIssue> issues = new ArrayList<>();
        ItcConfig config = result.config();

        validateMetadata(config, issues);
        validateRooms(result.rooms(), config, issues);
        validateClasses(result.classes(), result.rooms(), config, issues);
        validateDistributions(result.distributions(), result.classes(), issues);

        return new ValidationResult(issues);
    }

    private void validateMetadata(ItcConfig config, List<ValidationIssue> issues) {
        if (config.nrDays() <= 0) {
            addIssue(issues, "problem.nrDays", "nrDays must be greater than 0.");
        }
        if (config.nrWeeks() <= 0) {
            addIssue(issues, "problem.nrWeeks", "nrWeeks must be greater than 0.");
        }
        if (config.slotsPerDay() <= 0) {
            addIssue(issues, "problem.slotsPerDay", "slotsPerDay must be greater than 0.");
        }
    }

    private void validateRooms(List<ItcRoom> rooms, ItcConfig config, List<ValidationIssue> issues) {
        for (ItcRoom room : rooms) {
            for (int i = 0; i < room.unavailableTimes().size(); i++) {
                RoomUnavailable unavailable = room.unavailableTimes().get(i);
                String location = "room[" + displayId(room.id()) + "].unavailable[" + i + "]";
                validateBitstring(unavailable.days(), config.nrDays(), location + ".days", issues);
                validateBitstring(unavailable.weeks(), config.nrWeeks(), location + ".weeks", issues);
            }
        }
    }

    private void validateClasses(
            List<ItcClass> classes,
            List<ItcRoom> rooms,
            ItcConfig config,
            List<ValidationIssue> issues
    ) {
        Set<String> roomIds = new HashSet<>();
        for (ItcRoom room : rooms) {
            roomIds.add(room.id());
        }

        for (ItcClass itcClass : classes) {
            String classLocation = "class[" + displayId(itcClass.id()) + "]";
            if (itcClass.timeOptions().isEmpty()) {
                addIssue(issues, classLocation, "Class must have at least one allowed time.");
            }

            for (ClassRoomOption roomOption : itcClass.roomOptions()) {
                if (!roomIds.contains(roomOption.roomId())) {
                    addIssue(
                            issues,
                            classLocation + ".room[" + displayId(roomOption.roomId()) + "]",
                            "Class room option references unknown room id '" + displayId(roomOption.roomId()) + "'."
                    );
                }
            }

            for (int i = 0; i < itcClass.timeOptions().size(); i++) {
                ClassTimeOption timeOption = itcClass.timeOptions().get(i);
                String timeLocation = classLocation + ".time[" + i + "]";
                validateBitstring(timeOption.days(), config.nrDays(), timeLocation + ".days", issues);
                validateBitstring(timeOption.weeks(), config.nrWeeks(), timeLocation + ".weeks", issues);
            }
        }
    }

    private void validateDistributions(
            List<Distribution> distributions,
            List<ItcClass> classes,
            List<ValidationIssue> issues
    ) {
        Set<String> classIds = new HashSet<>();
        for (ItcClass itcClass : classes) {
            classIds.add(itcClass.id());
        }

        for (int i = 0; i < distributions.size(); i++) {
            Distribution distribution = distributions.get(i);
            for (DistributionClassRef classRef : distribution.classRefs()) {
                if (!classIds.contains(classRef.classId())) {
                    addIssue(
                            issues,
                            "distribution[" + i + "].class[" + displayId(classRef.classId()) + "]",
                            "Distribution references unknown class id '" + displayId(classRef.classId()) + "'."
                    );
                }
            }
        }
    }

    private void validateBitstring(
            String value,
            int expectedLength,
            String location,
            List<ValidationIssue> issues
    ) {
        if (value == null || value.isBlank()) {
            addIssue(issues, location, "Bitstring must not be blank.");
            return;
        }

        if (!BITSTRING.matcher(value).matches()) {
            addIssue(issues, location, "Bitstring must contain only 0 and 1.");
        }

        if (expectedLength > 0 && value.length() != expectedLength) {
            addIssue(
                    issues,
                    location,
                    "Bitstring length must be " + expectedLength + ", got " + value.length() + "."
            );
        }
    }

    private void addIssue(List<ValidationIssue> issues, String location, String message) {
        issues.add(new ValidationIssue(location, message));
    }

    private String displayId(String value) {
        if (value == null || value.isBlank()) {
            return "<missing>";
        }
        return value;
    }
}
