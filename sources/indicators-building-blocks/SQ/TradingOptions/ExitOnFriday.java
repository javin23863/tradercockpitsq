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

public class ExitOnFriday extends TradingOption {
   @Parameter(name = "Exit On Friday", defaultValue = "false", category = "Trading options")
   @Help("Close all positions at end of the week (Friday)?")
   @ForEngine("*,-SP,-SA")
   public boolean ExitOnFriday;
   @Parameter(name = "Friday Exit Time", defaultValue = "2300", category = "Trading options")
   @Help("Time on Friday when to close all positions.\nTime is in timezone of the data.")
   @Editor(type = 110)
   @Activator(param = "ExitOnFriday")
   @ForEngine("*,-SP,-SA")
   public int FridayExitTime;
   private long thisFridayExitTime = -1L;
   private long thisSundayBeginTime = -1L;
   private boolean closedThisWeek = false;
   private long EOFDayTime;
   private long previousTickTime = -1L;

   public boolean isUsedInTrading() {
      return this.ExitOnFriday;
   }

   public boolean OnBarUpdate(StrategyBase var1) throws TradingException {
      if (!this.ExitOnFriday) {
         return true;
      }

      long var2 = var1.MarketData.TimeCurrent();
      if (this.thisFridayExitTime == -1L) {
         this.initFridayExitTime(var2, false, var1);
      }

      if (var2 < this.thisFridayExitTime) {
         return true;
      }

      if (var2 < this.thisSundayBeginTime) {
         return false;
      }

      this.initFridayExitTime(var2, true, var1);
      return true;
   }

   public void OnTick(StrategyBase var1, TickEvent var2, boolean var3) throws Exception {
      if (this.ExitOnFriday) {
         long var4 = var2.getTime();
         if (!this.closedThisWeek && this.thisFridayExitTime != -1L && var4 >= this.thisFridayExitTime && var4 < this.thisSundayBeginTime) {
            if (Engines.isTradestationEngine(var1.getEngine())) {
               var3 = false;
            }

            this.closedThisWeek = this.closeAllPositions(var1, var3, var4, var2);
         }

         this.previousTickTime = var4;
      }
   }

   private void initFridayExitTime(long var1, boolean var3, StrategyBase var4) {
      if (var3) {
         int var5 = SQTime.getDayOfWeek(var1);
         this.thisFridayExitTime = SQTime.addDays(var1, var5 == 7 ? 1 : 0);
      } else {
         this.thisFridayExitTime = var1;
      }

      this.thisFridayExitTime = SQTime.setDayOfWeek(this.thisFridayExitTime, this.FridayExitTime == 0 ? 6 : 5);
      this.thisFridayExitTime = SQTime.setHHMM(this.thisFridayExitTime, this.FridayExitTime);
      this.thisFridayExitTime = SQTime.setSecond(this.thisFridayExitTime, 0);
      this.thisFridayExitTime = SQTime.setMiliSeconds(this.thisFridayExitTime, 0);
      this.EOFDayTime = SQTime.correctDayEndMT(this.thisFridayExitTime);
      this.thisSundayBeginTime = SQTime.setDayOfWeek(this.thisFridayExitTime, 7);
      this.thisSundayBeginTime = SQTime.setHHMM(this.thisSundayBeginTime, 0);
      this.thisSundayBeginTime = SQTime.setSecond(this.thisSundayBeginTime, 0);
      this.thisSundayBeginTime = SQTime.setMiliSeconds(this.thisSundayBeginTime, 0);
      this.closedThisWeek = false;
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
               int var14 = SQTime.getDayOfWeek(this.previousTickTime);
               if (var8 == this.EOFDayTime) {
                  if (var5.isBarClose()) {
                     if (var13.getOpenTime() == this.previousTickTime && SQTime.getHHMM(var13.getOpenTime()) == this.FridayExitTime) {
                     }

                     var13.Close((byte)16);
                     var10 = true;
                  }
               } else if (var13.getOpenTime() < var6 && var14 == 5) {
                  if (this.previousTickTime == var3) {
                     if (var3 >= var8) {
                        var13.Close((byte)14);
                        var10 = true;
                     }
                  } else {
                     var13.Close((byte)14);
                     var10 = true;
                  }
               }
            } else if (var8 == this.EOFDayTime) {
               var13.Close((byte)16);
               var10 = true;
            } else if (var13.getOpenTime() < var6) {
               var13.Close((byte)14);
               var10 = true;
            }
         }
      }

      return var10;
   }

   public TradingOption getClone() {
      ExitOnFriday var1 = new ExitOnFriday();
      var1.ExitOnFriday = this.ExitOnFriday;
      var1.FridayExitTime = this.FridayExitTime;
      return var1;
   }
}
