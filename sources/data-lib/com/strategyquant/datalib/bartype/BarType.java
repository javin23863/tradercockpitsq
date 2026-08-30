package com.strategyquant.datalib.bartype;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.DataException;
import java.io.Serializable;

public abstract class BarType implements Serializable {
   protected int barTimeType;
   protected final String timeframe;

   public BarType(int var1, String var2) {
      this.barTimeType = var1;
      this.timeframe = var2;
   }

   public String getTimeframe() {
      return this.timeframe;
   }

   public String toString(String var1) {
      return var1;
   }

   public int getBarTimeType() {
      return this.barTimeType;
   }

   public void processTick(TickEvent var1, BarTypeStatus var2, int var3) throws DataException {
      this.processTickImplementation(var1, var2, var3);
   }

   public abstract void processTickImplementation(TickEvent var1, BarTypeStatus var2, int var3) throws DataException;

   public abstract BarType clone(String var1);

   public abstract BarType clone();

   public abstract boolean isTickBar();

   public abstract String checkCanBeComputedFrom(String var1) throws DataException;

   public abstract String getBaseTF();

   public abstract String getTickTF();

   public abstract long estimateStartDate(int var1, long var2);

   public abstract boolean checkTimeframeIsSupported(String var1);

   public String toStr() {
      return "BarTypeObj: " + this.toString();
   }

   public abstract int getPeriodInMS();
}
