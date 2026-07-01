package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.parser.ParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RandomResetMutationOperator implements MutationOperator {
    @Override
    public Chromosome mutate(Chromosome chromosome, ParseResult problem, GAConfig config, Random random) {
        if (chromosome.geneCount() != problem.classes().size()) {
            throw new IllegalArgumentException("Chromosome gene count must match problem class count.");
        }

        List<Gene> genes = new ArrayList<>();
        for (int i = 0; i < chromosome.geneCount(); i++) {
            Gene gene = chromosome.genes().get(i);
            if (random.nextDouble() >= config.mutationRate()) {
                genes.add(gene);
                continue;
            }

            ItcClass itcClass = problem.classes().get(i);
            genes.add(mutateGene(gene, itcClass, random));
        }
        return new Chromosome(genes);
    }

    private Gene mutateGene(Gene gene, ItcClass itcClass, Random random) {
        boolean roomless = itcClass.roomOptions().isEmpty();
        boolean mutateTime = roomless || random.nextBoolean();
        boolean mutateRoom = !roomless && random.nextBoolean();
        if (!mutateTime && !mutateRoom) {
            mutateTime = true;
        }

        int timeOptionIndex = mutateTime
                ? random.nextInt(itcClass.timeOptions().size())
                : gene.timeOptionIndex();
        int roomOptionIndex = roomless
                ? Gene.NO_ROOM
                : mutateRoom ? random.nextInt(itcClass.roomOptions().size()) : gene.roomOptionIndex();

        return new Gene(timeOptionIndex, roomOptionIndex);
    }
}
