package com.strategyquant.datalib.dataseries;

public interface IDoubleValuesList {
   double getDouble(int var1);

   int size();

   void setDouble(int var1, double var2);

   void addDouble(double var1);

   void clear();

   long getCapacity();

   void release();

   void addDoubleValues(int var1, double var2);
}
