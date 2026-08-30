/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.lang.reflect.Array;
import java.util.Collection;

public class JsonCreator {
    private static final String QUOTES = "\"";
    private static final String EQUALS = ":";
    private static final String OBJECT_BEGIN = "{";
    private static final String OBJECT_END = "}";
    private static final String SEPARATOR = ",";
    private static final String ARRAY_BEGIN = "[";
    private static final String ARRAY_END = "]";
    private static final String NULL = "null";
    private StringBuilder sb = new StringBuilder();

    public String toJson() {
        return this.sb.toString();
    }

    public String toString() {
        return this.toJson();
    }

    public void beginObject() {
        this.sb.append(OBJECT_BEGIN);
    }

    public void endObject(boolean bl) {
        this.sb.append(OBJECT_END);
        if (bl) {
            this.separator();
        }
    }

    public void separator() {
        this.sb.append(SEPARATOR);
    }

    public void put(String string, Object object, boolean bl) {
        this.put(string);
        this.setValue(object, bl);
    }

    public void putRaw(String string, Object object, boolean bl) {
        this.put(string);
        this.sb.append(object);
        if (bl) {
            this.separator();
        }
    }

    public void put(String string) {
        this.sb.append(QUOTES);
        this.sb.append(string);
        this.sb.append(QUOTES);
        this.sb.append(EQUALS);
    }

    public void putBeginObject(String string) {
        this.put(string);
        this.beginObject();
    }

    public void putBeginArray(String string) {
        this.put(string);
        this.beginArray();
    }

    public void setValue(Object object, boolean bl) {
        boolean bl2 = false;
        if (object == null) {
            this.sb.append(NULL);
        } else if (object instanceof String) {
            this.sb.append(QUOTES);
            this.sb.append(object);
            this.sb.append(QUOTES);
        } else if (object instanceof Collection) {
            this.add((Collection)object, bl);
            bl2 = true;
        } else if (object.getClass().isArray()) {
            this.addArray(object, bl);
            bl2 = true;
        } else {
            this.sb.append(object);
        }
        if (bl && !bl2) {
            this.separator();
        }
    }

    public void beginArray() {
        this.sb.append(ARRAY_BEGIN);
    }

    public void endArray(boolean bl) {
        this.sb.append(ARRAY_END);
        if (bl) {
            this.separator();
        }
    }

    public void addArray(Object object, boolean bl) {
        this.beginArray();
        int n = Array.getLength(object);
        for (int i = 0; i < n; ++i) {
            Object object2 = Array.get(object, i);
            this.setValue(object2, i != n - 1);
        }
        this.endArray(bl);
    }

    public void add(Collection collection, boolean bl) {
        this.beginArray();
        int n = 0;
        for (Object e : collection) {
            this.setValue(e, n != collection.size() - 1);
            ++n;
        }
        this.endArray(bl);
    }
}

