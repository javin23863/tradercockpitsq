/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.L88OaFjjon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CwkMCS2pCb {
    public static final byte BYTE_MISSING = -99;
    public static final int INT_MISSING = -999999999;
    public static final long LONG_MISSING = -999999999L;
    public static final double DOUBLE_MISSING = -9.99999999E8;
    public static final float FLOAT_MISSING = -1.0E9f;
    private HashMap<String, Object> variables = null;
    private ArrayList<String> reservedVariables = null;

    public HashMap<String, Object> getAll() {
        if (this.variables == null) {
            this.variables = new HashMap();
        }
        return this.variables;
    }

    public void setAll(Map<String, Object> map) {
        if (this.variables == null) {
            this.variables = new HashMap();
        }
        this.variables.putAll(map);
    }

    public boolean contains(String string) {
        if (this.variables == null) {
            return false;
        }
        return this.variables.containsKey(this.computeKey(string));
    }

    public void set(String string, Object object) {
        if (this.variables == null) {
            this.variables = new HashMap();
        }
        this.variables.put(this.computeKey(string), object);
    }

    public Object get(String string) {
        if (this.variables == null) {
            return null;
        }
        return this.variables.get(this.computeKey(string));
    }

    public void remove(String string) {
        this.variables.remove(this.computeKey(string));
    }

    public void clearAll() {
        this.variables.clear();
    }

    public String getString(String string) {
        if (this.variables == null) {
            return null;
        }
        return (String)this.variables.get(this.computeKey(string));
    }

    public byte getByte(String string) {
        if (this.variables == null) {
            return -99;
        }
        String string2 = this.computeKey(string);
        try {
            return (Byte)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Short) {
                return ((Short)object).byteValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).byteValue();
            }
            if (object instanceof Long) {
                return ((Long)object).byteValue();
            }
            if (object instanceof Float) {
                return ((Float)object).byteValue();
            }
            if (object instanceof Double) {
                return ((Double)object).byteValue();
            }
            return -99;
        }
    }

    public byte getByte(String string, byte by) {
        if (this.variables == null) {
            return by;
        }
        String string2 = this.computeKey(string);
        if (!this.variables.containsKey(string2)) {
            return by;
        }
        try {
            return (Byte)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Short) {
                return ((Short)object).byteValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).byteValue();
            }
            if (object instanceof Long) {
                return ((Long)object).byteValue();
            }
            if (object instanceof Float) {
                return ((Float)object).byteValue();
            }
            if (object instanceof Double) {
                return ((Double)object).byteValue();
            }
            return -99;
        }
    }

    public int getInt(String string) {
        if (this.variables == null) {
            return -999999999;
        }
        String string2 = this.computeKey(string);
        try {
            return (Integer)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).intValue();
            }
            if (object instanceof Short) {
                return ((Short)object).intValue();
            }
            if (object instanceof Long) {
                return ((Long)object).intValue();
            }
            if (object instanceof Float) {
                return ((Float)object).intValue();
            }
            if (object instanceof Double) {
                return ((Double)object).intValue();
            }
            return -999999999;
        }
    }

    public int getInt(String string, int n) {
        if (this.variables == null) {
            return n;
        }
        String string2 = this.computeKey(string);
        if (!this.variables.containsKey(string2)) {
            return n;
        }
        try {
            return (Integer)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).intValue();
            }
            if (object instanceof Short) {
                return ((Short)object).intValue();
            }
            if (object instanceof Long) {
                return ((Long)object).intValue();
            }
            if (object instanceof Float) {
                return ((Float)object).intValue();
            }
            if (object instanceof Double) {
                return ((Double)object).intValue();
            }
            return -999999999;
        }
    }

    public long getLong(String string) {
        if (this.variables == null) {
            return -999999999L;
        }
        String string2 = this.computeKey(string);
        try {
            return (Long)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).longValue();
            }
            if (object instanceof Short) {
                return ((Short)object).longValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).longValue();
            }
            if (object instanceof Float) {
                return ((Float)object).longValue();
            }
            if (object instanceof Double) {
                return ((Double)object).longValue();
            }
            return -999999999L;
        }
    }

    public long getLong(String string, long l) {
        if (this.variables == null) {
            return l;
        }
        String string2 = this.computeKey(string);
        if (!this.variables.containsKey(string2)) {
            return l;
        }
        try {
            return (Long)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).longValue();
            }
            if (object instanceof Short) {
                return ((Short)object).longValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).longValue();
            }
            if (object instanceof Float) {
                return ((Float)object).longValue();
            }
            if (object instanceof Double) {
                return ((Double)object).longValue();
            }
            return -999999999L;
        }
    }

    public double getDouble(String string) {
        if (this.variables == null) {
            return -9.99999999E8;
        }
        String string2 = this.computeKey(string);
        try {
            return (Double)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).doubleValue();
            }
            if (object instanceof Short) {
                return ((Short)object).doubleValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).doubleValue();
            }
            if (object instanceof Long) {
                return ((Long)object).doubleValue();
            }
            if (object instanceof Float) {
                return ((Float)object).doubleValue();
            }
            return -9.99999999E8;
        }
    }

    public double getDouble(String string, double d) {
        if (this.variables == null) {
            return d;
        }
        String string2 = this.computeKey(string);
        if (!this.variables.containsKey(string2)) {
            return d;
        }
        try {
            return (Double)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).doubleValue();
            }
            if (object instanceof Short) {
                return ((Short)object).doubleValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).doubleValue();
            }
            if (object instanceof Long) {
                return ((Long)object).doubleValue();
            }
            if (object instanceof Float) {
                return ((Float)object).doubleValue();
            }
            return -9.99999999E8;
        }
    }

    public double getFloat(String string) {
        if (this.variables == null) {
            return -1.0E9;
        }
        String string2 = this.computeKey(string);
        try {
            return (Double)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).floatValue();
            }
            if (object instanceof Short) {
                return ((Short)object).floatValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).floatValue();
            }
            if (object instanceof Long) {
                return ((Long)object).floatValue();
            }
            if (object instanceof Double) {
                return ((Double)object).floatValue();
            }
            return -1.0E9;
        }
    }

    public double getFloat(String string, double d) {
        if (this.variables == null) {
            return d;
        }
        String string2 = this.computeKey(string);
        if (!this.variables.containsKey(string2)) {
            return d;
        }
        try {
            return (Double)this.variables.get(string2);
        }
        catch (Exception exception) {
            Object object = this.variables.get(string2);
            if (object instanceof Byte) {
                return ((Byte)object).floatValue();
            }
            if (object instanceof Short) {
                return ((Short)object).floatValue();
            }
            if (object instanceof Integer) {
                return ((Integer)object).floatValue();
            }
            if (object instanceof Long) {
                return ((Long)object).floatValue();
            }
            if (object instanceof Double) {
                return ((Double)object).floatValue();
            }
            return -1.0E9;
        }
    }

    public boolean getBoolean(String string, boolean bl) {
        if (this.variables == null) {
            return false;
        }
        String string2 = this.computeKey(string);
        if (!this.variables.containsKey(string2)) {
            return bl;
        }
        return (Boolean)this.variables.get(string2);
    }

    public String getString(String string, String string2) {
        if (this.variables == null) {
            return string2;
        }
        String string3 = this.computeKey(string);
        if (!this.variables.containsKey(string3)) {
            return string2;
        }
        return (String)this.variables.get(string3);
    }

    private String computeKey(String string) {
        return string;
    }

    protected String[] getAllKeys() {
        String[] stringArray = new String[this.variables.keySet().size()];
        int n = 0;
        for (String string : this.variables.keySet()) {
            stringArray[n++] = string;
        }
        return stringArray;
    }

    private void registerVariableNames(String[] stringArray) {
        if (this.reservedVariables == null) {
            this.reservedVariables = new ArrayList();
        }
        for (int i = 0; i < stringArray.length; ++i) {
            this.reservedVariables.add(stringArray[i]);
        }
    }

    private boolean isKeyRegistered(String string) {
        return this.reservedVariables != null && this.reservedVariables.contains(string);
    }

    protected int getRegisteredIntKey(String string) {
        return 0;
    }

    private void setRegisteredValue(String string, byte by) {
    }
}

