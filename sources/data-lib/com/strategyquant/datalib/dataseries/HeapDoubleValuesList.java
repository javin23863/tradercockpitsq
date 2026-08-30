package com.strategyquant.datalib.dataseries;

public class HeapDoubleValuesList implements IDoubleValuesList {
   private SQDoubleArrayList values = null;

   public HeapDoubleValuesList(int var1) {
      this.values = new SQDoubleArrayList(var1);
   }

   @Override
   public double getDouble(int var1) {
      return this.values.getDouble(var1);
   }

   @Override
   public int size() {
      return this.values.elementsCount;
   }

   @Override
   public void setDouble(int var1, double var2) {
      this.values.set(var1, var2);
   }

   @Override
   public void addDouble(double var1) {
      this.values.add(var1);
   }

   @Override
   public void clear() {
      this.values.clear();
   }

   @Override
   public long getCapacity() {
      return this.values.getCapacity();
   }

   @Override
   public void release() {
      this.values.release();
   }

   @Override
   public void addDoubleValues(int var1, double var2) {
      this.values.addValues(var1, var2);
   }
}
