/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jdom2.Element;

public class SettingsMap
extends ValuesMap {
    public void setIfNotExists(String string, Object object) {
        if (!this.containsKey(string)) {
            this.set(string, object);
        }
    }

    @Override
    public Element getXML() {
        Element element = super.getXML();
        element.setName("SettingsMap");
        return element;
    }

    @Override
    public synchronized SettingsMap clone() {
        SettingsMap settingsMap = new SettingsMap();
        settingsMap.values = SQUtils.cloneValuesMap((Int2ObjectOpenHashMap<Object>)this.values);
        return settingsMap;
    }

    public static double getDouble(Object object, double d) {
        if (object == null) {
            return d;
        }
        return (Double)object;
    }

    public static int getInt(Object object, int n) {
        if (object == null) {
            return n;
        }
        return (Integer)object;
    }

    public static boolean getBool(Object object, boolean bl) {
        if (object == null) {
            return bl;
        }
        return (Boolean)object;
    }

    public static long getLong(Object object, long l) {
        if (object == null) {
            return l;
        }
        return (Long)object;
    }
}

