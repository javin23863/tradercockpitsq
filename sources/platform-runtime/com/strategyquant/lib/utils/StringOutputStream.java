/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class StringOutputStream
extends OutputStream {
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private static final Charset DEFAULT_CHARACTER_SET = Charset.forName("UTF-8");
    private Charset characterSet;

    @Override
    public void write(int n) throws IOException {
        this.buffer.write(n);
    }

    @Override
    public void write(byte[] byArray) throws IOException {
        this.buffer.write(byArray);
    }

    @Override
    public void write(byte[] byArray, int n, int n2) throws IOException {
        this.buffer.write(byArray, n, n2);
    }

    public Charset getCharacterSet() {
        if (this.characterSet == null) {
            return DEFAULT_CHARACTER_SET;
        }
        return this.characterSet;
    }

    public void setCharacterSet(Charset charset) {
        this.characterSet = charset;
    }

    public String toString() {
        return new String(this.buffer.toByteArray(), this.getCharacterSet());
    }
}

