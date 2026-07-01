package com.gadynamicapp.itc.summary;

import java.util.Locale;
import java.util.Set;

final class KnownDistributionTypes {
    private static final Set<String> KNOWN_BASE_TYPES = Set.of(
            "DifferentDays",
            "DifferentRoom",
            "DifferentTime",
            "DifferentWeeks",
            "MaxBlock",
            "MaxBreaks",
            "MaxDayLoad",
            "MaxDays",
            "MinGap",
            "NotOverlap",
            "Overlap",
            "Precedence",
            "SameAttendees",
            "SameDays",
            "SameRoom",
            "SameStart",
            "SameTime",
            "SameWeeks",
            "WorkDay"
    );

    private KnownDistributionTypes() {
    }

    static boolean isKnown(String type) {
        String baseType = baseType(type);
        return KNOWN_BASE_TYPES.contains(baseType);
    }

    private static String baseType(String type) {
        String normalized = type == null ? "" : type.trim();
        int parameterStart = normalized.indexOf('(');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart);
        }
        return normalized.substring(0, Math.min(normalized.length(), 1)).toUpperCase(Locale.ROOT)
                + normalized.substring(Math.min(normalized.length(), 1));
    }
}
