package SQ.Columns.Databanks;

import SQ.Functions.StatFunctions;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

public class AHPR extends DatabankColumn {
   public AHPR() {
      super(L.tsq("AHPR"), "Decimal2", (byte)2, 0.0, 0.0, 40.0);
      this.setTooltip(L.tsq("Arithmetic Holding Period Return"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      AHPR.TempData var7 = this.init(var3);

      for (int var8 = 0; var8 < var3.size(); var8++) {
         Order var9 = var3.get(var8);
         int var10 = SQTime.getYear(var9.CloseTime);
         if (var7.yearlyReturn.containsKey(var10)) {
            double var11 = var7.yearlyReturn.get(var10);
            var11 += var9.PctPL;
            var7.yearlyReturn.put(var10, var11);
         }
      }

      DoubleArrayList var15 = new DoubleArrayList();
      IntSet var16 = var7.yearlyReturn.keySet();
      IntIterator var17 = var16.iterator();

      while (var17.hasNext()) {
         int var19 = var17.nextInt();
         var15.add(var7.yearlyReturn.get(var19));
      }

      double var20 = this.round2(StatFunctions.computeAverage(var15));
      double var13 = StatFunctions.computeStdev(var20, var15);
      var1.set("SHPR", var13);
      return var20;
   }

   private AHPR.TempData init(OrdersList var1) {
      AHPR.TempData var2 = new AHPR.TempData();
      var2.yearlyReturn = new Int2DoubleOpenHashMap();
      if (!var1.isEmpty()) {
         for (int var3 = 0; var3 < var1.size(); var3++) {
            Order var4 = var1.get(var3);
            if (var2.firstOrderDay == -1L || var4.OpenTime < var2.firstOrderDay) {
               var2.firstOrderDay = var4.OpenTime;
            }

            if (var2.lastOrderDay == -1L || var4.CloseTime > var2.lastOrderDay) {
               var2.lastOrderDay = var4.CloseTime;
            }
         }
      }

      if (var2.firstOrderDay != -1L && var2.lastOrderDay != -1L) {
         int var6 = SQTime.getYear(var2.firstOrderDay);
         int var7 = SQTime.getYear(var2.lastOrderDay);

         for (int var5 = var6; var5 <= var7; var5++) {
            var2.yearlyReturn.put(var5, 0.0);
         }
      }

      return var2;
   }

   class TempData {
      public Int2DoubleOpenHashMap yearlyReturn;
      public long firstOrderDay = -1L;
      public long lastOrderDay = -1L;
   }
}
