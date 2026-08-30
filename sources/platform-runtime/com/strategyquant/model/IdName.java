/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.model;

public class IdName {
    private Long id;
    private String name;

    public IdName() {
    }

    public IdName(Long l, String string) {
        this.id = l;
        this.name = string;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long l) {
        this.id = l;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public String toString() {
        return "IdName [id=" + this.id + ", name=" + this.name + "]";
    }
}

