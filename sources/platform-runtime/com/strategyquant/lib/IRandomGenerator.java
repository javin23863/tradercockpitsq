/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import java.util.Random;

public interface IRandomGenerator {
    public int nextInt(int var1);

    public IRandomGenerator newInstance(long var1);

    public double nextDouble();

    public double nextGaussian();

    public boolean probability(double var1);

    public Random getRandom();

    public long nextLong();

    public IRandomGenerator clone(long var1);

    public double nextDouble(double var1, double var3, double var5, int var7);

    public boolean nextBool();

    public int nextInt(int var1, int var2, int var3);
}

