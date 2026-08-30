package com.strategyquant.datalib.dataseries;

public interface ILongValuesList {
   long getLong(int var1);

   int size();

   void setLong(int var1, long var2);

   void addLong(long var1);

   void clear();
}
