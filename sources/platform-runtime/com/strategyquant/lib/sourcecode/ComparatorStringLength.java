/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import com.strategyquant.lib.sourcecode.VarAndIndex;
import java.util.Comparator;

public class ComparatorStringLength
implements Comparator<VarAndIndex> {
    @Override
    public int compare(VarAndIndex varAndIndex, VarAndIndex varAndIndex2) {
        return -1 * Integer.compare(varAndIndex.var.length(), varAndIndex2.var.length());
    }
}

