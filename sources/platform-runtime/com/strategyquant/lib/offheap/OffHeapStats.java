/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.offheap;

import com.strategyquant.lib.MemoryInfo;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OffHeapStats {
    private static final OffHeapStats instance = new OffHeapStats();
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private MemoryInfo memoryInfo = new MemoryInfo();

    public static void registerAllocate(long l) {
        instance._registerAllocate(l);
    }

    private void _registerAllocate(long l) {
        this.readWriteLock.writeLock().lock();
        try {
            this.memoryInfo.allocatedMemory += l;
            ++this.memoryInfo.allocatedObjects;
            ++this.memoryInfo.totalAllocatedObjects;
            this.memoryInfo.totalAllocatedMemory += l;
        }
        finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public static void registerReallocate(long l) {
        instance._registerReallocate(l);
    }

    private void _registerReallocate(long l) {
        this.readWriteLock.writeLock().lock();
        try {
            this.memoryInfo.allocatedMemory += l;
            this.memoryInfo.totalAllocatedMemory += l;
        }
        finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public static void registerDeallocate(long l) {
        instance._registerDeallocate(l);
    }

    private void _registerDeallocate(long l) {
        this.readWriteLock.writeLock().lock();
        try {
            this.memoryInfo.allocatedMemory -= l;
            --this.memoryInfo.allocatedObjects;
            this.memoryInfo.totalDeallocatedMemory += l;
            ++this.memoryInfo.totalDeallocatedObjects;
        }
        finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public static void getInfo(MemoryInfo memoryInfo) {
        instance._getInfo(memoryInfo);
    }

    private void _getInfo(MemoryInfo memoryInfo) {
        this.readWriteLock.readLock().lock();
        try {
            memoryInfo.copyFrom(this.memoryInfo);
        }
        finally {
            this.readWriteLock.readLock().unlock();
        }
    }
}

