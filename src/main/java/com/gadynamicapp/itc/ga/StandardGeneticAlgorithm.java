package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.fitness.FitnessEvaluator;
import com.gadynamicapp.itc.fitness.FitnessResult;
import com.gadynamicapp.itc.fitness.StandardItcFitnessEvaluator;
import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.parser.ParseResult;
import com.gadynamicapp.itc.solution.ClassAssignment;
import com.gadynamicapp.itc.solution.TimetableSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class StandardGeneticAlgorithm {

    private final FitnessEvaluator fitnessEvaluator;
    private final SelectionOperator selectionOperator;
    private final CrossoverOperator crossoverOperator;
    private final MutationOperator mutationOperator;
    private final ChromosomeRepairOperator repairOperator;

    public StandardGeneticAlgorithm() {
        this(
                new StandardItcFitnessEvaluator(false),
                new TournamentSelectionOperator(),
                new UniformCrossoverOperator(),
                new RandomResetMutationOperator(),
                new RoomConflictRepairOperator()
        );
    }

    public StandardGeneticAlgorithm(
            FitnessEvaluator fitnessEvaluator,
            SelectionOperator selectionOperator,
            CrossoverOperator crossoverOperator,
            MutationOperator mutationOperator
    ) {
        this(
                fitnessEvaluator,
                selectionOperator,
                crossoverOperator,
                mutationOperator,
                new RoomConflictRepairOperator()
        );
    }

    public StandardGeneticAlgorithm(
            FitnessEvaluator fitnessEvaluator,
            SelectionOperator selectionOperator,
            CrossoverOperator crossoverOperator,
            MutationOperator mutationOperator,
            ChromosomeRepairOperator repairOperator
    ) {
        this.fitnessEvaluator = Objects.requireNonNull(fitnessEvaluator, "fitnessEvaluator");
        this.selectionOperator = Objects.requireNonNull(selectionOperator, "selectionOperator");
        this.crossoverOperator = Objects.requireNonNull(crossoverOperator, "crossoverOperator");
        this.mutationOperator = Objects.requireNonNull(mutationOperator, "mutationOperator");
        this.repairOperator = Objects.requireNonNull(repairOperator, "repairOperator");
    }

    public GAResult run(ParseResult problem, GAConfig config) {
        long startNanos = System.nanoTime();

        Random random = new Random(config.seed());
        Population population = createInitialPopulation(problem, config, random);

        List<GAHistoryEntry> history = new ArrayList<>();
        history.add(toHistoryEntry(0, population));

        long initialBestScore = population.best().fitnessResult().totalScore();
        EvaluatedChromosome bestEver = population.best();

        for (int generation = 1; generation <= config.generations(); generation++) {
            List<Chromosome> nextGeneration = new ArrayList<>();

            for (EvaluatedChromosome elite : population.elites(config.elitismCount())) {
                nextGeneration.add(elite.chromosome());
            }

            while (nextGeneration.size() < config.populationSize()) {
                EvaluatedChromosome firstParent = selectionOperator.select(population, config, random);
                EvaluatedChromosome secondParent = selectionOperator.select(population, config, random);

                Chromosome child = crossoverOperator.crossover(
                        firstParent.chromosome(),
                        secondParent.chromosome(),
                        random
                );

                child = mutationOperator.mutate(child, problem, config, random);
                child = repairOperator.repair(child, problem);

                nextGeneration.add(child);
            }

            population = evaluatePopulation(problem, nextGeneration, config.seed());

            if (population.best().fitnessResult().totalScore() < bestEver.fitnessResult().totalScore()) {
                bestEver = population.best();
            }

            history.add(toHistoryEntry(generation, population));
        }

        TimetableSolution bestSolution = toSolution(problem, bestEver.chromosome(), config.seed());
        FitnessResult detailedBestFitness = new StandardItcFitnessEvaluator().evaluate(problem, bestSolution);

        long runtimeMillis = (System.nanoTime() - startNanos) / 1_000_000L;

        return new GAResult(
                config,
                initialBestScore,
                bestSolution,
                detailedBestFitness,
                history,
                runtimeMillis
        );
    }

    private Population createInitialPopulation(ParseResult problem, GAConfig config, Random random) {
        List<Chromosome> chromosomes = new ArrayList<>();

        for (int i = 0; i < config.populationSize(); i++) {
            Chromosome chromosome = randomChromosome(problem, random);
            chromosome = repairOperator.repair(chromosome, problem);
            chromosomes.add(chromosome);
        }

        return evaluatePopulation(problem, chromosomes, config.seed());
    }

    private Chromosome randomChromosome(ParseResult problem, Random random) {
        List<Gene> genes = new ArrayList<>();

        for (ItcClass itcClass : problem.classes()) {
            if (itcClass.timeOptions().isEmpty()) {
                throw new IllegalArgumentException(
                        "Class '" + itcClass.id() + "' has no allowed time options."
                );
            }

            int timeOptionIndex = random.nextInt(itcClass.timeOptions().size());
            int roomOptionIndex = itcClass.roomOptions().isEmpty()
                    ? Gene.NO_ROOM
                    : random.nextInt(itcClass.roomOptions().size());

            genes.add(new Gene(timeOptionIndex, roomOptionIndex));
        }

        return new Chromosome(genes);
    }

    private Population evaluatePopulation(ParseResult problem, List<Chromosome> chromosomes, long seed) {
        List<EvaluatedChromosome> evaluated = new ArrayList<>();

        for (Chromosome chromosome : chromosomes) {
            TimetableSolution solution = toSolution(problem, chromosome, seed);

            evaluated.add(new EvaluatedChromosome(
                    chromosome,
                    solution,
                    fitnessEvaluator.evaluate(problem, solution)
            ));
        }

        return new Population(evaluated);
    }

    private GAHistoryEntry toHistoryEntry(int generation, Population population) {
        FitnessResult bestFitness = population.best().fitnessResult();

        return new GAHistoryEntry(
                generation,
                bestFitness.totalScore(),
                population.averageScore(),
                bestFitness.hardViolationCount(),
                bestFitness.softPenaltyTotal()
        );
    }

    private TimetableSolution toSolution(ParseResult problem, Chromosome chromosome, long seed) {
        if (chromosome.geneCount() != problem.classes().size()) {
            throw new IllegalArgumentException("Chromosome gene count must match problem class count.");
        }

        List<ClassAssignment> assignments = new ArrayList<>();

        for (int i = 0; i < chromosome.geneCount(); i++) {
            ItcClass itcClass = problem.classes().get(i);
            Gene gene = chromosome.genes().get(i);

            ClassTimeOption selectedTime = selectTimeOption(itcClass, gene);
            Optional<ClassRoomOption> selectedRoom = selectRoomOption(itcClass, gene);

            assignments.add(new ClassAssignment(itcClass.id(), selectedTime, selectedRoom));
        }

        return new TimetableSolution(seed, assignments);
    }

    private ClassTimeOption selectTimeOption(ItcClass itcClass, Gene gene) {
        if (gene.timeOptionIndex() >= itcClass.timeOptions().size()) {
            throw new IllegalArgumentException(
                    "Gene for class '" + itcClass.id() + "' references an invalid time option."
            );
        }

        return itcClass.timeOptions().get(gene.timeOptionIndex());
    }

    private Optional<ClassRoomOption> selectRoomOption(ItcClass itcClass, Gene gene) {
        if (itcClass.roomOptions().isEmpty()) {
            return Optional.empty();
        }

        if (gene.roomOptionIndex() < 0 || gene.roomOptionIndex() >= itcClass.roomOptions().size()) {
            throw new IllegalArgumentException(
                    "Gene for class '" + itcClass.id() + "' references an invalid room option."
            );
        }

        return Optional.of(itcClass.roomOptions().get(gene.roomOptionIndex()));
    }
}