package com.gadynamicapp.itc.experiment;

import com.gadynamicapp.itc.fitness.ConstraintViolation;
import com.gadynamicapp.itc.fitness.FitnessBreakdown;
import com.gadynamicapp.itc.fitness.FitnessResult;
import com.gadynamicapp.itc.ga.GAConfig;
import com.gadynamicapp.itc.ga.GAHistoryEntry;
import com.gadynamicapp.itc.ga.GAResult;
import com.gadynamicapp.itc.model.Distribution;
import com.gadynamicapp.itc.model.DistributionClassRef;
import com.gadynamicapp.itc.model.ItcConfig;
import com.gadynamicapp.itc.model.OptimizationWeights;
import com.gadynamicapp.itc.parser.ParseResult;
import com.gadynamicapp.itc.solution.TimetableSolution;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentRunOutputWriterTest {
    private static final String SPECIAL_TEXT = "Problem \"quoted\" \\ path \n tab\t carriage\r control\u0001 end";
    private static final String SPECIAL_TYPE = "Unknown \"type\" \\ path \n tab\t carriage\r control\u0001 end";
    private static final String ESCAPED_SPECIAL_TEXT =
            "Problem \\\"quoted\\\" \\\\ path \\n tab\\t carriage\\r control\\u0001 end";
    private static final String ESCAPED_SPECIAL_TYPE =
            "Unknown \\\"type\\\" \\\\ path \\n tab\\t carriage\\r control\\u0001 end";

    @Test
    void generatedExperimentJsonFilesAreValidAndHaveNoTrailingCommas() throws Exception {
        withSampleOutput(output -> {
            String runConfigJson = Files.readString(output.runConfig());
            String problemSummaryJson = Files.readString(output.problemSummary());
            String finalFitnessJson = Files.readString(output.finalFitnessBreakdown());

            Map<String, Object> runConfig = parseJsonObject(runConfigJson);
            Map<String, Object> problemSummary = parseJsonObject(problemSummaryJson);
            Map<String, Object> finalFitnessBreakdown = parseJsonObject(finalFitnessJson);

            assertNoTrailingCommas(runConfigJson);
            assertNoTrailingCommas(problemSummaryJson);
            assertNoTrailingCommas(finalFitnessJson);

            assertEquals(SPECIAL_TEXT, runConfig.get("problemName"));
            assertEquals(SPECIAL_TEXT, problemSummary.get("problemName"));
            assertEquals(5L, runConfig.get("populationSize"));
            assertEquals(2L, runConfig.get("generations"));
            assertEquals(0.15, assertInstanceOf(Double.class, runConfig.get("mutationRate")));
            assertEquals(12345L, finalFitnessBreakdown.get("totalScore"));
            assertEquals(1L, finalFitnessBreakdown.get("constraintReportCount"));
            assertInstanceOf(Long.class, finalFitnessBreakdown.get("hardViolationCount"));

            Map<String, Object> roomCapacity = object(problemSummary.get("roomCapacity"));
            assertEquals(0L, roomCapacity.get("minimum"));
            assertInstanceOf(Double.class, roomCapacity.get("average"));

            List<Object> distributionTypes = array(problemSummary.get("distributionTypes"));
            assertEquals(1, distributionTypes.size());
            assertEquals(SPECIAL_TYPE, object(distributionTypes.get(0)).get("type"));
            assertEquals(List.of(SPECIAL_TYPE), array(problemSummary.get("unsupportedOrUnknownDistributionTypes")));
        });
    }

    @Test
    void generatedExperimentJsonEscapesSpecialStringCharacters() throws Exception {
        withSampleOutput(output -> {
            String runConfigJson = Files.readString(output.runConfig());
            String problemSummaryJson = Files.readString(output.problemSummary());

            assertTrue(runConfigJson.contains(ESCAPED_SPECIAL_TEXT));
            assertTrue(problemSummaryJson.contains(ESCAPED_SPECIAL_TEXT));
            assertTrue(problemSummaryJson.contains(ESCAPED_SPECIAL_TYPE));
            assertFalse(runConfigJson.contains("Problem \"quoted\" \\ path \n tab\t carriage\r"));
            assertFalse(problemSummaryJson.contains("Unknown \"type\" \\ path \n tab\t carriage\r"));
        });
    }

    private static void withSampleOutput(OutputAssertion assertion) throws Exception {
        ExperimentRunOutput output = null;
        try {
            output = new ExperimentRunOutputWriter().write(
                    Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                    problemWithSpecialStrings(),
                    gaResult()
            );
            assertion.accept(output);
        } finally {
            if (output != null) {
                cleanup(output);
            }
        }
    }

    private static ParseResult problemWithSpecialStrings() {
        return new ParseResult(
                new ItcConfig(SPECIAL_TEXT, 5, 2, 288, new OptimizationWeights(1, 2, 3, 4), 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(new Distribution(SPECIAL_TYPE, true, null, List.of(new DistributionClassRef("1"))))
        );
    }

    private static GAResult gaResult() {
        FitnessBreakdown breakdown = new FitnessBreakdown(11L, 22L, 1, 2, 3, 4, 5);
        FitnessResult fitness = new FitnessResult(
                12345L,
                10,
                33L,
                breakdown,
                List.of(new ConstraintViolation("TEST", true, 1L, "synthetic"))
        );
        return new GAResult(
                new GAConfig(5, 2, 3, 0.15, 1, 42L),
                999L,
                new TimetableSolution(42L, List.of()),
                fitness,
                List.of(new GAHistoryEntry(0, 12345L, 12345.0, 10, 33L)),
                1000L
        );
    }

    private static void cleanup(ExperimentRunOutput output) throws IOException {
        Files.deleteIfExists(output.runConfig());
        Files.deleteIfExists(output.problemSummary());
        Files.deleteIfExists(output.finalFitnessBreakdown());
        Files.deleteIfExists(output.convergence());
        Files.deleteIfExists(output.bestSolutionAssignments());
        Files.deleteIfExists(output.runDirectory());
    }

    private static void assertNoTrailingCommas(String json) {
        assertFalse(json.contains(",\n}"));
        assertFalse(json.contains(",\n]"));
        assertFalse(json.contains(",\r\n}"));
        assertFalse(json.contains(",\r\n]"));
    }

    private static Map<String, Object> parseJsonObject(String json) {
        return object(new JsonParser(json).parse());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) assertInstanceOf(Map.class, value);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value) {
        return (List<Object>) assertInstanceOf(List.class, value);
    }

    @FunctionalInterface
    private interface OutputAssertion {
        void accept(ExperimentRunOutput output) throws Exception;
    }

    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text;
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("Unexpected content after JSON value.");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("Expected JSON value.");
            }

            char ch = text.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return object;
            }

            while (true) {
                skipWhitespace();
                String name = parseString();
                skipWhitespace();
                expect(':');
                object.put(name, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return array;
            }

            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return value.toString();
                }
                if (ch < 0x20) {
                    throw error("Unescaped control character in JSON string.");
                }
                if (ch != '\\') {
                    value.append(ch);
                    continue;
                }

                if (index >= text.length()) {
                    throw error("Incomplete JSON string escape.");
                }
                char escape = text.charAt(index++);
                switch (escape) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> throw error("Invalid JSON string escape.");
                }
            }
            throw error("Unterminated JSON string.");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) {
                throw error("Incomplete unicode escape.");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(text.charAt(index++), 16);
                if (digit < 0) {
                    throw error("Invalid unicode escape.");
                }
                value = (value << 4) + digit;
            }
            return (char) value;
        }

        private Object parseNumber() {
            int start = index;
            consume('-');
            if (!consume('0')) {
                consumeDigits();
            }

            boolean floatingPoint = false;
            if (consume('.')) {
                floatingPoint = true;
                consumeDigits();
            }
            if (consume('e') || consume('E')) {
                floatingPoint = true;
                consume('+');
                consume('-');
                consumeDigits();
            }

            if (start == index) {
                throw error("Expected JSON number.");
            }
            String number = text.substring(start, index);
            try {
                return floatingPoint ? Double.valueOf(number) : Long.valueOf(number);
            } catch (NumberFormatException ex) {
                throw error("Invalid JSON number.");
            }
        }

        private void consumeDigits() {
            int start = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Expected digit.");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Invalid JSON literal.");
            }
            index += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t') {
                    return;
                }
                index++;
            }
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'.");
            }
        }

        private boolean consume(char expected) {
            if (index < text.length() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " At character " + index + ".");
        }
    }
}
