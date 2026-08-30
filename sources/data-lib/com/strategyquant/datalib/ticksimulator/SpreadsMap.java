package com.strategyquant.datalib.ticksimulator;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.InstrumentInfo;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.ArrayList;

public class SpreadsMap {
   Int2DoubleOpenHashMap map;
   private int lastUsedSpreadHashKey = 0;
   private double lastUsedSpread;

   public SpreadsMap(ArrayList<ChartDef> var1) {
      this.map = new Int2DoubleOpenHashMap();

      for (ChartDef var3 : var1) {
         int var4 = var3.getConnectionHash() + var3.getSymbolHash();
         double var5 = var3.getSpread();
         InstrumentInfo var7 = var3.getInstrumentInfo();
         double var8 = var5 == -1.0 ? -1.0 : var5 * var7.tickSize;
         this.map.put(var4, var8);
      }
   }

   public double getSpread(int var1) {
      if (this.lastUsedSpreadHashKey == var1) {
         return this.lastUsedSpread;
      }

      this.lastUsedSpread = this.map.get(var1);
      this.lastUsedSpreadHashKey = var1;
      return this.lastUsedSpread;
   }

   public boolean containsKey(int var1) {
      return this.map.containsKey(var1);
   }
}
