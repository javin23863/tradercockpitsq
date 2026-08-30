/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

public class MemoryInfo {
    public long allocatedMemory = 0L;
    public int allocatedObjects = 0;
    public long totalAllocatedMemory = 0L;
    public int totalAllocatedObjects = 0;
    public long totalDeallocatedMemory = 0L;
    public int totalDeallocatedObjects = 0;
    public int reallocations = 0;

    public void copyFrom(MemoryInfo memoryInfo) {
        this.allocatedMemory = memoryInfo.allocatedMemory;
        this.allocatedObjects = memoryInfo.allocatedObjects;
        this.totalAllocatedMemory = memoryInfo.totalAllocatedMemory;
        this.totalAllocatedObjects = memoryInfo.totalAllocatedObjects;
        this.totalDeallocatedMemory = memoryInfo.totalDeallocatedMemory;
        this.totalDeallocatedObjects = memoryInfo.totalDeallocatedObjects;
        this.reallocations = memoryInfo.reallocations;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("\n\n-- OFFHEAP MEMORY INFO -------------------\n");
        stringBuilder.append("allocatedMemory=");
        stringBuilder.append(this.allocatedMemory);
        stringBuilder.append("\n");
        stringBuilder.append("allocatedObjects=");
        stringBuilder.append(this.allocatedObjects);
        stringBuilder.append("\n");
        stringBuilder.append("totalAllocatedMemory=");
        stringBuilder.append(this.totalAllocatedMemory);
        stringBuilder.append("\n");
        stringBuilder.append("totalAllocatedObjects=");
        stringBuilder.append(this.totalAllocatedObjects);
        stringBuilder.append("\n");
        stringBuilder.append("totalDeallocatedMemory=");
        stringBuilder.append(this.totalDeallocatedMemory);
        stringBuilder.append("\n");
        stringBuilder.append("totalDeallocatedObjects=");
        stringBuilder.append(this.totalDeallocatedObjects);
        stringBuilder.append("\n");
        stringBuilder.append("reallocations=");
        stringBuilder.append(this.reallocations);
        stringBuilder.append("\n");
        stringBuilder.append("------------------------------------------------\n\n");
        return stringBuilder.toString();
    }
}

