package com.gadynamicapp.itc.ga;

import java.util.Random;

public interface CrossoverOperator {
    Chromosome crossover(Chromosome firstParent, Chromosome secondParent, Random random);
}
