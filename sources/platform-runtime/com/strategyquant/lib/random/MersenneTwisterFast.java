/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.random;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

public strictfp class MersenneTwisterFast
implements Serializable,
Cloneable {
    private static final long serialVersionUID = -8219700664442619525L;
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIX_A = -1727483681;
    private static final int UPPER_MASK = Integer.MIN_VALUE;
    private static final int LOWER_MASK = Integer.MAX_VALUE;
    private static final int TEMPERING_MASK_B = -1658038656;
    private static final int TEMPERING_MASK_C = -272236544;
    private int[] mt;
    private int mti;
    private int[] mag01;
    private double __nextNextGaussian;
    private boolean __haveNextNextGaussian;

    public Object clone() {
        try {
            MersenneTwisterFast mersenneTwisterFast = (MersenneTwisterFast)super.clone();
            mersenneTwisterFast.mt = (int[])this.mt.clone();
            mersenneTwisterFast.mag01 = (int[])this.mag01.clone();
            return mersenneTwisterFast;
        }
        catch (CloneNotSupportedException cloneNotSupportedException) {
            throw new InternalError();
        }
    }

    public boolean stateEquals(MersenneTwisterFast mersenneTwisterFast) {
        int n;
        if (mersenneTwisterFast == this) {
            return true;
        }
        if (mersenneTwisterFast == null) {
            return false;
        }
        if (this.mti != mersenneTwisterFast.mti) {
            return false;
        }
        for (n = 0; n < this.mag01.length; ++n) {
            if (this.mag01[n] == mersenneTwisterFast.mag01[n]) continue;
            return false;
        }
        for (n = 0; n < this.mt.length; ++n) {
            if (this.mt[n] == mersenneTwisterFast.mt[n]) continue;
            return false;
        }
        return true;
    }

    public void readState(DataInputStream dataInputStream) throws IOException {
        int n;
        int n2 = this.mt.length;
        for (n = 0; n < n2; ++n) {
            this.mt[n] = dataInputStream.readInt();
        }
        n2 = this.mag01.length;
        for (n = 0; n < n2; ++n) {
            this.mag01[n] = dataInputStream.readInt();
        }
        this.mti = dataInputStream.readInt();
        this.__nextNextGaussian = dataInputStream.readDouble();
        this.__haveNextNextGaussian = dataInputStream.readBoolean();
    }

    public void writeState(DataOutputStream dataOutputStream) throws IOException {
        int n;
        int n2 = this.mt.length;
        for (n = 0; n < n2; ++n) {
            dataOutputStream.writeInt(this.mt[n]);
        }
        n2 = this.mag01.length;
        for (n = 0; n < n2; ++n) {
            dataOutputStream.writeInt(this.mag01[n]);
        }
        dataOutputStream.writeInt(this.mti);
        dataOutputStream.writeDouble(this.__nextNextGaussian);
        dataOutputStream.writeBoolean(this.__haveNextNextGaussian);
    }

    public MersenneTwisterFast() {
        this(System.currentTimeMillis());
    }

    public MersenneTwisterFast(long l) {
        this.setSeed(l);
    }

    public MersenneTwisterFast(int[] nArray) {
        this.setSeed(nArray);
    }

    public void setSeed(long l) {
        this.__haveNextNextGaussian = false;
        this.mt = new int[624];
        this.mag01 = new int[2];
        this.mag01[0] = 0;
        this.mag01[1] = -1727483681;
        this.mt[0] = (int)(l & 0xFFFFFFFFFFFFFFFFL);
        this.mti = 1;
        while (this.mti < 624) {
            this.mt[this.mti] = 1812433253 * (this.mt[this.mti - 1] ^ this.mt[this.mti - 1] >>> 30) + this.mti;
            ++this.mti;
        }
    }

    public void setSeed(int[] nArray) {
        int n;
        if (nArray.length == 0) {
            throw new IllegalArgumentException("Array length must be greater than zero");
        }
        this.setSeed(19650218L);
        int n2 = 1;
        int n3 = 0;
        int n4 = n = 624 > nArray.length ? 624 : nArray.length;
        while (n != 0) {
            this.mt[n2] = (this.mt[n2] ^ (this.mt[n2 - 1] ^ this.mt[n2 - 1] >>> 30) * 1664525) + nArray[n3] + n3;
            ++n3;
            if (++n2 >= 624) {
                this.mt[0] = this.mt[623];
                n2 = 1;
            }
            if (n3 >= nArray.length) {
                n3 = 0;
            }
            --n;
        }
        for (n = 623; n != 0; --n) {
            this.mt[n2] = (this.mt[n2] ^ (this.mt[n2 - 1] ^ this.mt[n2 - 1] >>> 30) * 1566083941) - n2;
            if (++n2 < 624) continue;
            this.mt[0] = this.mt[623];
            n2 = 1;
        }
        this.mt[0] = Integer.MIN_VALUE;
    }

    public int nextInt() {
        int n;
        if (this.mti >= 624) {
            int n2;
            int[] nArray = this.mt;
            int[] nArray2 = this.mag01;
            for (n2 = 0; n2 < 227; ++n2) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
            }
            while (n2 < 623) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                ++n2;
            }
            n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
            nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        n ^= n >>> 18;
        return n;
    }

    public short nextShort() {
        int n;
        if (this.mti >= 624) {
            int n2;
            int[] nArray = this.mt;
            int[] nArray2 = this.mag01;
            for (n2 = 0; n2 < 227; ++n2) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
            }
            while (n2 < 623) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                ++n2;
            }
            n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
            nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        n ^= n >>> 18;
        return (short)(n >>> 16);
    }

    public char nextChar() {
        int n;
        if (this.mti >= 624) {
            int n2;
            int[] nArray = this.mt;
            int[] nArray2 = this.mag01;
            for (n2 = 0; n2 < 227; ++n2) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
            }
            while (n2 < 623) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                ++n2;
            }
            n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
            nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        n ^= n >>> 18;
        return (char)(n >>> 16);
    }

    public boolean nextBoolean() {
        int n;
        if (this.mti >= 624) {
            int n2;
            int[] nArray = this.mt;
            int[] nArray2 = this.mag01;
            for (n2 = 0; n2 < 227; ++n2) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
            }
            while (n2 < 623) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                ++n2;
            }
            n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
            nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        return (n ^= n >>> 18) >>> 31 != 0;
    }

    public boolean nextBoolean(float f) {
        int n;
        if (f < 0.0f || f > 1.0f) {
            throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
        }
        if (f == 0.0f) {
            return false;
        }
        if (f == 1.0f) {
            return true;
        }
        if (this.mti >= 624) {
            int n2;
            int[] nArray = this.mt;
            int[] nArray2 = this.mag01;
            for (n2 = 0; n2 < 227; ++n2) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
            }
            while (n2 < 623) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                ++n2;
            }
            n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
            nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        return (float)((n ^= n >>> 18) >>> 8) / 1.6777216E7f < f;
    }

    public boolean nextBoolean(double d) {
        int n;
        int n2;
        int n3;
        int[] nArray;
        int[] nArray2;
        if (d < 0.0 || d > 1.0) {
            throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
        }
        if (d == 0.0) {
            return false;
        }
        if (d == 1.0) {
            return true;
        }
        if (this.mti >= 624) {
            nArray2 = this.mt;
            nArray = this.mag01;
            for (n3 = 0; n3 < 227; ++n3) {
                n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + 397] ^ n2 >>> 1 ^ nArray[n2 & 1];
            }
            while (n3 < 623) {
                n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + -227] ^ n2 >>> 1 ^ nArray[n2 & 1];
                ++n3;
            }
            n2 = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
            nArray2[623] = nArray2[396] ^ n2 >>> 1 ^ nArray[n2 & 1];
            this.mti = 0;
        }
        n2 = this.mt[this.mti++];
        n2 ^= n2 >>> 11;
        n2 ^= n2 << 7 & 0x9D2C5680;
        n2 ^= n2 << 15 & 0xEFC60000;
        n2 ^= n2 >>> 18;
        if (this.mti >= 624) {
            nArray2 = this.mt;
            nArray = this.mag01;
            for (n3 = 0; n3 < 227; ++n3) {
                n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + 397] ^ n >>> 1 ^ nArray[n & 1];
            }
            while (n3 < 623) {
                n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + -227] ^ n >>> 1 ^ nArray[n & 1];
                ++n3;
            }
            n = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
            nArray2[623] = nArray2[396] ^ n >>> 1 ^ nArray[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        return (double)(((long)(n2 >>> 6) << 27) + (long)((n ^= n >>> 18) >>> 5)) / 9.007199254740992E15 < d;
    }

    public byte nextByte() {
        int n;
        if (this.mti >= 624) {
            int n2;
            int[] nArray = this.mt;
            int[] nArray2 = this.mag01;
            for (n2 = 0; n2 < 227; ++n2) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
            }
            while (n2 < 623) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                ++n2;
            }
            n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
            nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        n ^= n >>> 18;
        return (byte)(n >>> 24);
    }

    public void nextBytes(byte[] byArray) {
        for (int i = 0; i < byArray.length; ++i) {
            int n;
            if (this.mti >= 624) {
                int n2;
                int[] nArray = this.mt;
                int[] nArray2 = this.mag01;
                for (n2 = 0; n2 < 227; ++n2) {
                    n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                    nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
                }
                while (n2 < 623) {
                    n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                    nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                    ++n2;
                }
                n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
                nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
                this.mti = 0;
            }
            n = this.mt[this.mti++];
            n ^= n >>> 11;
            n ^= n << 7 & 0x9D2C5680;
            n ^= n << 15 & 0xEFC60000;
            n ^= n >>> 18;
            byArray[i] = (byte)(n >>> 24);
        }
    }

    public long nextLong() {
        int n;
        int n2;
        int n3;
        int[] nArray;
        int[] nArray2;
        if (this.mti >= 624) {
            nArray2 = this.mt;
            nArray = this.mag01;
            for (n3 = 0; n3 < 227; ++n3) {
                n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + 397] ^ n2 >>> 1 ^ nArray[n2 & 1];
            }
            while (n3 < 623) {
                n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + -227] ^ n2 >>> 1 ^ nArray[n2 & 1];
                ++n3;
            }
            n2 = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
            nArray2[623] = nArray2[396] ^ n2 >>> 1 ^ nArray[n2 & 1];
            this.mti = 0;
        }
        n2 = this.mt[this.mti++];
        n2 ^= n2 >>> 11;
        n2 ^= n2 << 7 & 0x9D2C5680;
        n2 ^= n2 << 15 & 0xEFC60000;
        n2 ^= n2 >>> 18;
        if (this.mti >= 624) {
            nArray2 = this.mt;
            nArray = this.mag01;
            for (n3 = 0; n3 < 227; ++n3) {
                n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + 397] ^ n >>> 1 ^ nArray[n & 1];
            }
            while (n3 < 623) {
                n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + -227] ^ n >>> 1 ^ nArray[n & 1];
                ++n3;
            }
            n = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
            nArray2[623] = nArray2[396] ^ n >>> 1 ^ nArray[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        n ^= n >>> 18;
        return ((long)n2 << 32) + (long)n;
    }

    public long nextLong(long l) {
        long l2;
        int n;
        int n2;
        long l3;
        if (l <= 0L) {
            throw new IllegalArgumentException("n must be positive, got: " + l);
        }
        do {
            int n3;
            int[] nArray;
            int[] nArray2;
            if (this.mti >= 624) {
                nArray2 = this.mt;
                nArray = this.mag01;
                for (n3 = 0; n3 < 227; ++n3) {
                    n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                    nArray2[n3] = nArray2[n3 + 397] ^ n2 >>> 1 ^ nArray[n2 & 1];
                }
                while (n3 < 623) {
                    n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                    nArray2[n3] = nArray2[n3 + -227] ^ n2 >>> 1 ^ nArray[n2 & 1];
                    ++n3;
                }
                n2 = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
                nArray2[623] = nArray2[396] ^ n2 >>> 1 ^ nArray[n2 & 1];
                this.mti = 0;
            }
            n2 = this.mt[this.mti++];
            n2 ^= n2 >>> 11;
            n2 ^= n2 << 7 & 0x9D2C5680;
            n2 ^= n2 << 15 & 0xEFC60000;
            n2 ^= n2 >>> 18;
            if (this.mti >= 624) {
                nArray2 = this.mt;
                nArray = this.mag01;
                for (n3 = 0; n3 < 227; ++n3) {
                    n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                    nArray2[n3] = nArray2[n3 + 397] ^ n >>> 1 ^ nArray[n & 1];
                }
                while (n3 < 623) {
                    n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                    nArray2[n3] = nArray2[n3 + -227] ^ n >>> 1 ^ nArray[n & 1];
                    ++n3;
                }
                n = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
                nArray2[623] = nArray2[396] ^ n >>> 1 ^ nArray[n & 1];
                this.mti = 0;
            }
            n = this.mt[this.mti++];
            n ^= n >>> 11;
            n ^= n << 7 & 0x9D2C5680;
            n ^= n << 15 & 0xEFC60000;
        } while ((l3 = ((long)n2 << 32) + (long)(n ^= n >>> 18) >>> 1) - (l2 = l3 % l) + (l - 1L) < 0L);
        return l2;
    }

    public double nextDouble() {
        int n;
        int n2;
        int n3;
        int[] nArray;
        int[] nArray2;
        if (this.mti >= 624) {
            nArray2 = this.mt;
            nArray = this.mag01;
            for (n3 = 0; n3 < 227; ++n3) {
                n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + 397] ^ n2 >>> 1 ^ nArray[n2 & 1];
            }
            while (n3 < 623) {
                n2 = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + -227] ^ n2 >>> 1 ^ nArray[n2 & 1];
                ++n3;
            }
            n2 = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
            nArray2[623] = nArray2[396] ^ n2 >>> 1 ^ nArray[n2 & 1];
            this.mti = 0;
        }
        n2 = this.mt[this.mti++];
        n2 ^= n2 >>> 11;
        n2 ^= n2 << 7 & 0x9D2C5680;
        n2 ^= n2 << 15 & 0xEFC60000;
        n2 ^= n2 >>> 18;
        if (this.mti >= 624) {
            nArray2 = this.mt;
            nArray = this.mag01;
            for (n3 = 0; n3 < 227; ++n3) {
                n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + 397] ^ n >>> 1 ^ nArray[n & 1];
            }
            while (n3 < 623) {
                n = nArray2[n3] & Integer.MIN_VALUE | nArray2[n3 + 1] & Integer.MAX_VALUE;
                nArray2[n3] = nArray2[n3 + -227] ^ n >>> 1 ^ nArray[n & 1];
                ++n3;
            }
            n = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
            nArray2[623] = nArray2[396] ^ n >>> 1 ^ nArray[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        n ^= n >>> 18;
        return (double)(((long)(n2 >>> 6) << 27) + (long)(n >>> 5)) / 9.007199254740992E15;
    }

    public double nextDouble(boolean bl, boolean bl2) {
        double d = 0.0;
        do {
            d = this.nextDouble();
            if (!bl2 || !this.nextBoolean()) continue;
            d += 1.0;
        } while (d > 1.0 || !bl && d == 0.0);
        return d;
    }

    public void clearGaussian() {
        this.__haveNextNextGaussian = false;
    }

    public double nextGaussian() {
        int n;
        int n2;
        double d;
        int n3;
        int n4;
        double d2;
        double d3;
        if (this.__haveNextNextGaussian) {
            this.__haveNextNextGaussian = false;
            return this.__nextNextGaussian;
        }
        do {
            int n5;
            int[] nArray;
            int[] nArray2;
            if (this.mti >= 624) {
                nArray2 = this.mt;
                nArray = this.mag01;
                for (n5 = 0; n5 < 227; ++n5) {
                    n4 = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + 397] ^ n4 >>> 1 ^ nArray[n4 & 1];
                }
                while (n5 < 623) {
                    n4 = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + -227] ^ n4 >>> 1 ^ nArray[n4 & 1];
                    ++n5;
                }
                n4 = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
                nArray2[623] = nArray2[396] ^ n4 >>> 1 ^ nArray[n4 & 1];
                this.mti = 0;
            }
            n4 = this.mt[this.mti++];
            n4 ^= n4 >>> 11;
            n4 ^= n4 << 7 & 0x9D2C5680;
            n4 ^= n4 << 15 & 0xEFC60000;
            n4 ^= n4 >>> 18;
            if (this.mti >= 624) {
                nArray2 = this.mt;
                nArray = this.mag01;
                for (n5 = 0; n5 < 227; ++n5) {
                    n3 = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + 397] ^ n3 >>> 1 ^ nArray[n3 & 1];
                }
                while (n5 < 623) {
                    n3 = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + -227] ^ n3 >>> 1 ^ nArray[n3 & 1];
                    ++n5;
                }
                n3 = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
                nArray2[623] = nArray2[396] ^ n3 >>> 1 ^ nArray[n3 & 1];
                this.mti = 0;
            }
            n3 = this.mt[this.mti++];
            n3 ^= n3 >>> 11;
            n3 ^= n3 << 7 & 0x9D2C5680;
            n3 ^= n3 << 15 & 0xEFC60000;
            n3 ^= n3 >>> 18;
            if (this.mti >= 624) {
                nArray2 = this.mt;
                nArray = this.mag01;
                for (n5 = 0; n5 < 227; ++n5) {
                    n2 = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + 397] ^ n2 >>> 1 ^ nArray[n2 & 1];
                }
                while (n5 < 623) {
                    n2 = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + -227] ^ n2 >>> 1 ^ nArray[n2 & 1];
                    ++n5;
                }
                n2 = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
                nArray2[623] = nArray2[396] ^ n2 >>> 1 ^ nArray[n2 & 1];
                this.mti = 0;
            }
            n2 = this.mt[this.mti++];
            n2 ^= n2 >>> 11;
            n2 ^= n2 << 7 & 0x9D2C5680;
            n2 ^= n2 << 15 & 0xEFC60000;
            n2 ^= n2 >>> 18;
            if (this.mti >= 624) {
                nArray2 = this.mt;
                nArray = this.mag01;
                for (n5 = 0; n5 < 227; ++n5) {
                    n = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + 397] ^ n >>> 1 ^ nArray[n & 1];
                }
                while (n5 < 623) {
                    n = nArray2[n5] & Integer.MIN_VALUE | nArray2[n5 + 1] & Integer.MAX_VALUE;
                    nArray2[n5] = nArray2[n5 + -227] ^ n >>> 1 ^ nArray[n & 1];
                    ++n5;
                }
                n = nArray2[623] & Integer.MIN_VALUE | nArray2[0] & Integer.MAX_VALUE;
                nArray2[623] = nArray2[396] ^ n >>> 1 ^ nArray[n & 1];
                this.mti = 0;
            }
            n = this.mt[this.mti++];
            n ^= n >>> 11;
            n ^= n << 7 & 0x9D2C5680;
            n ^= n << 15 & 0xEFC60000;
        } while ((d3 = (d2 = 2.0 * ((double)(((long)(n4 >>> 6) << 27) + (long)(n3 >>> 5)) / 9.007199254740992E15) - 1.0) * d2 + (d = 2.0 * ((double)(((long)(n2 >>> 6) << 27) + (long)((n ^= n >>> 18) >>> 5)) / 9.007199254740992E15) - 1.0) * d) >= 1.0 || d3 == 0.0);
        double d4 = StrictMath.sqrt(-2.0 * StrictMath.log(d3) / d3);
        this.__nextNextGaussian = d * d4;
        this.__haveNextNextGaussian = true;
        return d2 * d4;
    }

    public float nextFloat() {
        int n;
        if (this.mti >= 624) {
            int n2;
            int[] nArray = this.mt;
            int[] nArray2 = this.mag01;
            for (n2 = 0; n2 < 227; ++n2) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + 397] ^ n >>> 1 ^ nArray2[n & 1];
            }
            while (n2 < 623) {
                n = nArray[n2] & Integer.MIN_VALUE | nArray[n2 + 1] & Integer.MAX_VALUE;
                nArray[n2] = nArray[n2 + -227] ^ n >>> 1 ^ nArray2[n & 1];
                ++n2;
            }
            n = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
            nArray[623] = nArray[396] ^ n >>> 1 ^ nArray2[n & 1];
            this.mti = 0;
        }
        n = this.mt[this.mti++];
        n ^= n >>> 11;
        n ^= n << 7 & 0x9D2C5680;
        n ^= n << 15 & 0xEFC60000;
        n ^= n >>> 18;
        return (float)(n >>> 8) / 1.6777216E7f;
    }

    public float nextFloat(boolean bl, boolean bl2) {
        float f = 0.0f;
        do {
            f = this.nextFloat();
            if (!bl2 || !this.nextBoolean()) continue;
            f += 1.0f;
        } while (f > 1.0f || !bl && f == 0.0f);
        return f;
    }

    public int nextInt(int n) {
        int n2;
        int n3;
        int n4;
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive, got: " + n);
        }
        if ((n & -n) == n) {
            int n5;
            if (this.mti >= 624) {
                int n6;
                int[] nArray = this.mt;
                int[] nArray2 = this.mag01;
                for (n6 = 0; n6 < 227; ++n6) {
                    n5 = nArray[n6] & Integer.MIN_VALUE | nArray[n6 + 1] & Integer.MAX_VALUE;
                    nArray[n6] = nArray[n6 + 397] ^ n5 >>> 1 ^ nArray2[n5 & 1];
                }
                while (n6 < 623) {
                    n5 = nArray[n6] & Integer.MIN_VALUE | nArray[n6 + 1] & Integer.MAX_VALUE;
                    nArray[n6] = nArray[n6 + -227] ^ n5 >>> 1 ^ nArray2[n5 & 1];
                    ++n6;
                }
                n5 = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
                nArray[623] = nArray[396] ^ n5 >>> 1 ^ nArray2[n5 & 1];
                this.mti = 0;
            }
            n5 = this.mt[this.mti++];
            n5 ^= n5 >>> 11;
            n5 ^= n5 << 7 & 0x9D2C5680;
            n5 ^= n5 << 15 & 0xEFC60000;
            n5 ^= n5 >>> 18;
            return (int)((long)n * (long)(n5 >>> 1) >> 31);
        }
        do {
            if (this.mti >= 624) {
                int n7;
                int[] nArray = this.mt;
                int[] nArray3 = this.mag01;
                for (n7 = 0; n7 < 227; ++n7) {
                    n3 = nArray[n7] & Integer.MIN_VALUE | nArray[n7 + 1] & Integer.MAX_VALUE;
                    nArray[n7] = nArray[n7 + 397] ^ n3 >>> 1 ^ nArray3[n3 & 1];
                }
                while (n7 < 623) {
                    n3 = nArray[n7] & Integer.MIN_VALUE | nArray[n7 + 1] & Integer.MAX_VALUE;
                    nArray[n7] = nArray[n7 + -227] ^ n3 >>> 1 ^ nArray3[n3 & 1];
                    ++n7;
                }
                n3 = nArray[623] & Integer.MIN_VALUE | nArray[0] & Integer.MAX_VALUE;
                nArray[623] = nArray[396] ^ n3 >>> 1 ^ nArray3[n3 & 1];
                this.mti = 0;
            }
            n3 = this.mt[this.mti++];
            n3 ^= n3 >>> 11;
            n3 ^= n3 << 7 & 0x9D2C5680;
            n3 ^= n3 << 15 & 0xEFC60000;
        } while ((n4 = (n3 ^= n3 >>> 18) >>> 1) - (n2 = n4 % n) + (n - 1) < 0);
        return n2;
    }

    public static void main(String[] stringArray) {
        int n;
        MersenneTwisterFast mersenneTwisterFast = new MersenneTwisterFast(new int[]{291, 564, 837, 1110});
        for (n = 0; n < 1000; ++n) {
            long l = mersenneTwisterFast.nextInt();
            if (l < 0L) {
                l += 0x100000000L;
            }
            String string = String.valueOf(l);
            while (string.length() < 10) {
                string = " " + string;
            }
            System.out.print(string + " ");
            if (n % 5 != 4) continue;
            System.out.println();
        }
        System.out.println("\nTime to test grabbing 100000000 ints");
        Random random = new Random(4357L);
        int n2 = 0;
        long l = System.currentTimeMillis();
        for (n = 0; n < 100000000; ++n) {
            n2 += random.nextInt();
        }
        System.out.println("java.util.Random: " + (System.currentTimeMillis() - l) + "          Ignore this: " + n2);
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        l = System.currentTimeMillis();
        n2 = 0;
        for (n = 0; n < 100000000; ++n) {
            n2 += mersenneTwisterFast.nextInt();
        }
        System.out.println("Mersenne Twister Fast: " + (System.currentTimeMillis() - l) + "          Ignore this: " + n2);
        System.out.println("\nGrab the first 1000 booleans");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextBoolean() + " ");
            if (n % 8 != 7) continue;
            System.out.println();
        }
        if (n % 8 != 7) {
            System.out.println();
        }
        System.out.println("\nGrab 1000 booleans of increasing probability using nextBoolean(double)");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextBoolean((double)n / 999.0) + " ");
            if (n % 8 != 7) continue;
            System.out.println();
        }
        if (n % 8 != 7) {
            System.out.println();
        }
        System.out.println("\nGrab 1000 booleans of increasing probability using nextBoolean(float)");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextBoolean((float)n / 999.0f) + " ");
            if (n % 8 != 7) continue;
            System.out.println();
        }
        if (n % 8 != 7) {
            System.out.println();
        }
        byte[] byArray = new byte[1000];
        System.out.println("\nGrab the first 1000 bytes using nextBytes");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        mersenneTwisterFast.nextBytes(byArray);
        for (n = 0; n < 1000; ++n) {
            System.out.print(byArray[n] + " ");
            if (n % 16 != 15) continue;
            System.out.println();
        }
        if (n % 16 != 15) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 bytes -- must be same as nextBytes");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            byte by = mersenneTwisterFast.nextByte();
            System.out.print(by + " ");
            if (by != byArray[n]) {
                System.out.print("BAD ");
            }
            if (n % 16 != 15) continue;
            System.out.println();
        }
        if (n % 16 != 15) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 shorts");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextShort() + " ");
            if (n % 8 != 7) continue;
            System.out.println();
        }
        if (n % 8 != 7) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 ints");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextInt() + " ");
            if (n % 4 != 3) continue;
            System.out.println();
        }
        if (n % 4 != 3) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 ints of different sizes");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        int n3 = 1;
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextInt(n3) + " ");
            if ((n3 *= 2) <= 0) {
                n3 = 1;
            }
            if (n % 4 != 3) continue;
            System.out.println();
        }
        if (n % 4 != 3) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 longs");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextLong() + " ");
            if (n % 3 != 2) continue;
            System.out.println();
        }
        if (n % 3 != 2) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 longs of different sizes");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        long l2 = 1L;
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextLong(l2) + " ");
            if ((l2 *= 2L) <= 0L) {
                l2 = 1L;
            }
            if (n % 4 != 3) continue;
            System.out.println();
        }
        if (n % 4 != 3) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 floats");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextFloat() + " ");
            if (n % 4 != 3) continue;
            System.out.println();
        }
        if (n % 4 != 3) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 doubles");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextDouble() + " ");
            if (n % 3 != 2) continue;
            System.out.println();
        }
        if (n % 3 != 2) {
            System.out.println();
        }
        System.out.println("\nGrab the first 1000 gaussian doubles");
        mersenneTwisterFast = new MersenneTwisterFast(4357L);
        for (n = 0; n < 1000; ++n) {
            System.out.print(mersenneTwisterFast.nextGaussian() + " ");
            if (n % 3 != 2) continue;
            System.out.println();
        }
        if (n % 3 != 2) {
            System.out.println();
        }
    }
}

