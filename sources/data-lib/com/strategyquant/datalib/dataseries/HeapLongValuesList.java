package com.strategyquant.datalib.dataseries;

import it.unimi.dsi.fastutil.longs.LongArrayList;

public class HeapLongValuesList implements ILongValuesList {
   private LongArrayList values = null;

   public HeapLongValuesList(int var1) {
      this.values = new LongArrayList(var1);
   }

   @Override
   public long getLong(int var1) {
      return this.values.getLong(var1);
   }

   @Override
   public int size() {
      return this.values.size();
   }

   @Override
   public void setLong(int var1, long var2) {
      this.values.set(var1, var2);
   }

   @Override
   public void addLong(long var1) {
      this.values.add(var1);
   }

   @Override
   public void clear() {
      this.values.clear();
   }
}
