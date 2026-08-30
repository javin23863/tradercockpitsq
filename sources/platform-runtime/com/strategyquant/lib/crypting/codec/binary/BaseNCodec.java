/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.crypting.codec.binary;

import com.strategyquant.lib.crypting.codec.binary.StringUtils;
import java.util.Arrays;
import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;

public abstract class BaseNCodec
implements BinaryEncoder,
BinaryDecoder {
    static final int EOF = -1;
    public static final int MIME_CHUNK_SIZE = 76;
    public static final int PEM_CHUNK_SIZE = 64;
    private static final int DEFAULT_BUFFER_RESIZE_FACTOR = 2;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    protected static final int MASK_8BITS = 255;
    protected static final byte PAD_DEFAULT = 61;
    protected final byte PAD = (byte)61;
    private final int unencodedBlockSize;
    private final int encodedBlockSize;
    protected final int lineLength;
    private final int chunkSeparatorLength;

    protected BaseNCodec(int n, int n2, int n3, int n4) {
        this.unencodedBlockSize = n;
        this.encodedBlockSize = n2;
        boolean bl = n3 > 0 && n4 > 0;
        this.lineLength = bl ? n3 / n2 * n2 : 0;
        this.chunkSeparatorLength = n4;
    }

    boolean hasData(Context context) {
        return context.buffer != null;
    }

    int available(Context context) {
        return context.buffer != null ? context.pos - context.readPos : 0;
    }

    protected int getDefaultBufferSize() {
        return 8192;
    }

    private byte[] resizeBuffer(Context context) {
        if (context.buffer == null) {
            context.buffer = new byte[this.getDefaultBufferSize()];
            context.pos = 0;
            context.readPos = 0;
        } else {
            byte[] byArray = new byte[context.buffer.length * 2];
            System.arraycopy(context.buffer, 0, byArray, 0, context.buffer.length);
            context.buffer = byArray;
        }
        return context.buffer;
    }

    protected byte[] ensureBufferSize(int n, Context context) {
        if (context.buffer == null || context.buffer.length < context.pos + n) {
            return this.resizeBuffer(context);
        }
        return context.buffer;
    }

    int readResults(byte[] byArray, int n, int n2, Context context) {
        if (context.buffer != null) {
            int n3 = Math.min(this.available(context), n2);
            System.arraycopy(context.buffer, context.readPos, byArray, n, n3);
            context.readPos += n3;
            if (context.readPos >= context.pos) {
                context.buffer = null;
            }
            return n3;
        }
        return context.eof ? -1 : 0;
    }

    protected static boolean isWhiteSpace(byte by) {
        switch (by) {
            case 9: 
            case 10: 
            case 13: 
            case 32: {
                return true;
            }
        }
        return false;
    }

    public Object encode(Object object) throws EncoderException {
        if (!(object instanceof byte[])) {
            throw new EncoderException("Parameter supplied to Base-N encode is not a byte[]");
        }
        return this.encode((byte[])object);
    }

    public String encodeToString(byte[] byArray) {
        return StringUtils.newStringUtf8(this.encode(byArray));
    }

    public String encodeAsString(byte[] byArray) {
        return StringUtils.newStringUtf8(this.encode(byArray));
    }

    public Object decode(Object object) throws DecoderException {
        if (object instanceof byte[]) {
            return this.decode((byte[])object);
        }
        if (object instanceof String) {
            return this.decode((String)object);
        }
        throw new DecoderException("Parameter supplied to Base-N decode is not a byte[] or a String");
    }

    public byte[] decode(String string) {
        return this.decode(StringUtils.getBytesUtf8(string));
    }

    public byte[] decode(byte[] byArray) {
        if (byArray == null || byArray.length == 0) {
            return byArray;
        }
        Context context = new Context();
        this.decode(byArray, 0, byArray.length, context);
        this.decode(byArray, 0, -1, context);
        byte[] byArray2 = new byte[context.pos];
        this.readResults(byArray2, 0, byArray2.length, context);
        return byArray2;
    }

    public byte[] encode(byte[] byArray) {
        if (byArray == null || byArray.length == 0) {
            return byArray;
        }
        Context context = new Context();
        this.encode(byArray, 0, byArray.length, context);
        this.encode(byArray, 0, -1, context);
        byte[] byArray2 = new byte[context.pos - context.readPos];
        this.readResults(byArray2, 0, byArray2.length, context);
        return byArray2;
    }

    abstract void encode(byte[] var1, int var2, int var3, Context var4);

    abstract void decode(byte[] var1, int var2, int var3, Context var4);

    protected abstract boolean isInAlphabet(byte var1);

    public boolean isInAlphabet(byte[] byArray, boolean bl) {
        for (int i = 0; i < byArray.length; ++i) {
            if (this.isInAlphabet(byArray[i]) || bl && (byArray[i] == 61 || BaseNCodec.isWhiteSpace(byArray[i]))) continue;
            return false;
        }
        return true;
    }

    public boolean isInAlphabet(String string) {
        return this.isInAlphabet(StringUtils.getBytesUtf8(string), true);
    }

    protected boolean containsAlphabetOrPad(byte[] byArray) {
        if (byArray == null) {
            return false;
        }
        for (byte by : byArray) {
            if (61 != by && !this.isInAlphabet(by)) continue;
            return true;
        }
        return false;
    }

    public long getEncodedLength(byte[] byArray) {
        long l = (long)((byArray.length + this.unencodedBlockSize - 1) / this.unencodedBlockSize) * (long)this.encodedBlockSize;
        if (this.lineLength > 0) {
            l += (l + (long)this.lineLength - 1L) / (long)this.lineLength * (long)this.chunkSeparatorLength;
        }
        return l;
    }

    static class Context {
        int ibitWorkArea;
        long lbitWorkArea;
        byte[] buffer;
        int pos;
        int readPos;
        boolean eof;
        int currentLinePos;
        int modulus;

        Context() {
        }

        public String toString() {
            return String.format("%s[buffer=%s, currentLinePos=%s, eof=%s, ibitWorkArea=%s, lbitWorkArea=%s, modulus=%s, pos=%s, readPos=%s]", this.getClass().getSimpleName(), Arrays.toString(this.buffer), this.currentLinePos, this.eof, this.ibitWorkArea, this.lbitWorkArea, this.modulus, this.pos, this.readPos);
        }
    }
}

