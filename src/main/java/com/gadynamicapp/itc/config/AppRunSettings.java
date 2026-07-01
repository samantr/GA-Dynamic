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
     * If IntelliJ Program Arguments is empty, App.java uses this class.
     * If Program Arguments is not empty, command-line arguments still win.
     */
    public static final boolean USE_SETTINGS_WHEN_NO_ARGUMENTS = true;

    /*
     * Change only this line most of the time.
     */
    public static final RunProfile ACTIVE_PROFILE = RunProfile.BASELINE_MEDIUM;

    public enum RunProfile {

        VALIDATE_ONLY(
                "tg-spr18 validate only",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                true,
                true,
                false,
                false,
                false,
                50,
                100,
                0.05,
                42L
        ),

        RANDOM_SOLUTION_SCORE(
                "tg-spr18 random solution score",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                true,
                true,
                false,
                true,
                false,
                50,
                100,
                0.05,
                42L
        ),

        BASELINE_SMALL(
                "tg-spr18 baseline small",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                false,
                false,
                false,
                false,
                true,
                25,
                100,
                0.05,
                42L
        ),

        BASELINE_MEDIUM(
                "tg-spr18 baseline medium",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                false,
                false,
                false,
                false,
                true,
                50,
                100,
                0.05,
                42L
        ),

        BASELINE_LARGE(
                "tg-spr18 baseline large",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                false,
                false,
                false,
                false,
                true,
                100,
                100,
                0.05,
                42L
        ),

        BASELINE_LONG(
                "tg-spr18 baseline long",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                false,
                false,
                false,
                false,
                true,
                100,
                500,
                0.05,
                42L
        ),

        HIGH_MUTATION_TEST(
                "tg-spr18 high mutation test",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                false,
                false,
                false,
                false,
                true,
                50,
                100,
                0.10,
                42L
        ),

        LOW_MUTATION_TEST(
                "tg-spr18 low mutation test",
                "tg-spr18",
                Path.of("Dataset", "tg-spr18_postcompetition2.xml"),
                false,
                false,
                false,
                false,
                true,
                50,
                100,
                0.02,
                42L
        );

        private final String profileName;
        private final String datasetName;
        private final Path datasetFile;
        private final boolean summary;
        private final boolean validate;
        private final boolean randomSolution;
        private final boolean scoreRandomSolution;
        private final boolean runGa;
        private final int populationSize;
        private final int generations;
        private final double mutationRate;
        private final long seed;

        RunProfile(
                String profileName,
                String datasetName,
                Path datasetFile,
                boolean summary,
                boolean validate,
                boolean randomSolution,
                boolean scoreRandomSolution,
                boolean runGa,
                int populationSize,
                int generations,
                double mutationRate,
                long seed
        ) {
            this.profileName = profileName;
            this.datasetName = datasetName;
            this.datasetFile = datasetFile;
            this.summary = summary;
            this.validate = validate;
            this.randomSolution = randomSolution;
            this.scoreRandomSolution = scoreRandomSolution;
            this.runGa = runGa;
            this.populationSize = populationSize;
            this.generations = generations;
            this.mutationRate = mutationRate;
            this.seed = seed;
        }
    }

    public static String[] toCliArgs() {
        RunProfile profile = ACTIVE_PROFILE;

        List<String> args = new ArrayList<>();

        args.add("--input");
        args.add(profile.datasetFile.toString());

        if (profile.summary) {
            args.add("--summary");
        }

        if (profile.validate) {
            args.add("--validate");
        }

        if (profile.randomSolution) {
            args.add("--random-solution");
        }

        if (profile.scoreRandomSolution) {
            args.add("--score-random-solution");
        }

        if (profile.runGa) {
            args.add("--run-ga");

            args.add("--population");
            args.add(Integer.toString(profile.populationSize));

            args.add("--generations");
            args.add(Integer.toString(profile.generations));

            args.add("--mutation-rate");
            args.add(Double.toString(profile.mutationRate));
        }

        args.add("--seed");
        args.add(Long.toString(profile.seed));

        return args.toArray(new String[0]);
    }

    public static String describe() {
        RunProfile profile = ACTIVE_PROFILE;

        return "activeProfile=" + profile.name()
                + ", profileName=" + profile.profileName
                + ", datasetName=" + profile.datasetName
                + ", datasetFile=" + profile.datasetFile
                + ", summary=" + profile.summary
                + ", validate=" + profile.validate
                + ", randomSolution=" + profile.randomSolution
                + ", scoreRandomSolution=" + profile.scoreRandomSolution
                + ", runGa=" + profile.runGa
                + ", populationSize=" + profile.populationSize
                + ", generations=" + profile.generations
                + ", mutationRate=" + profile.mutationRate
                + ", seed=" + profile.seed;
    }
}