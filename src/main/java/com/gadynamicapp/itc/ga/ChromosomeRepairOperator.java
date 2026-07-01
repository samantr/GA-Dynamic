package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.parser.ParseResult;

public interface ChromosomeRepairOperator {

    Chromosome repair(Chromosome chromosome, ParseResult problem);
}