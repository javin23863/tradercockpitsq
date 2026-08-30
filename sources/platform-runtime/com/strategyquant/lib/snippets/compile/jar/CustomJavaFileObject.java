/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.jar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

class CustomJavaFileObject
implements JavaFileObject {
    private final String binaryName;
    private byte[] bytes;
    private final String name;

    public CustomJavaFileObject(String string, byte[] byArray) {
        this.binaryName = string;
        this.name = "crypted:" + string;
        this.bytes = byArray;
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Reader openReader(boolean bl) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getCharContent(boolean bl) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer openWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        return 0L;
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JavaFileObject.Kind getKind() {
        return JavaFileObject.Kind.CLASS;
    }

    @Override
    public boolean isNameCompatible(String string, JavaFileObject.Kind kind) {
        String string2 = string + kind.extension;
        return kind.equals((Object)this.getKind()) && (string2.equals(this.getName()) || this.getName().endsWith("/" + string2));
    }

    @Override
    public NestingKind getNestingKind() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Modifier getAccessLevel() {
        throw new UnsupportedOperationException();
    }

    public String binaryName() {
        return this.binaryName;
    }
}

