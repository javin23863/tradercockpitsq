/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.random;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.random.MTRandom;
import com.strategyquant.lib.random.MersenneTwisterFast;
import java.util.Random;

public class MersenneTwisterRng
implements IRandomGenerator {
    private MersenneTwisterFast rng = null;
    private MTRandom randomObj;

    public MersenneTwisterRng() {
        this.rng = new MersenneTwisterFast();
        this.randomObj = new MTRandom();
    }

    public MersenneTwisterRng(long l) {
        this.rng = new MersenneTwisterFast(l);
        this.randomObj = new MTRandom(l);
    }

    @Override
    public int nextInt(int n) {
        return this.rng.nextInt(n);
    }

    @Override
    public IRandomGenerator newInstance(long l) {
        return new MersenneTwisterRng(l);
    }

    @Override
    public double nextDouble() {
        return this.rng.nextDouble();
    }

    @Override
    public double nextGaussian() {
        return this.rng.nextGaussian();
    }

    @Override
    public boolean probability(double d) {
        return d == 1.0 || this.rng.nextDouble() < d;
    }

    @Override
    public Random getRandom() {
        return this.randomObj;
    }

    @Override
    public long nextLong() {
        return this.rng.nextLong();
    }

    @Override
    public IRandomGenerator clone(long l) {
        return new MersenneTwisterRng(l);
    }

    @Override
    public double nextDouble(double d, double d2, double d3, int n) {
        int n2 = (int)Math.ceil((d2 - d) / d3);
        int n3 = this.nextInt(n2 + 1);
        double d4 = d + (double)n3 * d3;
        if (n > 0) {
            d4 = SQUtils.round(d4, n);
        }
        return d4;
    }

    @Override
    public boolean nextBool() {
        return this.rng.nextInt(2) == 0;
    }

    @Override
    public int nextInt(int n, int n2, int n3) {
        int n4 = (n2 - n) / n3;
        int n5 = this.nextInt(n4 + 1);
        return n + n5 * n3;
    }
}

