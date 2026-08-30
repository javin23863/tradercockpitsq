package com.strategyquant.tradinglib.engine;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.ChartSetup;
import java.util.ArrayList;

public class ChartSetups extends ArrayList<ChartSetup> implements ISQCloneable<ChartSetups> {
   public ChartSetups() {
   }

   public ChartSetups(String var1, String var2, String var3, long var4, long var6, double var8, String var10) throws DataException {
      ChartDef var11 = new ChartDef(var1, var2, var3, var4, var6, var8, var10);
      ChartSetup var12 = new ChartSetup();
      var12.addChart(var11);
      this.add(var12);
   }

   public ChartSetups(ChartSetup var1) {
      this.add(var1);
   }

   public ChartSetup getMainSetup() {
      return this.get(0);
   }

   public ChartSetups getClone() {
      ChartSetups var1 = new ChartSetups();

      for (int var2 = 0; var2 < this.size(); var2++) {
         var1.add(this.get(var2).getClone());
      }

      return var1;
   }

   public ChartSetups getClone(long var1, long var3) {
      ChartSetups var5 = new ChartSetups();

      for (int var6 = 0; var6 < this.size(); var6++) {
         var5.add(this.get(var6).getClone(var1, var3));
      }

      return var5;
   }
}
