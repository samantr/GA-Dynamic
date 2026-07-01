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
import java.util.ArrayDeque;
import java.util.Deque;
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
        JsonWriter json = new JsonWriter()
                .beginObject()
                .field("inputFile", inputFile.toString())
                .field("problemName", problem.config().name())
                .field("populationSize", result.config().populationSize())
                .field("generations", result.config().generations())
                .field("mutationRate", result.config().mutationRate())
                .field("tournamentSize", result.config().tournamentSize())
                .field("elitismCount", result.config().elitismCount())
                .field("seed", result.config().seed())
                .field("javaVersion", System.getProperty("java.version"))
                .field("timestamp", timestamp.format(JSON_TIMESTAMP))
                .endObject();
        Files.writeString(output, json.toJson());
    }

    private void writeProblemSummary(Path output, ItcProblemSummary summary) throws IOException {
        RoomCapacitySummary capacity = summary.roomCapacity();
        JsonWriter json = new JsonWriter()
                .beginObject()
                .field("problemName", summary.problemName())
                .field("nrDays", summary.nrDays())
                .field("nrWeeks", summary.nrWeeks())
                .field("slotsPerDay", summary.slotsPerDay())
                .field("roomCount", summary.roomCount())
                .field("courseCount", summary.courseCount())
                .field("configCount", summary.configCount())
                .field("subpartCount", summary.subpartCount())
                .field("classCount", summary.classCount())
                .field("distributionCount", summary.distributionCount())
                .field("studentCount", summary.studentCount())
                .field("roomUnavailableCount", summary.roomUnavailableCount())
                .field("classRoomOptionCount", summary.classRoomOptionCount())
                .field("classTimeOptionCount", summary.classTimeOptionCount())
                .name("roomCapacity")
                .beginObject()
                .field("minimum", capacity.minimum())
                .field("maximum", capacity.maximum())
                .field("average", capacity.average())
                .endObject()
                .field("classesWithNoRoomOptions", summary.classesWithNoRoomOptions())
                .field("classesWithOneRoomOption", summary.classesWithOneRoomOption())
                .field("classesWithMultipleRoomOptions", summary.classesWithMultipleRoomOptions())
                .field("classesWithNoTimeOptions", summary.classesWithNoTimeOptions())
                .field("classesWithOneTimeOption", summary.classesWithOneTimeOption())
                .field("classesWithMultipleTimeOptions", summary.classesWithMultipleTimeOptions())
                .name("distributionTypes")
                .beginArray();

        for (DistributionTypeSummary distributionType : summary.distributionTypes()) {
            json.beginObject()
                    .field("type", distributionType.type())
                    .field("requiredCount", distributionType.requiredCount())
                    .field("nonRequiredCount", distributionType.nonRequiredCount())
                    .field("totalCount", distributionType.totalCount())
                    .endObject();
        }

        json.endArray()
                .name("unsupportedOrUnknownDistributionTypes")
                .beginArray();
        for (String type : summary.unsupportedOrUnknownDistributionTypes()) {
            json.value(type);
        }
        json.endArray()
                .endObject();

        Files.writeString(output, json.toJson());
    }

    private void writeFinalFitnessBreakdown(Path output, FitnessResult fitness) throws IOException {
        FitnessBreakdown breakdown = fitness.breakdown();
        JsonWriter json = new JsonWriter()
                .beginObject()
                .field("totalScore", fitness.totalScore())
                .field("hardViolationCount", fitness.hardViolationCount())
                .field("softPenaltyTotal", fitness.softPenaltyTotal())
                .field("timeChoicePenalty", breakdown.timeChoicePenalty())
                .field("roomChoicePenalty", breakdown.roomChoicePenalty())
                .field("roomConflictViolationCount", breakdown.roomConflictViolationCount())
                .field("roomUnavailableViolationCount", breakdown.roomUnavailableViolationCount())
                .field("requiredDistributionViolationCount", breakdown.requiredDistributionViolationCount())
                .field("unsupportedRequiredDistributionCount", breakdown.unsupportedRequiredDistributionCount())
                .field("unsupportedNonRequiredDistributionCount", breakdown.unsupportedNonRequiredDistributionCount())
                .field("constraintReportCount", fitness.violations().size())
                .endObject();
        Files.writeString(output, json.toJson());
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

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private static final class JsonWriter {
        private final StringBuilder json = new StringBuilder();
        private final Deque<JsonContext> contexts = new ArrayDeque<>();
        private int indent;
        private boolean expectingFieldValue;

        private JsonWriter beginObject() {
            beforeValue();
            json.append("{");
            contexts.push(new JsonContext(JsonContextType.OBJECT));
            indent += 2;
            return this;
        }

        private JsonWriter endObject() {
            endContext(JsonContextType.OBJECT, "}");
            return this;
        }

        private JsonWriter beginArray() {
            beforeValue();
            json.append("[");
            contexts.push(new JsonContext(JsonContextType.ARRAY));
            indent += 2;
            return this;
        }

        private JsonWriter endArray() {
            endContext(JsonContextType.ARRAY, "]");
            return this;
        }

        private JsonWriter name(String name) {
            if (contexts.isEmpty() || contexts.peek().type != JsonContextType.OBJECT) {
                throw new IllegalStateException("JSON field names can only be written inside objects.");
            }
            if (expectingFieldValue) {
                throw new IllegalStateException("Previous JSON field has no value.");
            }

            JsonContext context = contexts.peek();
            if (!context.first) {
                json.append(",");
            }
            json.append("\n");
            appendIndent();
            appendQuoted(name);
            json.append(": ");
            context.first = false;
            expectingFieldValue = true;
            return this;
        }

        private JsonWriter field(String name, String value) {
            return name(name).value(value);
        }

        private JsonWriter field(String name, long value) {
            return name(name).number(Long.toString(value));
        }

        private JsonWriter field(String name, double value) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("JSON numbers must be finite.");
            }
            return name(name).number(Double.toString(value));
        }

        private JsonWriter value(String value) {
            beforeValue();
            appendQuoted(value);
            return this;
        }

        private JsonWriter number(String value) {
            beforeValue();
            json.append(value);
            return this;
        }

        private String toJson() {
            if (!contexts.isEmpty()) {
                throw new IllegalStateException("JSON document has unclosed containers.");
            }
            if (expectingFieldValue) {
                throw new IllegalStateException("JSON document has a field without a value.");
            }
            return json.toString() + "\n";
        }

        private void beforeValue() {
            if (expectingFieldValue) {
                expectingFieldValue = false;
                return;
            }
            if (contexts.isEmpty()) {
                return;
            }

            JsonContext context = contexts.peek();
            if (context.type != JsonContextType.ARRAY) {
                throw new IllegalStateException("Object values must be preceded by a field name.");
            }
            if (!context.first) {
                json.append(",");
            }
            json.append("\n");
            appendIndent();
            context.first = false;
        }

        private void endContext(JsonContextType expectedType, String closingToken) {
            if (contexts.isEmpty() || contexts.peek().type != expectedType) {
                throw new IllegalStateException("JSON container nesting is invalid.");
            }
            if (expectingFieldValue) {
                throw new IllegalStateException("JSON field has no value.");
            }

            JsonContext context = contexts.pop();
            indent -= 2;
            if (!context.first) {
                json.append("\n");
                appendIndent();
            }
            json.append(closingToken);
        }

        private void appendIndent() {
            json.append(" ".repeat(indent));
        }

        private void appendQuoted(String value) {
            String safeValue = value == null ? "" : value;
            json.append("\"");
            for (int i = 0; i < safeValue.length(); i++) {
                char ch = safeValue.charAt(i);
                switch (ch) {
                    case '\\' -> json.append("\\\\");
                    case '\"' -> json.append("\\\"");
                    case '\b' -> json.append("\\b");
                    case '\f' -> json.append("\\f");
                    case '\n' -> json.append("\\n");
                    case '\r' -> json.append("\\r");
                    case '\t' -> json.append("\\t");
                    default -> {
                        if (ch <= 0x1F) {
                            appendUnicodeEscape(ch);
                        } else {
                            json.append(ch);
                        }
                    }
                }
            }
            json.append("\"");
        }

        private void appendUnicodeEscape(char ch) {
            json.append("\\u");
            String hex = Integer.toHexString(ch);
            json.append("0".repeat(4 - hex.length()));
            json.append(hex);
        }
    }

    private enum JsonContextType {
        OBJECT,
        ARRAY
    }

    private static final class JsonContext {
        private final JsonContextType type;
        private boolean first = true;

        private JsonContext(JsonContextType type) {
            this.type = type;
        }
    }
}
