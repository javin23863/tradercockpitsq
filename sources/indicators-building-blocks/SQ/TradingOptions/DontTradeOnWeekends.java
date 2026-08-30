package SQ.TradingOptions;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Activator;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class DontTradeOnWeekends extends TradingOption {
   @Parameter(name = "Don't trade on weekends", defaultValue = "false", category = "Trading options")
   @Help("Strategy won't perform any operations during weekends")
   @ForEngine("*,-SP,-SA")
   public boolean DontTradeOnWeekends;
   @Parameter(name = "Friday Close Time", defaultValue = "2300", category = "Trading options")
   @Help("From this time on the strategy won't trade.\nTime is in timezone of the data.")
   @Editor(type = 110)
   @Activator(param = "DontTradeOnWeekends")
   @ForEngine("*,-SP,-SA")
   public int FridayCloseTime;
   @Parameter(name = "Sunday Open Time", defaultValue = "2300", category = "Trading options")
   @Help("Strategy reenables trading at this time")
   @Editor(type = 110)
   @Activator(param = "DontTradeOnWeekends")
   @ForEngine("*,-SP,-SA")
   public int SundayOpenTime;
   private long thisFridayExitTime = -1L;
   private long thisSundayBeginTime = -1L;

   public boolean isUsedInTrading() {
      return this.DontTradeOnWeekends;
   }

   public boolean OnBarUpdate(StrategyBase var1) throws TradingException {
      if (!this.DontTradeOnWeekends) {
         return true;
      }

      long var2 = var1.MarketData.TimeCurrent();
      if (this.thisFridayExitTime == -1L) {
         this.initTimes(var2, false, var1);
      }

      if (var2 < this.thisFridayExitTime) {
         return true;
      }

      if (var2 < this.thisSundayBeginTime) {
         return false;
      }

      this.initTimes(var2, true, var1);
      return true;
   }

   public void OnTick(StrategyBase var1, TickEvent var2, boolean var3) throws Exception {
   }

   private void initTimes(long var1, boolean var3, StrategyBase var4) {
      if (var3) {
         int var5 = SQTime.getDayOfWeek(var1);
         this.thisFridayExitTime = SQTime.addDays(var1, var5 == 7 ? 1 : 0);
         this.thisSundayBeginTime = this.thisFridayExitTime;
      } else {
         this.thisFridayExitTime = var1;
         this.thisSundayBeginTime = var1;
      }

      this.thisFridayExitTime = SQTime.setDayOfWeek(this.thisFridayExitTime, this.FridayCloseTime == 0 ? 6 : 5);
      this.thisFridayExitTime = SQTime.setHHMM(this.thisFridayExitTime, this.FridayCloseTime);
      this.thisFridayExitTime = SQTime.setSecond(this.thisFridayExitTime, 0);
      this.thisFridayExitTime = SQTime.setMiliSeconds(this.thisFridayExitTime, 0);
      this.thisSundayBeginTime = SQTime.setDayOfWeek(this.thisSundayBeginTime, 7);
      this.thisSundayBeginTime = SQTime.setHHMM(this.thisSundayBeginTime, this.SundayOpenTime);
      this.thisSundayBeginTime = SQTime.setSecond(this.thisSundayBeginTime, 0);
      this.thisSundayBeginTime = SQTime.setMiliSeconds(this.thisSundayBeginTime, 0);
   }

   public TradingOption getClone() {
      DontTradeOnWeekends var1 = new DontTradeOnWeekends();
      var1.DontTradeOnWeekends = this.DontTradeOnWeekends;
      var1.FridayCloseTime = this.FridayCloseTime;
      var1.SundayOpenTime = this.SundayOpenTime;
      return var1;
   }
}
