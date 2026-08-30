package com.strategyquant.datalib.dataseries;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQDoubleArrayList {
   public static final Logger Log = LoggerFactory.getLogger("SQDoubleArrayList");
   public static final int DEFAULT_CAPACITY = 5000;
   private static final Object EMPTY = new double[0];
   public double[] buffer;
   public int elementsCount;

   public SQDoubleArrayList() {
      this(5000);
   }

   public SQDoubleArrayList(int var1) {
      this.ensureBufferSpace(var1);
   }

   public void add(double var1) {
      this.ensureBufferSpace(1);
      this.buffer[this.elementsCount++] = var1;
   }

   public double getDouble(int var1) {
      assert var1 >= 0 && var1 < this.elementsCount : "Index out of bounds.";
      return this.buffer[var1];
   }

   public double set(int var1, double var2) {
      assert var1 >= 0 && var1 < this.elementsCount : "Index out of bounds.";
      double var4 = this.buffer[var1];
      this.buffer[var1] = var2;
      return var4;
   }

   public void ensureCapacity(int var1) {
      if (var1 > this.buffer.length) {
         this.ensureBufferSpace(var1 - this.elementsCount);
      }
   }

   protected void ensureBufferSpace(int var1) {
      int var2 = this.buffer == null ? 0 : this.buffer.length;
      if (var2 < this.elementsCount + var1) {
         int var3 = (int)((this.elementsCount + var1) * 1.3);
         double[] var4 = new double[var3];
         if (var2 > 0) {
            System.arraycopy(this.buffer, 0, var4, 0, this.buffer.length);
         }

         this.buffer = var4;
      }
   }

   public void clear() {
      Arrays.fill(this.buffer, 0, this.elementsCount, 0.0);
      this.elementsCount = 0;
   }

   public void release() {
      this.buffer = (double[])EMPTY;
      this.elementsCount = 0;
   }

   public long getCapacity() {
      return this.buffer.length;
   }

   public void addValues(int var1, double var2) {
      this.ensureBufferSpace(var1);
      Arrays.fill(this.buffer, this.elementsCount, this.elementsCount + var1, var2);
      this.elementsCount += var1;
   }
}
