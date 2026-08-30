package SQ.Functions;

import SQ.Internal.Strategy;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;

public class OrderFunctions {
   public static boolean identify(ILiveOrder var0, Strategy var1, String var2, int var3, int var4, String var5) {
      if (!var0.getStrategyName().equals(var1.getStrategyName())) {
         return false;
      }

      if (!var2.equals("Any")) {
         if (var2.equals("Current")) {
            if (!var0.getSymbol().equals(var1.MarketData.Chart(0).Symbol)) {
               return false;
            }
         } else if (!var0.getSymbol().equals(var2)) {
            return false;
         }
      }

      if (var3 != 0 && var0.getDirection() != var3) {
         return false;
      } else {
         return var4 != 0 && var0.getMagicNumber() != var4 ? false : var5 == null || var5.equals("") || var0.getComment().contains(var5);
      }
   }

   public static boolean identify(Order var0, Strategy var1, String var2, int var3, int var4, String var5) {
      if (!var0.StrategyName.equals(var1.getStrategyName())) {
         return false;
      }

      if (!var2.equals("Any")) {
         if (var2.equals("Current")) {
            if (!var0.Symbol.equals(var1.MarketData.Chart(0).Symbol)) {
               return false;
            }
         } else if (!var0.Symbol.equals(var2)) {
            return false;
         }
      }

      if (var3 != 0 && var0.getDirection() != var3) {
         return false;
      } else {
         return var4 != 0 && var0.MagicNumber != var4 ? false : var5 == null || var5.equals("") || var0.Comment.contains(var5);
      }
   }

   public static ILiveOrder findLiveOrder(Strategy var0, String var1, int var2, int var3, String var4) {
      for (int var5 = 0; var5 < var0.Trader.getOpenOrdersCount(true); var5++) {
         ILiveOrder var6 = var0.Trader.getOpenOrder(var5, true);
         if (identify(var6, var0, var1, var2, var3, var4)) {
            return var6;
         }
      }

      return null;
   }

   public static Order findHistoryOrder(Strategy var0, String var1, int var2, int var3, String var4) {
      for (int var5 = 0; var5 < var0.Trader.getHistoryOrdersCount(); var5++) {
         Order var6 = var0.Trader.getHistoryOrder(var5);
         if (identify(var6, var0, var1, var2, var3, var4)) {
            return var6;
         }
      }

      return null;
   }

   public static Order findLastHistoryOrder(Strategy var0, String var1, int var2, int var3, String var4) {
      for (int var5 = var0.Trader.getHistoryOrdersCount() - 1; var5 >= 0; var5--) {
         Order var6 = var0.Trader.getHistoryOrder(var5);
         if (identify(var6, var0, var1, var2, var3, var4)) {
            return var6;
         }
      }

      return null;
   }
}
