package com.gadynamicapp.itc.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class UniformCrossoverOperator implements CrossoverOperator {
    @Override
    public Chromosome crossover(Chromosome firstParent, Chromosome secondParent, Random random) {
        if (firstParent.geneCount() != secondParent.geneCount()) {
            throw new IllegalArgumentException("Parents must have the same number of genes.");
        }

        List<Gene> genes = new ArrayList<>();
        for (int i = 0; i < firstParent.geneCount(); i++) {
            genes.add(random.nextBoolean()
                    ? firstParent.genes().get(i)
                    : secondParent.genes().get(i));
        }
        return new Chromosome(genes);
    }
}
