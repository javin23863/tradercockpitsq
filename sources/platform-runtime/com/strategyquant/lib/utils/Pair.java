/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

public class Pair<T, V> {
    private T A;
    private V B;

    public Pair(T t, V v) {
        this.A = t;
        this.B = v;
    }

    public T getA() {
        return this.A;
    }

    public void setA(T t) {
        this.A = t;
    }

    public V getB() {
        return this.B;
    }

    public void setB(V v) {
        this.B = v;
    }
}

