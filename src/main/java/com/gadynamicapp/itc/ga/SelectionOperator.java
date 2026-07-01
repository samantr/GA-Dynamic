package com.gadynamicapp.itc.ga;

import java.util.Random;

public interface SelectionOperator {
    EvaluatedChromosome select(Population population, GAConfig config, Random random);
}
