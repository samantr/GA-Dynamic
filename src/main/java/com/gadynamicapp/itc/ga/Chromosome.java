package com.gadynamicapp.itc.ga;

import java.util.List;
import java.util.Objects;

public record Chromosome(List<Gene> genes) {
    public Chromosome {
        Objects.requireNonNull(genes, "genes");
        genes = List.copyOf(genes);
    }

    public int geneCount() {
        return genes.size();
    }
}
