package fr.neatmonster.labs.neat;

public class Synapse {
    public int     input      = 0;
    public int     output     = 0;
    public double  weight     = 0.0;
    public boolean enabled    = true;
    public int     innovation = 0;

    @Override
    public Synapse clone() {
        final Synapse synapse = new Synapse();
        synapse.input = input;
        synapse.output = output;
        synapse.weight = weight;
        synapse.enabled = enabled;
        synapse.innovation = innovation;
        return synapse;
    }
}
