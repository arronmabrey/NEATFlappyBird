package fr.neatmonster.labs.neat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Pool {
    public static final int POPULATION    = 50;
    public static final int STALE_SPECIES = 15;
    public static final int INPUTS        = 4;
    public static final int OUTPUTS       = 1;
    public static final int TIMEOUT       = 20;

    public static final double DELTA_DISJOINT  = 2.0;
    public static final double DELTA_WEIGHTS   = 0.4;
    public static final double DELTA_THRESHOLD = 1.0;

    public static final double CONN_MUTATION    = 0.25;
    public static final double LINK_MUTATION    = 2.0;
    public static final double BIAS_MUTATION    = 0.4;
    public static final double NODE_MUTATION    = 0.5;
    public static final double ENABLE_MUTATION  = 0.2;
    public static final double DISABLE_MUTATION = 0.4;
    public static final double STEP_SIZE        = 0.1;
    public static final double PERTURBATION     = 0.9;
    public static final double CROSSOVER        = 0.75;

    public static final Random rnd = new Random();

    public static final List<Species> species    = new ArrayList<>();
    public static int                 generation = 0;
    public static int                 innovation = OUTPUTS;
    public static double              maxFitness = 0.0;

    public static void addToSpecies(final Genome child) {
        for (final Species species : Pool.species)
            if (child.sameSpecies(species.genomes.get(0))) {
                species.genomes.add(child);
                return;
            }

        final Species childSpecies = new Species();
        childSpecies.genomes.add(child);
        species.add(childSpecies);
    }

    public static void cullSpecies(final boolean cutToOne) {
        for (final Species species : Pool.species) {
            Collections.sort(species.genomes, new Comparator<Genome>() {

                @Override
                public int compare(final Genome o1, final Genome o2) {
                    final double cmp = o2.fitness - o1.fitness;
                    return cmp == 0.0 ? 0 : cmp > 0.0 ? 1 : -1;
                }
            });

            double remaining = Math.ceil(species.genomes.size() / 2.0);
            if (cutToOne)
                remaining = 1.0;

            while (species.genomes.size() > remaining)
                species.genomes.remove(species.genomes.size() - 1);
        }
    }

    public static void initializePool() {
        for (int i = 0; i < POPULATION; ++i) {
            final Genome basic = new Genome();
            basic.maxNeuron = INPUTS;
            basic.mutate();
            addToSpecies(basic);
        }
    }

    public static void newGeneration() {
        cullSpecies(false);
        rankGlobally();
        removeStaleSpecies();
        rankGlobally();
        for (final Species species : Pool.species)
            species.calculateAverageFitness();
        removeWeakSpecies();
        final double sum = totalAverageFitness();
        final List<Genome> children = new ArrayList<Genome>();
        for (final Species species : Pool.species) {
            final double breed = Math
                    .floor(species.averageFitness / sum * POPULATION) - 1.0;
            for (int i = 0; i < breed; ++i)
                children.add(species.breedChild());
        }
        cullSpecies(true);
        while (children.size() + species.size() < POPULATION) {
            final Species species = Pool.species
                    .get(rnd.nextInt(Pool.species.size()));
            children.add(species.breedChild());
        }
        for (final Genome child : children)
            addToSpecies(child);
        ++generation;
    }

    public static void rankGlobally() {
        final List<Genome> global = new ArrayList<Genome>();
        for (final Species species : Pool.species)
            for (final Genome genome : species.genomes)
                global.add(genome);

        Collections.sort(global, new Comparator<Genome>() {

            @Override
            public int compare(final Genome o1, final Genome o2) {
                final double cmp = o1.fitness - o2.fitness;
                return cmp == 0 ? 0 : cmp > 0 ? 1 : -1;
            }
        });

        for (int i = 0; i < global.size(); ++i)
            global.get(i).globalRank = i;
    }

    public static void removeStaleSpecies() {
        final List<Species> survived = new ArrayList<Species>();
        for (final Species species : Pool.species) {
            Collections.sort(species.genomes, new Comparator<Genome>() {

                @Override
                public int compare(final Genome o1, final Genome o2) {
                    final double cmp = o2.fitness - o1.fitness;
                    return cmp == 0 ? 0 : cmp > 0 ? 1 : -1;
                }
            });

            if (species.genomes.get(0).fitness > species.topFitness) {
                species.topFitness = species.genomes.get(0).fitness;
                species.staleness = 0;
            } else
                ++species.staleness;

            if (species.staleness < STALE_SPECIES
                    || species.topFitness >= maxFitness)
                survived.add(species);
        }

        species.clear();
        species.addAll(survived);
    }

    public static void removeWeakSpecies() {
        final List<Species> survived = new ArrayList<Species>();

        final double sum = totalAverageFitness();
        for (final Species species : Pool.species) {
            final double breed = Math
                    .floor(species.averageFitness / sum * POPULATION);
            if (breed >= 1.0)
                survived.add(species);
        }

        species.clear();
        species.addAll(survived);
    }

    public static double totalAverageFitness() {
        double total = 0;
        for (final Species species : Pool.species)
            total += species.averageFitness;
        return total;
    }
}
