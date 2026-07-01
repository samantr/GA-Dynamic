package com.gadynamicapp.itc.fitness;

import com.gadynamicapp.itc.parser.ParseResult;
import com.gadynamicapp.itc.solution.TimetableSolution;

public interface FitnessEvaluator {
    FitnessResult evaluate(ParseResult problem, TimetableSolution solution);
}
