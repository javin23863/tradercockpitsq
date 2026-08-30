/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

public class NonexistingCustomClassException
extends Exception {
    public NonexistingCustomClassException(String string) {
        super("Class with name '" + string + "' doesn't exist!");
    }
}

