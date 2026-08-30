package SQ.TradingOptions;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class MaxTradesPerDay extends TradingOption {
   @Parameter(name = "Maximum Trades Per Day", defaultValue = "0", minValue = 0.0, category = "Trading options")
   @Help("Maximum allowed number of trades per day, 0 means there is no limit.")
   @ForEngine("*,-SP,-SA")
   public int MaxTradesPerDay;
   private int historyPositionPreviousDay = -1;
   private long openTimeToday;
   private long EODTime;
   private boolean reachedLimitToday = false;

   public boolean isUsedInTrading() {
      return this.MaxTradesPerDay != 0;
   }

   public boolean OnBarUpdate(StrategyBase var1) throws TradingException {
      if (this.MaxTradesPerDay == 0) {
         return true;
      }

      long var2 = var1.MarketData.TimeCurrent();
      if (var2 > this.EODTime) {
         this.initForCurrentDay(var2);
      }

      if (this.reachedLimitToday) {
         return false;
      } else if (this.getNumberOfTradesToday(var1) >= this.MaxTradesPerDay) {
         this.reachedLimitToday = true;
         return false;
      } else {
         return true;
      }
   }

   private void initForCurrentDay(long var1) {
      this.EODTime = SQTime.correctDayEndMT(var1);
      this.openTimeToday = SQTime.correctDayStart(var1);
      this.reachedLimitToday = false;
   }

   private int getNumberOfTradesToday(StrategyBase var1) {
      int var2 = 0;
      int var3 = this.historyPositionPreviousDay + 1;

      for (int var4 = var3; var4 < var1.Trader.getHistoryOrdersCount(); var4++) {
         Order var5 = var1.Trader.getHistoryOrder(var4);
         if (!var5.isPendingOrder() && var5.StrategyName.equals(var1.getStrategyName())) {
            if (var5.OpenTime >= this.openTimeToday) {
               var2++;
            } else {
               this.historyPositionPreviousDay = var4;
            }
         }
      }

      for (int var8 = 0; var8 < var1.Trader.getOpenOrdersCount(true); var8++) {
         ILiveOrder var9 = var1.Trader.getOpenOrder(var8, true);
         if (var9.isFilled()) {
            long var6 = var9.isFilled() ? var9.getOpenTime() : var9.getOriginalOpenTime();
            if (var9.getStrategyName().equals(var1.getStrategyName()) && var6 >= this.openTimeToday) {
               var2++;
            }
         }
      }

      return var2;
   }

   public TradingOption getClone() {
      MaxTradesPerDay var1 = new MaxTradesPerDay();
      var1.MaxTradesPerDay = this.MaxTradesPerDay;
      return var1;
   }
}
