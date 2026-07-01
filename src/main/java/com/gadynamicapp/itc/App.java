package com.gadynamicapp.itc;

import com.gadynamicapp.itc.experiment.ExperimentRunOutput;
import com.gadynamicapp.itc.experiment.ExperimentRunOutputWriter;
import com.gadynamicapp.itc.fitness.FitnessResult;
import com.gadynamicapp.itc.fitness.FitnessSummaryService;
import com.gadynamicapp.itc.fitness.StandardItcFitnessEvaluator;
import com.gadynamicapp.itc.ga.GAConfig;
import com.gadynamicapp.itc.ga.GAResult;
import com.gadynamicapp.itc.ga.GASummaryService;
import com.gadynamicapp.itc.ga.StandardGeneticAlgorithm;
import com.gadynamicapp.itc.parser.ItcParseException;
import com.gadynamicapp.itc.parser.ItcXmlParser;
import com.gadynamicapp.itc.parser.ParseResult;
import com.gadynamicapp.itc.solution.RandomSolutionGenerator;
import com.gadynamicapp.itc.solution.SolutionSummaryService;
import com.gadynamicapp.itc.solution.TimetableSolution;
import com.gadynamicapp.itc.summary.ItcProblemSummaryService;
import com.gadynamicapp.itc.validation.ItcProblemValidator;
import com.gadynamicapp.itc.validation.ValidationIssue;
import com.gadynamicapp.itc.validation.ValidationResult;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        }

        if (options.help()) {
            printUsage(out);
            return 0;
        }

        if (options.input() == null) {
            err.println("Missing required option: --input <xml-file>");
            printUsage(err);
            return 2;
        }

        Path input = Path.of(options.input());
        try {
            ParseResult result = new ItcXmlParser().parse(input);
            boolean shouldPrintSummary = options.summary() || options.noActionSelected();
            boolean printedSection = false;

            if (shouldPrintSummary) {
                new ItcProblemSummaryService().print(out, input, result);
                printedSection = true;
            }

            if (options.validate()) {
                if (printedSection) {
                    out.println();
                }
                ValidationResult validationResult = new ItcProblemValidator().validate(result);
                printValidation(out, validationResult);
                printedSection = true;
                if (!validationResult.valid()) {
                    return 1;
                }
            }

            if (options.randomSolution() || options.scoreRandomSolution()) {
                if (printedSection) {
                    out.println();
                }
                TimetableSolution solution = new RandomSolutionGenerator().generate(result, options.seed());

                if (options.randomSolution()) {
                    new SolutionSummaryService().print(out, solution);
                    printedSection = true;
                }

                if (options.scoreRandomSolution()) {
                    if (printedSection) {
                        out.println();
                    }
                    FitnessResult fitnessResult = new StandardItcFitnessEvaluator().evaluate(result, solution);
                    new FitnessSummaryService().print(out, fitnessResult);
                    printedSection = true;
                }
            }

            if (options.runGa()) {
                if (printedSection) {
                    out.println();
                }
                GAConfig gaConfig = new GAConfig(
                        options.populationSize(),
                        options.generations(),
                        GAConfig.DEFAULT_TOURNAMENT_SIZE,
                        options.mutationRate(),
                        GAConfig.DEFAULT_ELITISM_COUNT,
                        options.seed()
                );
                GAResult gaResult = new StandardGeneticAlgorithm().run(result, gaConfig);
                ExperimentRunOutput output = new ExperimentRunOutputWriter().write(input, result, gaResult);
                new GASummaryService().print(out, gaResult, output);
            }

            return 0;
        } catch (ItcParseException ex) {
            err.println("Failed to parse ITC XML: " + ex.getMessage());
            return 1;
        } catch (IOException ex) {
            err.println("I/O failure: " + ex.getMessage());
            return 1;
        } catch (IllegalArgumentException ex) {
            err.println("Failed to process solution: " + ex.getMessage());
            return 1;
        }
    }

    private static void printUsage(PrintStream stream) {
        stream.println("Usage: java -jar target/*.jar --input <itc-2019-xml-file> [--summary] [--validate] [--random-solution] [--score-random-solution] [--run-ga] [--population <int>] [--generations <int>] [--mutation-rate <0..1>] [--seed <long>]");
    }

    private static void printValidation(PrintStream stream, ValidationResult validationResult) {
        stream.println("ITC Problem Validation");
        stream.println("Status: " + (validationResult.valid() ? "PASSED" : "FAILED"));
        stream.println("Issues: " + validationResult.issueCount());
        for (ValidationIssue issue : validationResult.issues()) {
            stream.println("  - " + issue.location() + ": " + issue.message());
        }
    }

    private record CliOptions(
            String input,
            boolean summary,
            boolean validate,
            boolean randomSolution,
            boolean scoreRandomSolution,
            boolean runGa,
            int populationSize,
            int generations,
            double mutationRate,
            long seed,
            boolean help
    ) {
        private static final long DEFAULT_SEED = 0L;

        boolean noActionSelected() {
            return !summary && !validate && !randomSolution && !scoreRandomSolution && !runGa;
        }

        static CliOptions parse(String[] args) {
            String input = null;
            boolean summary = false;
            boolean validate = false;
            boolean randomSolution = false;
            boolean scoreRandomSolution = false;
            boolean runGa = false;
            int populationSize = GAConfig.DEFAULT_POPULATION_SIZE;
            int generations = GAConfig.DEFAULT_GENERATIONS;
            double mutationRate = GAConfig.DEFAULT_MUTATION_RATE;
            long seed = DEFAULT_SEED;
            boolean help = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--input", "-i" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Option " + arg + " requires a value.");
                        }
                        input = args[++i];
                    }
                    case "--summary" -> summary = true;
                    case "--validate" -> validate = true;
                    case "--random-solution" -> randomSolution = true;
                    case "--score-random-solution" -> scoreRandomSolution = true;
                    case "--run-ga" -> runGa = true;
                    case "--population" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Option " + arg + " requires a value.");
                        }
                        populationSize = parsePositiveInt(arg, args[++i]);
                    }
                    case "--generations" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Option " + arg + " requires a value.");
                        }
                        generations = parseNonNegativeInt(arg, args[++i]);
                    }
                    case "--mutation-rate" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Option " + arg + " requires a value.");
                        }
                        mutationRate = parseProbability(arg, args[++i]);
                    }
                    case "--seed" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Option " + arg + " requires a value.");
                        }
                        seed = parseSeed(args[++i]);
                    }
                    case "--help", "-h" -> help = true;
                    default -> throw new IllegalArgumentException("Unknown option: " + arg);
                }
            }

            return new CliOptions(
                    input,
                    summary,
                    validate,
                    randomSolution,
                    scoreRandomSolution,
                    runGa,
                    populationSize,
                    generations,
                    mutationRate,
                    seed,
                    help
            );
        }

        private static int parsePositiveInt(String option, String value) {
            int parsed = parseInt(option, value);
            if (parsed <= 0) {
                throw new IllegalArgumentException("Option " + option + " must be greater than 0.");
            }
            return parsed;
        }

        private static int parseNonNegativeInt(String option, String value) {
            int parsed = parseInt(option, value);
            if (parsed < 0) {
                throw new IllegalArgumentException("Option " + option + " must be non-negative.");
            }
            return parsed;
        }

        private static int parseInt(String option, String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Option " + option + " requires a whole number, got '" + value + "'.", ex);
            }
        }

        private static double parseProbability(String option, String value) {
            try {
                double parsed = Double.parseDouble(value);
                if (Double.isNaN(parsed) || parsed < 0.0 || parsed > 1.0) {
                    throw new IllegalArgumentException("Option " + option + " must be between 0.0 and 1.0.");
                }
                return parsed;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Option " + option + " requires a decimal number, got '" + value + "'.", ex);
            }
        }

        private static long parseSeed(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Option --seed requires a whole number, got '" + value + "'.", ex);
            }
        }
    }
}