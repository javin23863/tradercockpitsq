/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.util.ArrayList;

public class LimitedList<E>
extends ArrayList<E> {
    private int limit;

    public LimitedList(int n) {
        this.limit = n;
    }

    @Override
    public boolean add(E e) {
        boolean bl = super.add(e);
        while (bl && this.size() > this.limit) {
            super.remove(0);
        }
        return bl;
    }
}

