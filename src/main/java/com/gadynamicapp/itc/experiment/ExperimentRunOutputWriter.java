package com.gadynamicapp.itc.experiment;

import com.gadynamicapp.itc.fitness.FitnessBreakdown;
import com.gadynamicapp.itc.fitness.FitnessResult;
import com.gadynamicapp.itc.ga.GAHistoryEntry;
import com.gadynamicapp.itc.ga.GAResult;
import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.parser.ParseResult;
import com.gadynamicapp.itc.solution.ClassAssignment;
import com.gadynamicapp.itc.summary.DistributionTypeSummary;
import com.gadynamicapp.itc.summary.ItcProblemSummary;
import com.gadynamicapp.itc.summary.ItcProblemSummaryService;
import com.gadynamicapp.itc.summary.RoomCapacitySummary;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ExperimentRunOutputWriter {
    private static final DateTimeFormatter DIRECTORY_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter JSON_TIMESTAMP =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public ExperimentRunOutput write(Path inputFile, ParseResult problem, GAResult result) throws IOException {
        ZonedDateTime timestamp = ZonedDateTime.now();
        Path runDirectory = createRunDirectory(timestamp);

        Path runConfig = runDirectory.resolve("run_config.json");
        Path problemSummary = runDirectory.resolve("problem_summary.json");
        Path finalFitnessBreakdown = runDirectory.resolve("final_fitness_breakdown.json");
        Path convergence = runDirectory.resolve("convergence.csv");
        Path bestSolutionAssignments = runDirectory.resolve("best_solution_assignments.csv");

        writeRunConfig(runConfig, inputFile, problem, result, timestamp);
        writeProblemSummary(problemSummary, new ItcProblemSummaryService().summarize(problem));
        writeFinalFitnessBreakdown(finalFitnessBreakdown, result.bestFitness());
        writeConvergence(convergence, result);
        writeBestSolutionAssignments(bestSolutionAssignments, problem, result);

        return new ExperimentRunOutput(
                runDirectory,
                runConfig,
                problemSummary,
                finalFitnessBreakdown,
                convergence,
                bestSolutionAssignments
        );
    }

    private Path createRunDirectory(ZonedDateTime timestamp) throws IOException {
        Path runsDirectory = Path.of("output", "runs");
        Files.createDirectories(runsDirectory);

        String baseName = timestamp.format(DIRECTORY_TIMESTAMP);
        Path runDirectory = runsDirectory.resolve(baseName);
        int suffix = 1;
        while (Files.exists(runDirectory)) {
            runDirectory = runsDirectory.resolve(baseName + "_" + suffix);
            suffix++;
        }
        Files.createDirectories(runDirectory);
        return runDirectory;
    }

    private void writeRunConfig(
            Path output,
            Path inputFile,
            ParseResult problem,
            GAResult result,
            ZonedDateTime timestamp
    ) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendJsonField(json, "inputFile", inputFile.toString(), true);
        appendJsonField(json, "problemName", problem.config().name(), true);
        appendJsonField(json, "populationSize", result.config().populationSize(), true);
        appendJsonField(json, "generations", result.config().generations(), true);
        appendJsonField(json, "mutationRate", result.config().mutationRate(), true);
        appendJsonField(json, "tournamentSize", result.config().tournamentSize(), true);
        appendJsonField(json, "elitismCount", result.config().elitismCount(), true);
        appendJsonField(json, "seed", result.config().seed(), true);
        appendJsonField(json, "javaVersion", System.getProperty("java.version"), true);
        appendJsonField(json, "timestamp", timestamp.format(JSON_TIMESTAMP), false);
        json.append("}\n");
        Files.writeString(output, json.toString());
    }

    private void writeProblemSummary(Path output, ItcProblemSummary summary) throws IOException {
        StringBuilder json = new StringBuilder();
        RoomCapacitySummary capacity = summary.roomCapacity();

        json.append("{\n");
        appendJsonField(json, "problemName", summary.problemName(), true);
        appendJsonField(json, "nrDays", summary.nrDays(), true);
        appendJsonField(json, "nrWeeks", summary.nrWeeks(), true);
        appendJsonField(json, "slotsPerDay", summary.slotsPerDay(), true);
        appendJsonField(json, "roomCount", summary.roomCount(), true);
        appendJsonField(json, "courseCount", summary.courseCount(), true);
        appendJsonField(json, "configCount", summary.configCount(), true);
        appendJsonField(json, "subpartCount", summary.subpartCount(), true);
        appendJsonField(json, "classCount", summary.classCount(), true);
        appendJsonField(json, "distributionCount", summary.distributionCount(), true);
        appendJsonField(json, "studentCount", summary.studentCount(), true);
        appendJsonField(json, "roomUnavailableCount", summary.roomUnavailableCount(), true);
        appendJsonField(json, "classRoomOptionCount", summary.classRoomOptionCount(), true);
        appendJsonField(json, "classTimeOptionCount", summary.classTimeOptionCount(), true);

        json.append("  \"roomCapacity\": {\n");
        appendJsonField(json, "minimum", capacity.minimum(), true, 4);
        appendJsonField(json, "maximum", capacity.maximum(), true, 4);
        appendJsonField(json, "average", capacity.average(), false, 4);
        json.append("  },\n");

        appendJsonField(json, "classesWithNoRoomOptions", summary.classesWithNoRoomOptions(), true);
        appendJsonField(json, "classesWithOneRoomOption", summary.classesWithOneRoomOption(), true);
        appendJsonField(json, "classesWithMultipleRoomOptions", summary.classesWithMultipleRoomOptions(), true);
        appendJsonField(json, "classesWithNoTimeOptions", summary.classesWithNoTimeOptions(), true);
        appendJsonField(json, "classesWithOneTimeOption", summary.classesWithOneTimeOption(), true);
        appendJsonField(json, "classesWithMultipleTimeOptions", summary.classesWithMultipleTimeOptions(), true);

        json.append("  \"distributionTypes\": [\n");
        for (int i = 0; i < summary.distributionTypes().size(); i++) {
            DistributionTypeSummary distributionType = summary.distributionTypes().get(i);
            json.append("    {\n");
            appendJsonField(json, "type", distributionType.type(), true, 6);
            appendJsonField(json, "requiredCount", distributionType.requiredCount(), true, 6);
            appendJsonField(json, "nonRequiredCount", distributionType.nonRequiredCount(), true, 6);
            appendJsonField(json, "totalCount", distributionType.totalCount(), false, 6);
            json.append("    }");
            json.append(i + 1 < summary.distributionTypes().size() ? "," : "");
            json.append("\n");
        }
        json.append("  ],\n");

        json.append("  \"unsupportedOrUnknownDistributionTypes\": [");
        for (int i = 0; i < summary.unsupportedOrUnknownDistributionTypes().size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append(quote(summary.unsupportedOrUnknownDistributionTypes().get(i)));
        }
        json.append("]\n");
        json.append("}\n");

        Files.writeString(output, json.toString());
    }

    private void writeFinalFitnessBreakdown(Path output, FitnessResult fitness) throws IOException {
        FitnessBreakdown breakdown = fitness.breakdown();
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendJsonField(json, "totalScore", fitness.totalScore(), true);
        appendJsonField(json, "hardViolationCount", fitness.hardViolationCount(), true);
        appendJsonField(json, "softPenaltyTotal", fitness.softPenaltyTotal(), true);
        appendJsonField(json, "timeChoicePenalty", breakdown.timeChoicePenalty(), true);
        appendJsonField(json, "roomChoicePenalty", breakdown.roomChoicePenalty(), true);
        appendJsonField(json, "roomConflictViolationCount", breakdown.roomConflictViolationCount(), true);
        appendJsonField(json, "roomUnavailableViolationCount", breakdown.roomUnavailableViolationCount(), true);
        appendJsonField(json, "requiredDistributionViolationCount", breakdown.requiredDistributionViolationCount(), true);
        appendJsonField(json, "unsupportedRequiredDistributionCount", breakdown.unsupportedRequiredDistributionCount(), true);
        appendJsonField(json, "unsupportedNonRequiredDistributionCount", breakdown.unsupportedNonRequiredDistributionCount(), true);
        appendJsonField(json, "constraintReportCount", fitness.violations().size(), false);
        json.append("}\n");
        Files.writeString(output, json.toString());
    }

    private void writeConvergence(Path output, GAResult result) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("generation,bestScore,averageScore,bestHardViolations,bestSoftPenalty");
            writer.newLine();
            for (GAHistoryEntry entry : result.history()) {
                writer.write(entry.generation()
                        + "," + entry.bestScore()
                        + "," + String.format(Locale.ROOT, "%.2f", entry.averageScore())
                        + "," + entry.bestHardViolations()
                        + "," + entry.bestSoftPenalty());
                writer.newLine();
            }
        }
    }

    private void writeBestSolutionAssignments(Path output, ParseResult problem, GAResult result) throws IOException {
        Map<String, ItcClass> classesById = new HashMap<>();
        for (ItcClass itcClass : problem.classes()) {
            classesById.put(itcClass.id(), itcClass);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("classId,courseId,configId,subpartId,roomId,days,weeks,start,length,timePenalty,roomPenalty");
            writer.newLine();
            for (ClassAssignment assignment : result.bestSolution().assignments()) {
                ItcClass itcClass = classesById.get(assignment.classId());
                ClassTimeOption time = assignment.selectedTimeOption();
                Optional<ClassRoomOption> room = assignment.selectedRoomOption();

                writer.write(csv(assignment.classId()));
                writer.write(",");
                writer.write(csv(itcClass == null ? "" : itcClass.courseId()));
                writer.write(",");
                writer.write(csv(itcClass == null ? "" : itcClass.configId()));
                writer.write(",");
                writer.write(csv(itcClass == null ? "" : itcClass.subpartId()));
                writer.write(",");
                writer.write(csv(room.map(ClassRoomOption::roomId).orElse("")));
                writer.write(",");
                writer.write(csv(time.days()));
                writer.write(",");
                writer.write(csv(time.weeks()));
                writer.write(",");
                writer.write(Integer.toString(time.start()));
                writer.write(",");
                writer.write(Integer.toString(time.length()));
                writer.write(",");
                writer.write(Integer.toString(time.penalty()));
                writer.write(",");
                writer.write(Integer.toString(room.map(ClassRoomOption::penalty).orElse(0)));
                writer.newLine();
            }
        }
    }

    private void appendJsonField(StringBuilder json, String name, String value, boolean trailingComma) {
        appendJsonField(json, name, quote(value), trailingComma, 2);
    }

    private void appendJsonField(StringBuilder json, String name, long value, boolean trailingComma) {
        appendJsonField(json, name, Long.toString(value), trailingComma, 2);
    }

    private void appendJsonField(StringBuilder json, String name, long value, boolean trailingComma, int indent) {
        appendJsonField(json, name, Long.toString(value), trailingComma, indent);
    }

    private void appendJsonField(StringBuilder json, String name, double value, boolean trailingComma) {
        appendJsonField(json, name, Double.toString(value), trailingComma, 2);
    }

    private void appendJsonField(StringBuilder json, String name, double value, boolean trailingComma, int indent) {
        appendJsonField(json, name, Double.toString(value), trailingComma, indent);
    }

    private void appendJsonField(StringBuilder json, String name, String value, boolean trailingComma, int indent) {
        json.append(" ".repeat(indent));
        json.append(quote(name)).append(": ").append(value);
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private String quote(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }
}
