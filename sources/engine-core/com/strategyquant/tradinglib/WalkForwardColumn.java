package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.table.TableColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WalkForwardColumn extends TableColumn {
   private static final Logger Log = LoggerFactory.getLogger(WalkForwardColumn.class);
   private byte valueType = -1;
   public static final String WFScoreStatsColumn = "WFScore";

   public byte getValueType() {
      return this.valueType;
   }

   public WalkForwardColumn(String var1, String var2, String var3, byte var4) {
      super(var2, var3);
      this.statsValueName = var1;
      this.valueType = var4;
   }

   public abstract double compute(WalkForwardResult var1);

   public synchronized void compute(WalkForwardResult var1, SQStats var2) {
      double var3 = this.compute(var1);
      var2.set(this.statsValueName, var3);
   }
}
