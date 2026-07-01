# GA Dynamic Timetabling App

First prototype for loading, validating, summarizing, randomly assigning, scoring, and baseline-GA optimizing ITC 2019 timetabling XML instances.

## Current Scope

- Loads an ITC 2019 XML file from the command line.
- Parses problem metadata, optimization weights, rooms, courses/configs/subparts/classes, class room/time options, distributions, and students.
- Prints a clean summary with counts, room capacity statistics, class option counts, and distribution type totals.
- Validates parsed ITC problem structure before later optimization work.
- Generates a seeded random timetable solution with one assignment per class.
- Scores a timetable solution with choice penalties, room hard constraints, room unavailable hard constraints, and required `NotOverlap` distributions.
- Runs a simple baseline genetic algorithm with tournament selection, uniform crossover, mutation, and elitism.
- Writes paper-oriented experiment outputs for GA runs under `output/runs/<timestamp>/`.
- Reports unsupported required distributions as hard violations and unsupported non-required distributions as soft constraint reports.
- Does not include a database, frontend, AI agent, custom GA, or dynamic fitness functions yet.

## Requirements

- Java 17
- Maven

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/*.jar --input "Dataset/tg-spr18_postcompetition2.xml" --summary --validate
```

Generate a reproducible random timetable solution:

```bash
java -jar target/*.jar --input "Dataset/tg-spr18_postcompetition2.xml" --random-solution --seed 42
```

Score a reproducible random timetable solution:

```bash
java -jar target/*.jar --input "Dataset/tg-spr18_postcompetition2.xml" --score-random-solution --seed 42
```

## Running the first ITC 2019 GA baseline

Use this exact command for the `Dataset/tg-spr18_postcompetition2.xml` instance:

```bash
java -jar target/*.jar --input "Dataset/tg-spr18_postcompetition2.xml" --run-ga --population 50 --generations 100 --mutation-rate 0.05 --seed 42
```

Each GA run writes a timestamped experiment folder under `output/runs/<timestamp>/` containing:

- `run_config.json`
- `problem_summary.json`
- `final_fitness_breakdown.json`
- `convergence.csv`
- `best_solution_assignments.csv`

The `--input` value can point to any compatible ITC 2019 XML file, not only the starter dataset. If no action flag is provided, the app prints the summary by default.

## Test

```bash
mvn test
```