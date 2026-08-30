/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.memory;

import com.strategyquant.lib.MemoryInfo;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.offheap.IOffHeapCallback;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

public class OffHeapMemory {
    public static final Logger Log = LoggerFactory.getLogger((String)"OffHeapMemory");
    private StampedLock lock = new StampedLock();
    private MemoryInfo memoryInfo = new MemoryInfo();
    private Int2ObjectOpenHashMap<OffHeapObject> mapOffHeapObjects = new Int2ObjectOpenHashMap();
    private static final Unsafe unsafe;
    private static final long byteArrayOffset;
    protected static final long MEGABYTE = 0x100000L;
    public static final long AddressDeallocated = -1L;
    public static final long AddressUnset = -2L;
    private static final OffHeapMemory instance;

    public static long allocate(IOffHeapCallback iOffHeapCallback, long l) {
        return instance._allocate(iOffHeapCallback, l);
    }

    public static long reallocate(IOffHeapCallback iOffHeapCallback, long l, long l2) {
        return instance._reallocate(iOffHeapCallback, l, l2);
    }

    public static void deallocate(IOffHeapCallback iOffHeapCallback, long l) {
        instance._deallocate(iOffHeapCallback, l);
    }

    public static void copyMemory(long l, long l2, long l3) {
        instance._copyMemory(l, l2, l3);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void _copyMemory(long l, long l2, long l3) {
        long l4 = this.lock.writeLock();
        try {
            unsafe.copyMemory(l, l2, l3);
        }
        finally {
            this.lock.unlock(l4);
        }
    }

    public static void copyMemory(Object object, long l, Object object2, long l2, long l3) {
        instance._copyMemory(object, l, object2, l2, l3);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void _copyMemory(Object object, long l, Object object2, long l2, long l3) {
        long l4 = this.lock.writeLock();
        try {
            unsafe.copyMemory(object, l, object2, l2, l3);
        }
        finally {
            this.lock.unlock(l4);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private long _allocate(IOffHeapCallback iOffHeapCallback, long l) {
        long l2 = this.lock.writeLock();
        try {
            long l3 = unsafe.allocateMemory(l);
            this.memoryInfo.allocatedMemory += l;
            ++this.memoryInfo.allocatedObjects;
            ++this.memoryInfo.totalAllocatedObjects;
            this.memoryInfo.totalAllocatedMemory += l;
            this.mapOffHeapObjects.put(iOffHeapCallback.hashCode(), (Object)new OffHeapObject(iOffHeapCallback, l));
            long l4 = l3;
            return l4;
        }
        finally {
            this.lock.unlock(l2);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private long _reallocate(IOffHeapCallback iOffHeapCallback, long l, long l2) {
        long l3 = this.lock.writeLock();
        try {
            long l4 = unsafe.reallocateMemory(l, l2);
            ++this.memoryInfo.reallocations;
            if (!this.mapOffHeapObjects.containsKey(iOffHeapCallback.hashCode())) {
                Log.error("Reallocate - Map of OffHeap Objects doesn't contains key " + iOffHeapCallback.hashCode() + " for object " + iOffHeapCallback.toString());
                this.memoryInfo.allocatedMemory += l2;
                ++this.memoryInfo.allocatedObjects;
                ++this.memoryInfo.totalAllocatedObjects;
                this.memoryInfo.totalAllocatedMemory += l2;
                this.mapOffHeapObjects.put(iOffHeapCallback.hashCode(), (Object)new OffHeapObject(iOffHeapCallback, l2));
            } else {
                OffHeapObject offHeapObject = (OffHeapObject)this.mapOffHeapObjects.get(iOffHeapCallback.hashCode());
                this.memoryInfo.allocatedMemory -= offHeapObject.size;
                this.memoryInfo.totalAllocatedMemory -= offHeapObject.size;
                offHeapObject.size = l2;
                this.memoryInfo.allocatedMemory += offHeapObject.size;
                this.memoryInfo.totalAllocatedMemory += offHeapObject.size;
            }
            long l5 = l4;
            return l5;
        }
        finally {
            this.lock.unlock(l3);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void _deallocate(IOffHeapCallback iOffHeapCallback, long l) {
        long l2 = this.lock.writeLock();
        try {
            unsafe.freeMemory(l);
            ++this.memoryInfo.totalDeallocatedObjects;
            if (!this.mapOffHeapObjects.containsKey(iOffHeapCallback.hashCode())) {
                Log.error("Deallocate - Map of OffHeap Objects doesn't contains key " + iOffHeapCallback.hashCode() + " for object " + iOffHeapCallback.toString() + ", ADDRESS: " + l);
            } else {
                OffHeapObject offHeapObject = (OffHeapObject)this.mapOffHeapObjects.get(iOffHeapCallback.hashCode());
                offHeapObject.object = null;
                this.memoryInfo.allocatedMemory -= offHeapObject.size;
                --this.memoryInfo.allocatedObjects;
                this.memoryInfo.totalDeallocatedMemory += offHeapObject.size;
                this.mapOffHeapObjects.remove(iOffHeapCallback.hashCode());
            }
        }
        finally {
            this.lock.unlock(l2);
        }
    }

    public static void getInfo(MemoryInfo memoryInfo) {
        instance._getInfo(memoryInfo);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void _getInfo(MemoryInfo memoryInfo) {
        long l = this.lock.readLock();
        try {
            memoryInfo.copyFrom(this.memoryInfo);
        }
        finally {
            this.lock.unlock(l);
        }
    }

    public static final long getByteArrayOffset() {
        return byteArrayOffset;
    }

    public static final void putDouble(long l, double d) {
        unsafe.putDouble(l, d);
    }

    public static final void putLong(long l, long l2) {
        unsafe.putLong(l, l2);
    }

    public static final void putInt(long l, int n) {
        unsafe.putInt(l, n);
    }

    public static final void putShort(long l, short s) {
        unsafe.putShort(l, s);
    }

    public static final void putByte(long l, byte by) {
        unsafe.putByte(l, by);
    }

    public static final double getDouble(long l) {
        return unsafe.getDouble(l);
    }

    public static final float getFloat(long l) {
        return unsafe.getFloat(l);
    }

    public static final long getLong(long l) {
        return unsafe.getLong(l);
    }

    public static final int getInt(long l) {
        return unsafe.getInt(l);
    }

    public static final short getShort(long l) {
        return unsafe.getShort(l);
    }

    public static final byte getByte(long l) {
        return unsafe.getByte(l);
    }

    public static String getOffHeapObjects() {
        return instance._getOffHeapObjects();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private String _getOffHeapObjects() {
        long l = this.lock.writeLock();
        try {
            ObjectCollection objectCollection = this.mapOffHeapObjects.values();
            ArrayList arrayList = new ArrayList();
            arrayList.addAll(objectCollection);
            StringBuilder stringBuilder = new StringBuilder("\n\n-- OFFHEAP OBJECTS: " + arrayList.size() + " -------------------\n");
            for (int i = 0; i < arrayList.size() && i < arrayList.size(); ++i) {
                OffHeapObject offHeapObject = (OffHeapObject)arrayList.get(i);
                stringBuilder.append(offHeapObject.object.getIdentification());
                stringBuilder.append("=");
                stringBuilder.append(offHeapObject.object);
                stringBuilder.append("\n");
            }
            stringBuilder.append("------------------------------------------------\n\n");
            String string = stringBuilder.toString();
            return string;
        }
        finally {
            this.lock.unlock(l);
        }
    }

    public static void throwMemoryException(long l) {
        OffHeapMemory.throwMemoryException(l, "General");
    }

    public static void throwMemoryException(long l, String string) {
        StringBuffer stringBuffer = new StringBuffer("OffHeap - Accessing inorrect address - General");
        if (l == -1L) {
            stringBuffer.append(" - Deallocated");
        } else if (l == -2L) {
            stringBuffer.append(" - Unset");
        } else if (l == -2L) {
            stringBuffer.append(" - Unknown :");
            stringBuffer.append(l);
        }
        stringBuffer.append("Stack trace:\n");
        stringBuffer.append(SQUtils.getStackTrace());
        String string2 = stringBuffer.toString();
        Log.error(string2);
        throw new IllegalArgumentException(string2);
    }

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
        instance = new OffHeapMemory();
    }

    private class OffHeapObject {
        public long size = 0L;
        public IOffHeapCallback object;

        public OffHeapObject(IOffHeapCallback iOffHeapCallback, long l) {
            this.object = iOffHeapCallback;
            this.size = l;
        }
    }
}

