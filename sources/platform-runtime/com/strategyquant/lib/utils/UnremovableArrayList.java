/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class UnremovableArrayList<T>
implements Iterable<T>,
Serializable {
    protected ArrayList<T> list = new ArrayList();

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>(){
            private final Iterator<T> iter;
            {
                this.iter = UnremovableArrayList.this.list.iterator();
            }

            @Override
            public boolean hasNext() {
                return this.iter.hasNext();
            }

            @Override
            public T next() {
                return this.iter.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("No changes allowed");
            }
        };
    }

    public int size() {
        return this.list.size();
    }

    public void add(T t) {
        this.list.add(t);
    }

    public void addAll(ArrayList<T> arrayList) {
        this.list.addAll(arrayList);
    }

    public void addAll(UnremovableArrayList<T> unremovableArrayList) {
        for (T t : unremovableArrayList) {
            this.list.add(t);
        }
    }
}

