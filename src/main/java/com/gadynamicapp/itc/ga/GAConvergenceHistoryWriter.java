package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.parser.ParseResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class GAConvergenceHistoryWriter {
    public Path write(ParseResult problem, GAResult result) throws IOException {
        Path outputDirectory = Path.of("output");
        Files.createDirectories(outputDirectory);

        String problemName = sanitize(problem.config().name());
        Path output = outputDirectory.resolve(
                "convergence_" + problemName + "_seed" + result.config().seed() + ".csv"
        );

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

        return output;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "problem";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
