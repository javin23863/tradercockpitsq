/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.jar;

public class SecuredClassLoder
extends ClassLoader {
    public SecuredClassLoder() {
        super(SecuredClassLoder.class.getClassLoader());
    }
}

