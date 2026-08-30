package SQ.TradingOptions;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Activator;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.simulator.Engines;

public class LimitTimeRange extends TradingOption {
   @Parameter(name = "Limit Time Range", defaultValue = "false", category = "Trading options")
   @Help("Limit trading to given time range?")
   @ForEngine("*,-SP,-SA")
   public boolean LimitTimeRange;
   @Parameter(name = "Time Range From", defaultValue = "0800", category = "Trading options")
   @Help("Time range for taking signal - from time.\nTime is in timezone of the data.")
   @Editor(type = 110)
   @Activator(param = "LimitTimeRange")
   @ForEngine("*,-SP,-SA")
   public int SignalTimeRangeFrom;
   @Parameter(name = "Time Range To", defaultValue = "1600", category = "Trading options")
   @Help("Time range for taking signal - to time.\nTime is in timezone of the data.")
   @Editor(type = 110)
   @Activator(param = "LimitTimeRange")
   @ForEngine("*,-SP,-SA")
   public int SignalTimeRangeTo;
   @Parameter(name = "Exit At End Of Range", defaultValue = "false", category = "Trading options")
   @Help("Close all positions at end of range?")
   @Activator(param = "LimitTimeRange")
   @ForEngine("*,-SP,-SA")
   public boolean ExitAtEndOfRange;
   @Parameter(name = "Order Types To Close", defaultValue = "0", category = "Trading options")
   @Help("Type of orders to be closed at end of range - by default all (live & pending) are closed. Works only if Exit At End Of Range = true.")
   @Editor(type = 40, values = "All=0,Live only=1,Pending only=2")
   @Activator(param = "LimitTimeRange")
   @ForEngine("*,-SP,-SA")
   public int OrderTypeToExit;
   private long dailySignalTimeRangeFrom;
   private long dailySignalTimeRangeTo;
   private boolean closedThisDay = false;

   public boolean isUsedInTrading() {
      return this.LimitTimeRange;
   }

   public boolean OnBarUpdate(StrategyBase var1) throws TradingException {
      if (!this.LimitTimeRange) {
         return true;
      }

      long var2 = var1.MarketData.TimeCurrent();
      long var4 = 0L;
      if (var2 > this.dailySignalTimeRangeTo) {
         this.initTimesForCurrentDay(var2);
      }

      return var2 + var4 >= this.dailySignalTimeRangeFrom && var2 + var4 < this.dailySignalTimeRangeTo;
   }

   public void OnTick(StrategyBase var1, TickEvent var2, boolean var3) throws Exception {
      if (this.LimitTimeRange) {
         long var4 = var2.getTime();
         if (this.ExitAtEndOfRange) {
            if (!var1.isTradestationEngine() && !this.closedThisDay && var4 >= this.dailySignalTimeRangeTo) {
               this.closeAllPositions(var1, var2);
               this.closedThisDay = true;
            }

            if (var1.isTradestationEngine() && !this.closedThisDay && var2.isBarClose() && var4 >= this.dailySignalTimeRangeTo) {
               this.closeAllPositions(var1, var2);
               this.closedThisDay = true;
            }
         }
      }
   }

   private void initTimesForCurrentDay(long var1) {
      this.dailySignalTimeRangeFrom = SQTime.setHHMM(var1, this.SignalTimeRangeFrom);
      this.dailySignalTimeRangeFrom = SQTime.setSecond(this.dailySignalTimeRangeFrom, 0);
      this.dailySignalTimeRangeFrom = SQTime.setMiliSeconds(this.dailySignalTimeRangeFrom, 0);
      this.dailySignalTimeRangeTo = SQTime.setHHMM(var1, this.SignalTimeRangeTo);
      this.dailySignalTimeRangeTo = SQTime.setSecond(this.dailySignalTimeRangeTo, 0);
      this.dailySignalTimeRangeTo = SQTime.setMiliSeconds(this.dailySignalTimeRangeTo, 0);
      if (this.SignalTimeRangeFrom >= this.SignalTimeRangeTo) {
         if (SQTime.getHHMM(var1) < this.SignalTimeRangeTo) {
            this.dailySignalTimeRangeFrom = SQTime.addDays(this.dailySignalTimeRangeFrom, -1);
         } else {
            this.dailySignalTimeRangeTo = SQTime.addDays(this.dailySignalTimeRangeTo, 1);
         }
      } else if (var1 > this.dailySignalTimeRangeTo) {
         this.dailySignalTimeRangeFrom = SQTime.addDays(this.dailySignalTimeRangeFrom, 1);
         this.dailySignalTimeRangeTo = SQTime.addDays(this.dailySignalTimeRangeTo, 1);
      }

      this.closedThisDay = false;
   }

   private void closeAllPositions(StrategyBase var1, TickEvent var2) throws TradingException {
      int var3 = var1.Trader.getOpenOrdersCount(false);

      for (int var4 = var3 - 1; var4 >= 0; var4--) {
         ILiveOrder var5 = var1.Trader.getOpenOrder(var4, false);
         if ((this.OrderTypeToExit != 1 || !var5.isPendingOrder()) && (this.OrderTypeToExit != 2 || var5.isPendingOrder())) {
            if (Engines.isTradestationEngine(var1.getEngine())) {
               if (var2.isBarClose() && var5.getStrategyName().equals(var1.getStrategyName())) {
                  var5.Close((byte)17);
               }
            } else if (var5.getStrategyName().equals(var1.getStrategyName())) {
               var5.Close((byte)17);
            }
         }
      }
   }

   public TradingOption getClone() {
      LimitTimeRange var1 = new LimitTimeRange();
      var1.LimitTimeRange = this.LimitTimeRange;
      var1.SignalTimeRangeFrom = this.SignalTimeRangeFrom;
      var1.SignalTimeRangeTo = this.SignalTimeRangeTo;
      var1.ExitAtEndOfRange = this.ExitAtEndOfRange;
      var1.OrderTypeToExit = this.OrderTypeToExit;
      return var1;
   }
}
