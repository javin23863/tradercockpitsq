package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TradeAnalysisChart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class TradesByCloseTypeChart extends TradeAnalysisChart {
   public TradesByCloseTypeChart() {
      this.name = L.tsq("Trades by Close Type");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      if (var1 == null) {
         return var4;
      }

      HashMap var5 = this.computeData(var1, var2);
      ArrayList var6 = new ArrayList(var5.entrySet());
      var6.sort(
         (var0, var1x) -> Integer.compare(
            ((TradesByCloseTypeChart.CloseTypeStats)var1x.getValue()).tradeCount, ((TradesByCloseTypeChart.CloseTypeStats)var0.getValue()).tradeCount
         )
      );
      int var7 = 0;

      for (TradesByCloseTypeChart.CloseTypeStats var9 : var5.values()) {
         var7 += var9.tradeCount;
      }

      for (Entry var16 : var6) {
         byte var10 = (Byte)var16.getKey();
         TradesByCloseTypeChart.CloseTypeStats var11 = (TradesByCloseTypeChart.CloseTypeStats)var16.getValue();
         double var12 = var7 > 0 ? var11.tradeCount * 100.0 / var7 : 0.0;
         String var14 = String.format("%s (%.1f%%)", getShortName(var10), var12);
         var4.addValue(L.tsq("Trades"), var14, var12);
      }

      return var4;
   }

   private HashMap<Byte, TradesByCloseTypeChart.CloseTypeStats> computeData(OrdersList var1, byte var2) {
      HashMap var3 = new HashMap();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Order var5 = var1.get(var4);
         TradesByCloseTypeChart.CloseTypeStats var6 = (TradesByCloseTypeChart.CloseTypeStats)var3.get(var5.CloseType);
         if (var6 == null) {
            var6 = new TradesByCloseTypeChart.CloseTypeStats();
            var3.put(var5.CloseType, var6);
         }

         var6.tradeCount++;
      }

      return var3;
   }

   public static String getShortName(byte var0) {
      switch (var0) {
         case 1:
            return "MAN";
         case 2:
            return "SL";
         case 3:
            return "PT";
         case 4:
            return "ET";
         case 5:
            return "EOD";
         case 6:
            return "EXP";
         case 7:
            return "REV";
         case 8:
            return "DEL";
         case 9:
            return "REP";
         case 10:
         case 15:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 56:
         case 57:
         case 58:
         case 59:
         default:
            return "?";
         case 11:
            return "OCA";
         case 12:
            return "COM";
         case 13:
            return "EOD-T";
         case 14:
            return "EOF";
         case 16:
            return "EOF-T";
         case 17:
            return "EOR";
         case 18:
            return "CTRL";
         case 19:
            return "XBAR";
         case 20:
            return "BE";
         case 21:
            return "TS";
         case 22:
            return "SIG";
         case 55:
            return "EOD-NO";
         case 60:
            return "DL";
      }
   }

   private class CloseTypeStats {
      int tradeCount = 0;
   }
}
