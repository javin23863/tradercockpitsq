/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sqxbusiness;

public class EAOption {
    public String name;
    public String type;
    public String value;
    public boolean enabled;

    public EAOption(String string, String string2, String string3, boolean bl) {
        this.name = string;
        this.type = string2;
        this.value = string3;
        this.enabled = bl;
    }

    public EAOption clone() {
        return new EAOption(this.name, this.type, this.value, this.enabled);
    }
}

