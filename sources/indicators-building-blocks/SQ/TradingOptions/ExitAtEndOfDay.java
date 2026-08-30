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

public class ExitAtEndOfDay extends TradingOption {
   @Parameter(name = "Exit At End Of Day", defaultValue = "false", category = "Trading options")
   @Help("Close all positions at end of day?")
   @ForEngine("*,-SP,-SA")
   public boolean ExitAtEndOfDay;
   @Parameter(name = "End Of Day Exit Time", defaultValue = "2355", category = "Trading options")
   @Help(
      "End Of Day Time when to close all positions. If set to 0, it will close all positions from previous day at midnight.\nTime is in timezone of the data."
   )
   @Editor(type = 110)
   @Activator(param = "ExitAtEndOfDay")
   @ForEngine("*,-SP,-SA")
   public int EODExitTime;
   private long dailyEODExitTime = -1L;
   private long EODTime = -1L;
   private boolean closedThisDay = false;
   private long previousTickTime = -1L;
   private boolean LimitTimeRangeChecked = false;
   private int SignalTimeRangeFrom = 0;
   private int SignalTimeRangeTo = 0;
   private long dailySignalTimeRangeFrom = 0L;
   private long dailySignalTimeRangeTo = 0L;
   private boolean LimitTimeRange = false;

   public boolean isUsedInTrading() {
      return this.ExitAtEndOfDay;
   }

   public boolean OnBarUpdate(StrategyBase var1) throws Exception {
      if (!this.ExitAtEndOfDay) {
         return true;
      }

      long var2 = var1.MarketData.TimeCurrent();
      if (!this.LimitTimeRangeChecked) {
         this.LimitTimeRangeChecked = true;
         TradingOption[] var4 = var1.getTradingOptions();

         for (int var5 = 0; var5 < var4.length; var5++) {
            if (var4[var5].getClass().getName().equals("SQ.TradingOptions.LimitTimeRange")) {
               LimitTimeRange var6 = (LimitTimeRange)var4[var5];
               if (var6.LimitTimeRange) {
                  this.LimitTimeRange = true;
                  this.SignalTimeRangeFrom = var6.SignalTimeRangeFrom;
                  this.SignalTimeRangeTo = var6.SignalTimeRangeTo;
               }
            }
         }
      }

      if (var2 != 0L && var2 > this.dailySignalTimeRangeTo && this.LimitTimeRange) {
         this.initLimitTimeRangeForCurrentDay(var2);
      }

      if (var2 != 0L && var2 > this.EODTime) {
         this.initTimesForCurrentDay(var2, var1);
      }

      if (!this.LimitTimeRange && var2 >= this.dailyEODExitTime) {
         return false;
      }

      if (this.LimitTimeRange) {
         if (this.dailyEODExitTime < this.dailySignalTimeRangeFrom) {
            if (var2 >= this.dailyEODExitTime && var2 < this.dailySignalTimeRangeFrom) {
               return false;
            }
         } else if (this.dailyEODExitTime >= this.dailySignalTimeRangeFrom
            && this.SignalTimeRangeFrom < this.SignalTimeRangeTo
            && var2 >= this.dailyEODExitTime) {
            return false;
         }
      }

      return true;
   }

   public void OnTick(StrategyBase var1, TickEvent var2, boolean var3) throws Exception {
      if (this.ExitAtEndOfDay) {
         long var4 = var2.getTime();
         if (!this.closedThisDay) {
            if (!this.LimitTimeRange && var4 >= this.dailyEODExitTime) {
               if (Engines.isTradestationEngine(var1.getEngine())) {
                  var3 = false;
               }

               this.closedThisDay = this.closeAllPositions(var1, var3, var4, var2);
            }

            if (this.LimitTimeRange) {
               if (this.dailyEODExitTime < this.dailySignalTimeRangeFrom) {
                  if (var4 >= this.dailyEODExitTime && var4 < this.dailySignalTimeRangeFrom) {
                     if (Engines.isTradestationEngine(var1.getEngine())) {
                        var3 = false;
                     }

                     this.closedThisDay = this.closeAllPositions(var1, var3, var4, var2);
                  }
               } else if (this.dailyEODExitTime >= this.dailySignalTimeRangeFrom
                  && this.SignalTimeRangeFrom < this.SignalTimeRangeTo
                  && var4 >= this.dailyEODExitTime) {
                  if (Engines.isTradestationEngine(var1.getEngine())) {
                     var3 = false;
                  }

                  this.closedThisDay = this.closeAllPositions(var1, var3, var4, var2);
               }
            }
         }

         this.previousTickTime = var4;
      }
   }

   private void initTimesForCurrentDay(long var1, StrategyBase var3) {
      this.EODTime = SQTime.correctDayEndMT(var1);
      if (this.EODExitTime == 0) {
         this.dailyEODExitTime = this.EODTime;
      } else if (this.LimitTimeRange && this.SignalTimeRangeFrom >= this.SignalTimeRangeTo) {
         this.dailyEODExitTime = SQTime.setHHMM(this.dailySignalTimeRangeTo, this.EODExitTime);
         this.dailyEODExitTime = SQTime.setSecond(this.dailyEODExitTime, 0);
         this.dailyEODExitTime = SQTime.setMiliSeconds(this.dailyEODExitTime, 0);
      } else {
         this.dailyEODExitTime = SQTime.setHHMM(var1, this.EODExitTime);
         this.dailyEODExitTime = SQTime.setSecond(this.dailyEODExitTime, 0);
         this.dailyEODExitTime = SQTime.setMiliSeconds(this.dailyEODExitTime, 0);
      }

      this.closedThisDay = false;
   }

   private void initLimitTimeRangeForCurrentDay(long var1) {
      this.dailySignalTimeRangeFrom = SQTime.setHHMM(var1, this.SignalTimeRangeFrom);
      this.dailySignalTimeRangeFrom = SQTime.setSecond(this.dailySignalTimeRangeFrom, 0);
      this.dailySignalTimeRangeFrom = SQTime.setMiliSeconds(this.dailySignalTimeRangeFrom, 0);
      this.dailySignalTimeRangeTo = SQTime.setHHMM(var1, this.SignalTimeRangeTo);
      this.dailySignalTimeRangeTo = SQTime.setSecond(this.dailySignalTimeRangeTo, 0);
      this.dailySignalTimeRangeTo = SQTime.setMiliSeconds(this.dailySignalTimeRangeTo, 0);
      if (this.SignalTimeRangeFrom >= this.SignalTimeRangeTo) {
         this.dailySignalTimeRangeTo = SQTime.addDays(this.dailySignalTimeRangeTo, 1);
      }
   }

   private boolean closeAllPositions(StrategyBase var1, boolean var2, long var3, TickEvent var5) throws TradingException {
      long var6 = -1L;
      long var8 = -1L;
      boolean var10 = false;
      int var11 = var1.Trader.getOpenOrdersCount(false);

      for (int var12 = var11 - 1; var12 >= 0; var12--) {
         ILiveOrder var13 = var1.Trader.getOpenOrder(var12, false);
         if ((var2 || !var13.isPendingOrder()) && var13.getStrategyName().equals(var1.getStrategyName())) {
            if (var6 == -1L) {
               var6 = SQTime.correctDayStart(var3);
               var8 = SQTime.correctDayEndMT(var3);
            }

            if (Engines.isTradestationEngine(var1.getEngine())) {
               if (var8 == this.EODTime) {
                  if (var5.isBarClose()) {
                     if (var13.getOpenTime() == this.previousTickTime && SQTime.getHHMM(var13.getOpenTime()) != this.EODExitTime) {
                     }

                     var13.Close((byte)13);
                     var10 = true;
                  }
               } else if (var13.getOpenTime() < var6) {
                  var13.Close((byte)5);
                  var10 = true;
               }
            } else if (var8 == this.EODTime) {
               var13.Close((byte)13);
               var10 = true;
            } else if (var13.getOpenTime() < var6) {
               var13.Close((byte)5);
               var10 = true;
            }
         }
      }

      return var10;
   }

   public TradingOption getClone() {
      ExitAtEndOfDay var1 = new ExitAtEndOfDay();
      var1.ExitAtEndOfDay = this.ExitAtEndOfDay;
      var1.EODExitTime = this.EODExitTime;
      return var1;
   }
}
