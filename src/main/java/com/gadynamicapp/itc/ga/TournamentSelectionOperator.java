package com.gadynamicapp.itc.ga;

import java.util.Random;

public final class TournamentSelectionOperator implements SelectionOperator {
    @Override
    public EvaluatedChromosome select(Population population, GAConfig config, Random random) {
        int tournamentSize = Math.min(config.tournamentSize(), population.individuals().size());
        EvaluatedChromosome best = null;
        for (int i = 0; i < tournamentSize; i++) {
            EvaluatedChromosome candidate = population.individuals()
                    .get(random.nextInt(population.individuals().size()));
            if (best == null || candidate.fitnessResult().totalScore() < best.fitnessResult().totalScore()) {
                best = candidate;
            }
        }
        return best;
    }
}
