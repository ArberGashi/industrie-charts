package com.arbergashi.charts.engine.spatial;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector API helpers for spatial math.
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class VectorIntrinsics {
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    private VectorIntrinsics() {
    }

    public static VectorSpecies<Double> getPreferredSpecies() {
        return SPECIES;
    }

    public static int getLaneCount() {
        return SPECIES.length();
    }
}
