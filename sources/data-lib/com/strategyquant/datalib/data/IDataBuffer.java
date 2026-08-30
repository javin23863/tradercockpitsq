package com.strategyquant.datalib.data;

import com.strategyquant.datalib.TickEvent;

public interface IDataBuffer {
   void put(TickEvent var1);

   void printRing();

   long get(long var1, TickEvent[] var3);

   long getOne(long var1, TickEvent var3);
}
