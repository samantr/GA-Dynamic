package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.parser.ParseResult;

import java.util.Random;

public interface MutationOperator {
    Chromosome mutate(Chromosome chromosome, ParseResult problem, GAConfig config, Random random);
}
