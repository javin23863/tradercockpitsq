/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.settings.ValuesMapKeys;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.io.Serializable;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Content;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValuesMap
implements IXMLAble,
Serializable {
    public static final Logger Log = LoggerFactory.getLogger((String)"ValuesMap");
    public static final byte BYTE_MISSING = -99;
    public static final int INT_MISSING = -999999999;
    public static final long LONG_MISSING = -999999999L;
    public static final double DOUBLE_MISSING = -9.99999999E8;
    public static final float FLOAT_MISSING = -1.0E9f;
    protected Int2ObjectOpenHashMap<Object> values = new Int2ObjectOpenHashMap();
    private static final Int2ObjectOpenHashMap<String> unknownKeysMap = new Int2ObjectOpenHashMap();
    private static final StampedLock stampedLock = new StampedLock();

    public void set(int n, Object object) {
        this.values.put(n, object);
    }

    public void set(String string, Object object) {
        this.values.put(this.getKeyHash(string, true), object);
    }

    public void remove(int n) {
        this.values.remove(n);
    }

    public Object get(String string) {
        if (string == null) {
            return null;
        }
        int n = this.getKeyHash(string, false);
        if (n == 0) {
            return null;
        }
        return this.values.get(n);
    }

    public Object get(int n) {
        if (n == 0) {
            return null;
        }
        return this.values.get(n);
    }

    public Object get(String string, Object object) {
        if (string == null) {
            return object;
        }
        int n = this.getKeyHash(string, false);
        if (this.values.containsKey(n)) {
            return this.values.get(n);
        }
        return object;
    }

    public boolean containsKey(String string) {
        if (string == null) {
            return false;
        }
        return this.values.containsKey(this.getKeyHash(string, false));
    }

    public boolean containsKey(int n) {
        if (n == 0) {
            return false;
        }
        return this.values.containsKey(n);
    }

    public void clear() {
        this.values.clear();
    }

    public int getInt(String string) {
        return this.getInt(string, 0);
    }

    public boolean getBoolean(String string) {
        Object object = this.get(string);
        if (object == null) {
            return false;
        }
        if (!(object instanceof Boolean)) {
            return false;
        }
        return (Boolean)object;
    }

    public long getLong(String string) {
        Object object = this.get(string);
        if (object == null) {
            return 0L;
        }
        return (Long)object;
    }

    public long getLong(String string, long l) {
        Object object = this.get(string);
        if (object == null) {
            return l;
        }
        return (Long)object;
    }

    public int getInt(String string, int n) {
        Object object = this.get(string);
        if (object == null) {
            return n;
        }
        if (object instanceof Double) {
            return (int)((Double)object).doubleValue();
        }
        return (Integer)object;
    }

    public String getString(String string) {
        return (String)this.get(string);
    }

    public String getString(String string, String string2) {
        Object object = this.get(string);
        if (object == null) {
            return string2;
        }
        return (String)object;
    }

    public void setString(String string, String string2) {
        this.set(string, (Object)string2);
    }

    public double getDouble(String string) {
        return this.getDouble(string, -9.99999999E8);
    }

    public double getDouble(String string, double d) {
        if (string == null) {
            return d;
        }
        int n = this.getKeyHash(string, false);
        if (n == 0 || this.values == null || !this.values.containsKey(n)) {
            return d;
        }
        try {
            return (Double)this.values.get(n);
        }
        catch (Exception exception) {
            Object object = this.values.get(n);
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

    public boolean getBoolean(String string, boolean bl) {
        if (string == null) {
            return bl;
        }
        int n = this.getKeyHash(string, false);
        if (n == 0 || this.values == null || !this.values.containsKey(n)) {
            return bl;
        }
        return (Boolean)this.values.get(n);
    }

    public static ValuesMap fromJSON(JSONObject jSONObject) {
        ValuesMap valuesMap = new ValuesMap();
        for (String string : jSONObject.keySet()) {
            String[] stringArray;
            try {
                JSONArray jSONArray = jSONObject.getJSONArray(string);
                stringArray = new String[jSONArray.length()];
                for (int i = 0; i < jSONArray.length(); ++i) {
                    stringArray[i] = jSONArray.get(i).toString();
                }
                valuesMap.set(string, (Object)stringArray);
            }
            catch (Exception exception) {
                stringArray = jSONObject.get(string);
                valuesMap.set(string, (Object)stringArray);
            }
        }
        return valuesMap;
    }

    public JSONObject toJSON() {
        JSONObject jSONObject = new JSONObject();
        for (Int2ObjectMap.Entry entry : this.values.int2ObjectEntrySet()) {
            int n = entry.getIntKey();
            Object object = entry.getValue();
            String string = this.getKey(n);
            if (object.getClass().isArray()) {
                Object[] objectArray;
                JSONArray jSONArray = new JSONArray();
                for (Object object2 : objectArray = (Object[])object) {
                    jSONArray.put(object2);
                }
                jSONObject.put(string, (Object)jSONArray);
                continue;
            }
            jSONObject.put(string, object);
        }
        return jSONObject;
    }

    public String[] getAllKeys() {
        if (this.values == null || this.values.size() == 0) {
            return new String[0];
        }
        String[] stringArray = new String[this.values.keySet().size()];
        int n = 0;
        IntIterator intIterator = this.values.keySet().iterator();
        while (intIterator.hasNext()) {
            int n2 = (Integer)intIterator.next();
            stringArray[n++] = this.getKey(n2);
        }
        return stringArray;
    }

    public void removeUnsavableValues() {
        IntIterator intIterator = this.values.keySet().iterator();
        while (intIterator.hasNext()) {
            int n = intIterator.nextInt();
            Object object = this.values.get(n);
            Element element = XMLUtil.valueToElement(SQUtils.encodeXmlKey(this.getKey(n)), object);
            if (element != null) continue;
            intIterator.remove();
        }
    }

    @Override
    public Element getXML() {
        Element element = new Element("ValuesMap");
        IntIterator intIterator = this.values.keySet().iterator();
        while (intIterator.hasNext()) {
            Element element2;
            int n = (Integer)intIterator.next();
            Object object = this.values.get(n);
            String string = this.getKey(n);
            if (string == null || (element2 = XMLUtil.valueToElement(SQUtils.encodeXmlKey(string), object)) == null) continue;
            element.addContent((Content)element2);
        }
        return element;
    }

    @Override
    public void setFromXML(Element element) {
        this.setFromXML(element, null);
    }

    public void setFromXML(Element element, int[] nArray) {
        this.values.clear();
        for (Element element2 : element.getChildren()) {
            String string = SQUtils.decodeXmlKey(element2.getName());
            if (nArray != null && this.isInArray(string.hashCode(), nArray)) continue;
            Object object = null;
            if (element2.getContentSize() > 0) {
                object = XMLUtil.elementToValue(element2);
            }
            if (string.equals("MinMaxSLPT.MinimumSLPT")) {
                this.set("MinMaxSLPT.MinimumSL", object);
                this.set("MinMaxSLPT.MinimumPT", object);
                continue;
            }
            if (string.equals("MinMaxSLPT.MaximumSLPT")) {
                this.set("MinMaxSLPT.MaximumSL", object);
                this.set("MinMaxSLPT.MaximumPT", object);
                continue;
            }
            this.set(string, object);
        }
    }

    public void removeIgnoredKeys(int[] nArray) {
        if (nArray == null) {
            return;
        }
        for (int i = 0; i < nArray.length; ++i) {
            if (!this.values.containsKey(nArray[i])) continue;
            this.values.remove(nArray[i]);
        }
    }

    protected boolean isInArray(int n, int[] nArray) {
        for (int i = 0; i < nArray.length; ++i) {
            if (n != nArray[i]) continue;
            return true;
        }
        return false;
    }

    public ValuesMap clone() {
        ValuesMap valuesMap = new ValuesMap();
        valuesMap.values = SQUtils.cloneValuesMap(this.values);
        return valuesMap;
    }

    public int hashCode() {
        int n = 0;
        IntIterator intIterator = this.values.keySet().iterator();
        while (intIterator.hasNext()) {
            int n2 = (Integer)intIterator.next();
            Object object = this.values.get(n2);
            n += XMLUtil.valueHash(n2, object);
        }
        return n;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private int registerKey(String string, boolean bl) {
        int n = string.hashCode();
        long l = stampedLock.tryOptimisticRead();
        if (unknownKeysMap.containsKey(n) && stampedLock.validate(l)) {
            return n;
        }
        l = stampedLock.writeLock();
        try {
            if (unknownKeysMap.containsKey(n)) {
                int n2 = n;
                return n2;
            }
            if (string.startsWith("MEC_")) {
                // empty if block
            }
            if (bl) {
                unknownKeysMap.put(n, (Object)string);
                int n3 = n;
                return n3;
            }
            int n4 = 0;
            return n4;
        }
        finally {
            stampedLock.unlockWrite(l);
        }
    }

    protected int getKeyHash(String string) {
        return this.getKeyHash(string, false);
    }

    protected int getKeyHash(String string, boolean bl) {
        int n = string.hashCode();
        if (ValuesMapKeys.isDefaultKey(n)) {
            return n;
        }
        if (string.startsWith("stats")) {
            return ValuesMap.getStatsIntKeyFromString(string);
        }
        n = this.registerKey(string, bl);
        return n;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private String getKey(int n) {
        String string = ValuesMapKeys.getDefaultKeyAsString(n);
        if (string != null) {
            return string;
        }
        long l = stampedLock.tryOptimisticRead();
        if (unknownKeysMap.containsKey(n) && stampedLock.validate(l)) {
            return (String)unknownKeysMap.get(n);
        }
        l = stampedLock.writeLock();
        try {
            if (unknownKeysMap.containsKey(n)) {
                String string2 = (String)unknownKeysMap.get(n);
                return string2;
            }
            String string3 = null;
            return string3;
        }
        finally {
            stampedLock.unlockWrite(l);
        }
    }

    public static int getStatsIntKeyFromString(String string) {
        return ValuesMapKeys.getStatsIntKeyFromString(string);
    }
}

