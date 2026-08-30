/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.random;

import java.util.Random;

public class MTRandom
extends Random {
    private static final long serialVersionUID = -515082678588212038L;
    private static final int UPPER_MASK = Integer.MIN_VALUE;
    private static final int LOWER_MASK = Integer.MAX_VALUE;
    private static final int N = 624;
    private static final int M = 397;
    private static final int[] MAGIC = new int[]{0, -1727483681};
    private static final int MAGIC_FACTOR1 = 1812433253;
    private static final int MAGIC_FACTOR2 = 1664525;
    private static final int MAGIC_FACTOR3 = 1566083941;
    private static final int MAGIC_MASK1 = -1658038656;
    private static final int MAGIC_MASK2 = -272236544;
    private static final int MAGIC_SEED = 19650218;
    private static final long DEFAULT_SEED = 5489L;
    private transient int[] mt;
    private transient int mti;
    private transient boolean compat = false;
    private transient int[] ibuf;

    public MTRandom() {
    }

    public MTRandom(boolean bl) {
        super(0L);
        this.compat = bl;
        this.setSeed(this.compat ? 5489L : System.currentTimeMillis());
    }

    public MTRandom(long l) {
        super(l);
    }

    public MTRandom(byte[] byArray) {
        super(0L);
        this.setSeed(byArray);
    }

    public MTRandom(int[] nArray) {
        super(0L);
        this.setSeed(nArray);
    }

    private final void setSeed(int n) {
        if (this.mt == null) {
            this.mt = new int[624];
        }
        this.mt[0] = n;
        this.mti = 1;
        while (this.mti < 624) {
            this.mt[this.mti] = 1812433253 * (this.mt[this.mti - 1] ^ this.mt[this.mti - 1] >>> 30) + this.mti;
            ++this.mti;
        }
    }

    @Override
    public final synchronized void setSeed(long l) {
        if (this.compat) {
            this.setSeed((int)l);
        } else {
            if (this.ibuf == null) {
                this.ibuf = new int[2];
            }
            this.ibuf[0] = (int)l;
            this.ibuf[1] = (int)(l >>> 32);
            this.setSeed(this.ibuf);
        }
    }

    public final void setSeed(byte[] byArray) {
        this.setSeed(MTRandom.pack(byArray));
    }

    public final synchronized void setSeed(int[] nArray) {
        int n;
        int n2 = nArray.length;
        if (n2 == 0) {
            throw new IllegalArgumentException("Seed buffer may not be empty");
        }
        int n3 = 1;
        int n4 = 0;
        this.setSeed(19650218);
        for (n = 624 > n2 ? 624 : n2; n > 0; --n) {
            this.mt[n3] = (this.mt[n3] ^ (this.mt[n3 - 1] ^ this.mt[n3 - 1] >>> 30) * 1664525) + nArray[n4] + n4;
            ++n4;
            if (++n3 >= 624) {
                this.mt[0] = this.mt[623];
                n3 = 1;
            }
            if (n4 < n2) continue;
            n4 = 0;
        }
        for (n = 623; n > 0; --n) {
            this.mt[n3] = (this.mt[n3] ^ (this.mt[n3 - 1] ^ this.mt[n3 - 1] >>> 30) * 1566083941) - n3;
            if (++n3 < 624) continue;
            this.mt[0] = this.mt[623];
            n3 = 1;
        }
        this.mt[0] = Integer.MIN_VALUE;
    }

    @Override
    protected final synchronized int next(int n) {
        int n2;
        if (this.mti >= 624) {
            int n3;
            for (n3 = 0; n3 < 227; ++n3) {
                n2 = this.mt[n3] & Integer.MIN_VALUE | this.mt[n3 + 1] & Integer.MAX_VALUE;
                this.mt[n3] = this.mt[n3 + 397] ^ n2 >>> 1 ^ MAGIC[n2 & 1];
            }
            while (n3 < 623) {
                n2 = this.mt[n3] & Integer.MIN_VALUE | this.mt[n3 + 1] & Integer.MAX_VALUE;
                this.mt[n3] = this.mt[n3 + -227] ^ n2 >>> 1 ^ MAGIC[n2 & 1];
                ++n3;
            }
            n2 = this.mt[623] & Integer.MIN_VALUE | this.mt[0] & Integer.MAX_VALUE;
            this.mt[623] = this.mt[396] ^ n2 >>> 1 ^ MAGIC[n2 & 1];
            this.mti = 0;
        }
        n2 = this.mt[this.mti++];
        n2 ^= n2 >>> 11;
        n2 ^= n2 << 7 & 0x9D2C5680;
        n2 ^= n2 << 15 & 0xEFC60000;
        n2 ^= n2 >>> 18;
        return n2 >>> 32 - n;
    }

    public static int[] pack(byte[] byArray) {
        int n = byArray.length;
        int n2 = byArray.length + 3 >>> 2;
        int[] nArray = new int[n2];
        for (int i = 0; i < n2; ++i) {
            int n3 = i + 1 << 2;
            if (n3 > n) {
                n3 = n;
            }
            int n4 = byArray[--n3] & 0xFF;
            while ((n3 & 3) != 0) {
                n4 = n4 << 8 | byArray[--n3] & 0xFF;
            }
            nArray[i] = n4;
        }
        return nArray;
    }
}

