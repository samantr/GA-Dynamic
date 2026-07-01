package com.gadynamicapp.itc.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AppRunSettings {

    private AppRunSettings() {
    }

    /*
     * IntelliJ-friendly run settings.
     *
     * How it works:
     * - If IntelliJ Program Arguments is empty, App.java will use this class.
     * - If Program Arguments is not empty, command-line arguments still win.
     */
    public static final boolean USE_SETTINGS_WHEN_NO_ARGUMENTS = true;

    /*
     * Dataset settings.
     * Change these when you want to test another ITC dataset.
     */
    public static final String PROFILE_NAME = "tg-spr18 baseline GA";
    public static final String DATASET_NAME = "tg-spr18";
    public static final Path DATASET_FILE = Path.of("Dataset", "tg-spr18_postcompetition2.xml");

    /*
     * Action flags.
     * You can enable more than one if needed.
     */
    public static final boolean SUMMARY = false;
    public static final boolean VALIDATE = false;
    public static final boolean RANDOM_SOLUTION = false;
    public static final boolean SCORE_RANDOM_SOLUTION = false;
    public static final boolean RUN_GA = true;

    /*
     * GA settings.
     * Change these for your experiments.
     */
    public static final int POPULATION_SIZE = 50;
    public static final int GENERATIONS = 100;
    public static final double MUTATION_RATE = 0.05;
    public static final long SEED = 42L;

    public static String[] toCliArgs() {
        List<String> args = new ArrayList<>();

        args.add("--input");
        args.add(DATASET_FILE.toString());

        if (SUMMARY) {
            args.add("--summary");
        }

        if (VALIDATE) {
            args.add("--validate");
        }

        if (RANDOM_SOLUTION) {
            args.add("--random-solution");
        }

        if (SCORE_RANDOM_SOLUTION) {
            args.add("--score-random-solution");
        }

        if (RUN_GA) {
            args.add("--run-ga");

            args.add("--population");
            args.add(Integer.toString(POPULATION_SIZE));

            args.add("--generations");
            args.add(Integer.toString(GENERATIONS));

            args.add("--mutation-rate");
            args.add(Double.toString(MUTATION_RATE));

            args.add("--seed");
            args.add(Long.toString(SEED));
        } else {
            args.add("--seed");
            args.add(Long.toString(SEED));
        }

        return args.toArray(String[]::new);
    }

    public static String describe() {
        return "profile=" + PROFILE_NAME
                + ", datasetName=" + DATASET_NAME
                + ", datasetFile=" + DATASET_FILE
                + ", summary=" + SUMMARY
                + ", validate=" + VALIDATE
                + ", randomSolution=" + RANDOM_SOLUTION
                + ", scoreRandomSolution=" + SCORE_RANDOM_SOLUTION
                + ", runGa=" + RUN_GA
                + ", populationSize=" + POPULATION_SIZE
                + ", generations=" + GENERATIONS
                + ", mutationRate=" + MUTATION_RATE
                + ", seed=" + SEED;
    }
}